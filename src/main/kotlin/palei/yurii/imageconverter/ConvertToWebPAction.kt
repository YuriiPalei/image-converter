package palei.yurii.imageconverter

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ConvertToWebPAction : AnAction() {
    private val MAX_FILES = 5

    override fun actionPerformed(e: AnActionEvent) {
        val allFiles: Array<VirtualFile>? = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        if (allFiles != null && allFiles.isNotEmpty()) {
            val project = e.project

            // Show confirmation dialog
            val dialog = ConfirmationDialog(project, allFiles.toList(), MAX_FILES)
            if (dialog.showAndGet()) {
                // User clicked "Confirm"
                val selectedFiles = dialog.getSelectedFiles()
                if (selectedFiles.isEmpty()) {
                    Messages.showInfoMessage("No files selected for conversion.", "Information")
                    return
                }

                if (selectedFiles.size > MAX_FILES) {
                    Messages.showErrorDialog(
                        "You have selected ${selectedFiles.size} files. The maximum number of files allowed is $MAX_FILES.",
                        "File Limit Exceeded"
                    )
                    return
                }

                object : Task.Backgroundable(project, "Converting to WebP", true) {
                    // Thread-safe list for failed files
                    private val failedFiles = CopyOnWriteArrayList<String>()

                    override fun run(indicator: ProgressIndicator) {
                        val executor = Executors.newFixedThreadPool(MAX_FILES)
                        val totalFiles = selectedFiles.size
                        var processedFiles = 0

                        for (file in selectedFiles) {
                            if (!file.isDirectory) {
                                val inputFile = File(file.path)
                                val extension = file.extension?.lowercase()
                                if (isSupportedImage(extension)) {
                                    val outputPath = "${inputFile.parent}/${getFileNameWithoutExtension(inputFile)}.webp"
                                    val outputFile = File(outputPath)

                                    executor.submit {
                                        try {
                                            WebPConverter.convertToWebP(inputFile, outputFile)
                                        } catch (ex: Exception) {
                                            ex.printStackTrace()
                                            // Add failed file to the list
                                            failedFiles.add("${inputFile.name}: Conversion error (${ex.message})")
                                        } finally {
                                            synchronized(indicator) {
                                                processedFiles++
                                                indicator.fraction = processedFiles.toDouble() / totalFiles
                                                indicator.text = "Processed files: $processedFiles of $totalFiles"
                                            }
                                        }
                                    }
                                } else {
                                    // File has unsupported format
                                    failedFiles.add("${inputFile.name}: Unsupported format")
                                    synchronized(indicator) {
                                        processedFiles++
                                        indicator.fraction = processedFiles.toDouble() / totalFiles
                                        indicator.text = "Processed files: $processedFiles of $totalFiles"
                                    }
                                }
                            } else {
                                // Skip directories or handle recursively if needed
                                synchronized(indicator) {
                                    processedFiles++
                                    indicator.fraction = processedFiles.toDouble() / totalFiles
                                    indicator.text = "Processed files: $processedFiles of $totalFiles"
                                }
                            }
                        }
                        executor.shutdown()
                        try {
                            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
                        } catch (ex: InterruptedException) {
                            Thread.currentThread().interrupt()
                            logger.warn("Waiting for thread pool termination was interrupted", ex)
                        }
                    }

                    override fun onSuccess() {
                        // Show message to the user
                        if (failedFiles.isNotEmpty()) {
                            val messageBuilder = StringBuilder("Conversion completed with errors.\n\nIssues occurred with the following files:\n")
                            failedFiles.forEach { fileInfo ->
                                messageBuilder.append("- $fileInfo\n")
                            }
                            Messages.showWarningDialog(messageBuilder.toString(), "Conversion Completed")
                        } else {
                            Messages.showInfoMessage("All files have been successfully converted.", "Conversion Completed")
                        }
                    }

                    override fun onThrowable(error: Throwable) {
                        Messages.showErrorDialog("Error during conversion: ${error.message}", "Error")
                    }
                }.queue()
            } else {
                // User clicked "Cancel"
                // Do nothing
            }
        } else {
            Messages.showErrorDialog("Please select one or more image files.", "No Files Selected")
        }
    }

    private fun getFileNameWithoutExtension(file: File): String {
        val name = file.name
        val pos = name.lastIndexOf(".")
        return if (pos > 0) {
            name.substring(0, pos)
        } else {
            name
        }
    }

    private fun isSupportedImage(extension: String?): Boolean {
        val supportedExtensions = setOf("jpg", "jpeg", "png")
        return extension != null && extension in supportedExtensions
    }

    companion object {
        // Initialize logger
        private val logger = com.intellij.openapi.diagnostic.Logger.getInstance(ConvertToWebPAction::class.java)
    }
}