package palei.yurii.imageconverter.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBScrollPane;
import palei.yurii.imageconverter.config.ConversionConfig;
import palei.yurii.imageconverter.model.ConversionStrategy;
import palei.yurii.imageconverter.model.FileData;
import palei.yurii.imageconverter.service.ConversionService;
import palei.yurii.imageconverter.service.ConversionServiceImpl;
import java.awt.*;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConfirmationDialog extends DialogWrapper {
  private final List<FileData> fileDataList = new ArrayList<>();
  private final FileTableModel tableModel = new FileTableModel();
  private final DecimalFormat decimalFormat = new DecimalFormat("#.##");
  private final ConversionService conversionService;
  private JBCheckBox selectAllCheckBox;
  private boolean isUpdatingSelectAllCheckBox = false;

  public ConfirmationDialog(@Nullable Project project, @NotNull List<VirtualFile> files) {
    super(project);
    this.conversionService = new ConversionServiceImpl();
    init();
    setTitle("Confirm Conversion");

    // Prepare data for the table
    for (var file : files) {
      long fileSize = file.getLength();
      var fileData = new FileData(file, true, fileSize);
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
    int maxFiles = ConversionConfig.getMaxFiles();
    setOKActionEnabled(selectedCount > 0 && selectedCount <= maxFiles);
    if (selectedCount > maxFiles) {
      setErrorText("You can select up to " + maxFiles + " files.");
    } else {
      setErrorText(null);
    }
  }

  private void estimateSizesInBackground() {
    List<File> ioFiles = fileDataList.stream().map(FileData::getIoFile).toList();
    var results = conversionService.convertFiles(ioFiles, "webp", ConversionStrategy.SIMULATION);

    if (results.isEmpty()) {
      System.out.println("No results returned from conversion service.");
      return;
    }

    for (int i = 0; i < fileDataList.size(); i++) {
      final int index = i;
      var fileData = fileDataList.get(i);
      var file = fileData.getFile();
      var extension = fileData.getExtension();

      if (conversionService.isFormatSupported(extension)) {
        try {
          System.out.println("Processing result for file: " + file.getName());
          if (i < results.size()) {
            var result = results.get(i);
            System.out.println("Result success: " + result.isSuccess());
            System.out.println("Result converted size: " + result.getConvertedSize());
            System.out.println("Result reduction: " + result.getReductionPercentage());

            if (result.isSuccess()) {
              SwingUtilities.invokeLater(
                  () -> {
                    fileData.setEstimatedSize(result.getConvertedSize());
                    fileData.setReductionPercentage(result.getReductionPercentage());
                    fileData.setStatus("Estimated");
                    System.out.println("Setting status to Estimated");
                    tableModel.fireTableRowsUpdated(index, index);
                  });
            } else {
              SwingUtilities.invokeLater(
                  () -> {
                    fileData.setStatus("Error: " + result.getErrorMessage());
                    System.out.println("Setting error status: " + result.getErrorMessage());
                    tableModel.fireTableRowsUpdated(index, index);
                  });
            }
          } else {
            SwingUtilities.invokeLater(
                () -> {
                  fileData.setStatus("Error: No result available");
                  System.out.println("Setting error status: No result available");
                  tableModel.fireTableRowsUpdated(index, index);
                });
          }
        } catch (Exception e) {
          System.out.println("Exception during processing: " + e.getMessage());
          e.printStackTrace();
          SwingUtilities.invokeLater(
              () -> {
                fileData.setStatus("Error: " + e.getMessage());
                tableModel.fireTableRowsUpdated(index, index);
              });
        }
      } else {
        System.out.println("Format not supported: " + extension);
        SwingUtilities.invokeLater(
            () -> {
              fileData.setStatus("Unsupported format");
              tableModel.fireTableRowsUpdated(index, index);
            });
      }
    }
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
        case 3 -> {
          if ("Estimating...".equals(fileData.getStatus())) {
            yield "Estimating...";
          } else if (fileData.getStatus().startsWith("Error")) {
            yield fileData.getStatus();
          } else {
            yield formatSize(fileData.getEstimatedSize());
          }
        }
        case 4 -> {
          if ("Estimating...".equals(fileData.getStatus())) {
            yield "Estimating...";
          } else if (fileData.getStatus().startsWith("Error")) {
            yield fileData.getStatus();
          } else {
            yield formatPercentage(fileData.getReductionPercentage());
          }
        }
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
}
