package palei.yurii.imageconverter.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import palei.yurii.imageconverter.config.ConversionConfig;
import palei.yurii.imageconverter.service.ConversionService;
import palei.yurii.imageconverter.service.ConversionServiceImpl;
import palei.yurii.imageconverter.ui.ConfirmationDialog;

public class ConvertToWebPAction extends AnAction {
  private final ConversionService conversionService;

  public ConvertToWebPAction() {
    this.conversionService = new ConversionServiceImpl();
  }

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

        int maxFiles = ConversionConfig.getMaxFiles();
        if (selectedFiles.size() > maxFiles) {
          Messages.showErrorDialog(
              String.format(
                  "You have selected %d files. The maximum number of files allowed is %d.",
                  selectedFiles.size(), maxFiles),
              "File Limit Exceeded");
          return;
        }

        new Task.Backgroundable(project, "Converting to WebP", true) {
          @Override
          public void run(@NotNull ProgressIndicator indicator) {
            List<File> ioFiles =
                selectedFiles.stream().map(file -> new File(file.getPath())).toList();

            var results = conversionService.convertFiles(ioFiles, "webp");
            var processedFiles = new AtomicInteger(0);

            for (var result : results) {
              if (result.isSuccess()) {
                var localFileSystem = LocalFileSystem.getInstance();
                var virtualOutputFile =
                    localFileSystem.refreshAndFindFileByIoFile(result.getOutputFile());
                if (virtualOutputFile != null) {
                  virtualOutputFile.refresh(false, false);
                }
              }

              synchronized (indicator) {
                int currentProcessed = processedFiles.incrementAndGet();
                indicator.setFraction((double) currentProcessed / results.size());
                indicator.setText(
                    String.format("Processed files: %d of %d", currentProcessed, results.size()));
              }
            }

            var projectBaseDir = project != null ? project.getBaseDir() : null;
            if (projectBaseDir != null) {
              projectBaseDir.refresh(false, true);
            }
          }

          @Override
          public void onSuccess() {
            Messages.showInfoMessage(
                "All files have been successfully converted.", "Conversion Completed");
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
}
