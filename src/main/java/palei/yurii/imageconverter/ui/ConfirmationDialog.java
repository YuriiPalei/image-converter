package palei.yurii.imageconverter.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBScrollPane;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConfirmationDialog extends DialogWrapper {
  private static final int MAX_FILES = 5;
  private final List<FileData> fileDataList = new ArrayList<>();
  private final FileTableModel tableModel = new FileTableModel();
  private final DecimalFormat decimalFormat = new DecimalFormat("#.##");
  private JBCheckBox selectAllCheckBox;
  private boolean isUpdatingSelectAllCheckBox = false;

  public ConfirmationDialog(@Nullable Project project, @NotNull List<VirtualFile> files) {
    super(project);
    init();
    setTitle("Confirm Conversion");

    // Prepare data for the table
    for (var file : files) {
      long fileSize = file.getLength();
      var fileData = new FileData(file, true, fileSize, null, null, "Estimating...");
      fileDataList.add(fileData);
    }

    estimateSizesInBackground();
    updateOkButton();
  }

  @Override
  protected @Nullable JComponent createCenterPanel() {
    var panel = new JPanel(new BorderLayout());

    selectAllCheckBox = new JBCheckBox("Select all", true);
    selectAllCheckBox.addChangeListener(
        e -> {
          if (!isUpdatingSelectAllCheckBox) {
            boolean selected = selectAllCheckBox.isSelected();
            for (int i = 0; i < fileDataList.size(); i++) {
              fileDataList.get(i).setSelected(selected);
              tableModel.fireTableCellUpdated(i, 0);
            }
            updateOkButton();
          }
        });
    panel.add(selectAllCheckBox, BorderLayout.NORTH);

    var table = new JBTable(tableModel);
    table.setPreferredScrollableViewportSize(new Dimension(600, 400));
    table.setFillsViewportHeight(true);

    var centerRenderer = new DefaultTableCellRenderer();
    centerRenderer.setHorizontalAlignment(JLabel.CENTER);
    table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
    table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
    table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);

    table.getColumnModel().getColumn(0).setPreferredWidth(50); // Select
    table.getColumnModel().getColumn(1).setPreferredWidth(250); // File Name
    table.getColumnModel().getColumn(2).setPreferredWidth(100); // Current Size
    table.getColumnModel().getColumn(3).setPreferredWidth(100); // Estimated Size
    table.getColumnModel().getColumn(4).setPreferredWidth(100); // Reduction

    var scrollPane = new JBScrollPane(table);
    panel.add(scrollPane, BorderLayout.CENTER);

    return panel;
  }

  public List<VirtualFile> getSelectedFiles() {
    return fileDataList.stream().filter(FileData::isSelected).map(FileData::getFile).toList();
  }

  private void updateOkButton() {
    long selectedCount = fileDataList.stream().filter(FileData::isSelected).count();
    setOKActionEnabled(selectedCount >= 1 && selectedCount <= MAX_FILES);
    if (selectedCount > MAX_FILES) {
      setErrorText("You can select up to " + MAX_FILES + " files.");
    } else {
      setErrorText(null);
    }
  }

  private void estimateSizesInBackground() {
    var executor = Executors.newFixedThreadPool(5);
    for (int i = 0; i < fileDataList.size(); i++) {
      final int index = i;
      var fileData = fileDataList.get(i);
      executor.submit(
          () -> {
            var file = fileData.getFile();
            if (isSupportedImage(
                file.getExtension() != null ? file.getExtension().toLowerCase() : null)) {
              var estimatedSize = estimateWebPSize(file);
              fileData.setEstimatedSize(estimatedSize);
              if (estimatedSize != null) {
                fileData.setReductionPercentage(
                    ((fileData.getFileSize() - estimatedSize) / (double) fileData.getFileSize())
                        * 100);
                fileData.setStatus("Estimated");
              } else {
                fileData.setStatus("Error");
              }
            } else {
              fileData.setStatus("Unsupported");
            }
            SwingUtilities.invokeLater(() -> tableModel.fireTableRowsUpdated(index, index));
          });
    }
    executor.shutdown();
  }

  private String formatSize(Long sizeInBytes) {
    if (sizeInBytes == null) return "N/A";
    double kb = sizeInBytes / 1024.0;
    double mb = kb / 1024.0;
    if (mb >= 1) {
      return decimalFormat.format(mb) + " MB";
    } else if (kb >= 1) {
      return decimalFormat.format(kb) + " KB";
    } else {
      return sizeInBytes + " B";
    }
  }

  private String formatPercentage(Double value) {
    if (value == null) return "N/A";
    return decimalFormat.format(value) + "%";
  }

  private boolean isSupportedImage(String extension) {
    var supportedExtensions = List.of("jpg", "jpeg", "png");
    return extension != null && supportedExtensions.contains(extension);
  }

  private Long estimateWebPSize(VirtualFile file) {
    try {
      var inputStream = file.getInputStream();
      var bufferedImage = ImageIO.read(inputStream);
      inputStream.close();

      var byteArrayOutputStream = new ByteArrayOutputStream();
      var writers = ImageIO.getImageWritersByFormatName("webp");
      if (!writers.hasNext()) {
        return null;
      }
      var writer = writers.next();
      var writeParam = writer.getDefaultWriteParam();

      var ios = ImageIO.createImageOutputStream(byteArrayOutputStream);
      writer.setOutput(ios);
      writer.write(null, new IIOImage(bufferedImage, null, null), writeParam);
      ios.close();
      writer.dispose();

      return (long) byteArrayOutputStream.size();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  private class FileTableModel extends AbstractTableModel {
    private final String[] columnNames = {
      "Select", "File Name", "Current Size", "Estimated Size", "Reduction"
    };

    @Override
    public int getRowCount() {
      return fileDataList.size();
    }

    @Override
    public int getColumnCount() {
      return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
      return columnNames[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      return columnIndex == 0 ? Boolean.class : String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return columnIndex == 0;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      var fileData = fileDataList.get(rowIndex);
      return switch (columnIndex) {
        case 0 -> fileData.isSelected();
        case 1 -> fileData.getFile().getName();
        case 2 -> formatSize(fileData.getFileSize());
        case 3 ->
            switch (fileData.getStatus()) {
              case "Estimating..." -> "Estimating...";
              case "Error" -> "Error";
              default -> formatSize(fileData.getEstimatedSize());
            };
        case 4 ->
            switch (fileData.getStatus()) {
              case "Estimating..." -> "Estimating...";
              case "Error" -> "Error";
              default -> formatPercentage(fileData.getReductionPercentage());
            };
        default -> null;
      };
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      if (columnIndex == 0) {
        fileDataList.get(rowIndex).setSelected((Boolean) aValue);
        fireTableCellUpdated(rowIndex, columnIndex);
        updateSelectAllCheckBox();
        updateOkButton();
      }
    }

    private void updateSelectAllCheckBox() {
      boolean allSelected = fileDataList.stream().allMatch(FileData::isSelected);

      isUpdatingSelectAllCheckBox = true;
      selectAllCheckBox.setSelected(allSelected);
      isUpdatingSelectAllCheckBox = false;
    }
  }

  private static class FileData {
    private final VirtualFile file;
    private boolean selected;
    private final long fileSize;
    private Long estimatedSize;
    private Double reductionPercentage;
    private String status;

    public FileData(
        VirtualFile file,
        boolean selected,
        long fileSize,
        Long estimatedSize,
        Double reductionPercentage,
        String status) {
      this.file = file;
      this.selected = selected;
      this.fileSize = fileSize;
      this.estimatedSize = estimatedSize;
      this.reductionPercentage = reductionPercentage;
      this.status = status;
    }

    public VirtualFile getFile() {
      return file;
    }

    public boolean isSelected() {
      return selected;
    }

    public void setSelected(boolean selected) {
      this.selected = selected;
    }

    public long getFileSize() {
      return fileSize;
    }

    public Long getEstimatedSize() {
      return estimatedSize;
    }

    public void setEstimatedSize(Long estimatedSize) {
      this.estimatedSize = estimatedSize;
    }

    public Double getReductionPercentage() {
      return reductionPercentage;
    }

    public void setReductionPercentage(Double reductionPercentage) {
      this.reductionPercentage = reductionPercentage;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }
  }
}
