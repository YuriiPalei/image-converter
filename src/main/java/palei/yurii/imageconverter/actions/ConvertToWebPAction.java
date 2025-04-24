package palei.yurii.imageconverter.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import palei.yurii.imageconverter.core.WebPConverter;
import palei.yurii.imageconverter.ui.ConfirmationDialog;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ConvertToWebPAction extends AnAction {
  private static final Logger LOG = Logger.getInstance(ConvertToWebPAction.class);
  private static final int MAX_FILES = 5;

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
    var presentation = e.getPresentation();

    if (files == null || files.length == 0) {
      presentation.setEnabledAndVisible(false);
    } else {
      boolean hasDirectory = false;
      for (VirtualFile file : files) {
        if (file.isDirectory()) {
          hasDirectory = true;
          break;
        }
      }
      presentation.setEnabledAndVisible(!hasDirectory);
    }
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    VirtualFile[] allFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
    if (allFiles != null && allFiles.length > 0) {
      var project = e.getProject();

      var dialog = new ConfirmationDialog(project, List.of(allFiles));
      if (dialog.showAndGet()) {
        var selectedFiles = dialog.getSelectedFiles();
        if (selectedFiles.isEmpty()) {
          Messages.showInfoMessage("No files selected for conversion.", "Information");
          return;
        }

        if (selectedFiles.size() > MAX_FILES) {
          Messages.showErrorDialog(
              String.format(
                  "You have selected %d files. The maximum number of files allowed is %d.",
                  selectedFiles.size(), MAX_FILES),
              "File Limit Exceeded");
          return;
        }

        new Task.Backgroundable(project, "Converting to WebP", true) {
          private final CopyOnWriteArrayList<String> failedFiles = new CopyOnWriteArrayList<>();

          @Override
          public void run(@NotNull ProgressIndicator indicator) {
            var executor = Executors.newFixedThreadPool(MAX_FILES);
            int totalFiles = selectedFiles.size();
            var processedFiles = new AtomicInteger(0);

            for (var file : selectedFiles) {
              if (!file.isDirectory()) {
                var inputFile = new File(file.getPath());
                var extension =
                    file.getExtension() != null ? file.getExtension().toLowerCase() : null;
                if (isSupportedImage(extension)) {
                  var outputPath =
                      String.format(
                          "%s/%s.webp",
                          inputFile.getParent(), getFileNameWithoutExtension(inputFile));
                  var outputFile = new File(outputPath);

                  executor.submit(
                      () -> {
                        try {
                          WebPConverter.convertToWebP(inputFile, outputFile);

                          var localFileSystem = LocalFileSystem.getInstance();
                          var virtualOutputFile =
                              localFileSystem.refreshAndFindFileByIoFile(outputFile);
                          if (virtualOutputFile != null) {
                            virtualOutputFile.refresh(false, false);
                          }
                        } catch (Exception ex) {
                          ex.printStackTrace();
                          failedFiles.add(
                              String.format(
                                  "%s: Conversion error (%s)",
                                  inputFile.getName(), ex.getMessage()));
                        } finally {
                          synchronized (indicator) {
                            int currentProcessed = processedFiles.incrementAndGet();
                            indicator.setFraction((double) currentProcessed / totalFiles);
                            indicator.setText(
                                String.format(
                                    "Processed files: %d of %d", currentProcessed, totalFiles));
                          }
                        }
                      });
                } else {
                  failedFiles.add(String.format("%s: Unsupported format", inputFile.getName()));
                  synchronized (indicator) {
                    int currentProcessed = processedFiles.incrementAndGet();
                    indicator.setFraction((double) currentProcessed / totalFiles);
                    indicator.setText(
                        String.format("Processed files: %d of %d", currentProcessed, totalFiles));
                  }
                }
              } else {
                synchronized (indicator) {
                  int currentProcessed = processedFiles.incrementAndGet();
                  indicator.setFraction((double) currentProcessed / totalFiles);
                  indicator.setText(
                      String.format("Processed files: %d of %d", currentProcessed, totalFiles));
                }
              }
            }
            executor.shutdown();
            try {
              executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException ex) {
              Thread.currentThread().interrupt();
              LOG.warn("Waiting for thread pool termination was interrupted", ex);
            }

            var projectBaseDir = project != null ? project.getBaseDir() : null;
            if (projectBaseDir != null) {
              projectBaseDir.refresh(false, true);
            }
          }

          @Override
          public void onSuccess() {
            if (!failedFiles.isEmpty()) {
              var messageBuilder =
                  new StringBuilder(
                      "Conversion completed with errors.\n\nIssues occurred with the following files:\n");
              failedFiles.forEach(
                  fileInfo -> messageBuilder.append("- ").append(fileInfo).append("\n"));
              Messages.showWarningDialog(messageBuilder.toString(), "Conversion Completed");
            } else {
              Messages.showInfoMessage(
                  "All files have been successfully converted.", "Conversion Completed");
            }
          }

          @Override
          public void onThrowable(@NotNull Throwable error) {
            Messages.showErrorDialog("Error during conversion: " + error.getMessage(), "Error");
          }
        }.queue();
      }
    } else {
      Messages.showErrorDialog("Please select one or more image files.", "No Files Selected");
    }
  }

  private String getFileNameWithoutExtension(File file) {
    String name = file.getName();
    int pos = name.lastIndexOf(".");
    return pos > 0 ? name.substring(0, pos) : name;
  }

  private boolean isSupportedImage(String extension) {
    var supportedExtensions = List.of("jpg", "jpeg", "png");
    return extension != null && supportedExtensions.contains(extension);
  }
}
