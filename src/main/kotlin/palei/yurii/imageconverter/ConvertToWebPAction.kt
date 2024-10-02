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
    override fun actionPerformed(e: AnActionEvent) {
        val allFiles: Array<VirtualFile>? = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        if (allFiles != null && allFiles.isNotEmpty()) {
            val project = e.project

            // Открываем диалог подтверждения
            val dialog = ConfirmationDialog(project, allFiles.toList())
            if (dialog.showAndGet()) {
                // Пользователь нажал "Подтвердить"
                val selectedFiles = dialog.getSelectedFiles()
                if (selectedFiles.isEmpty()) {
                    Messages.showInfoMessage("Нет выбранных файлов для конвертации.", "Информация")
                    return
                }

                object : Task.Backgroundable(project, "Конвертация в WebP", true) {
                    // Потокобезопасный список для неудавшихся файлов
                    private val failedFiles = CopyOnWriteArrayList<String>()

                    override fun run(indicator: ProgressIndicator) {
                        val executor = Executors.newFixedThreadPool(5)
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
                                            // Добавляем имя неудавшегося файла в список
                                            failedFiles.add("${inputFile.name}: Ошибка при конвертации (${ex.message})")
                                        } finally {
                                            synchronized(indicator) {
                                                processedFiles++
                                                indicator.fraction = processedFiles.toDouble() / totalFiles
                                                indicator.text = "Обработано файлов: $processedFiles из $totalFiles"
                                            }
                                        }
                                    }
                                } else {
                                    // Файл имеет неподдерживаемый формат
                                    failedFiles.add("${inputFile.name}: Неподдерживаемый формат")
                                    synchronized(indicator) {
                                        processedFiles++
                                        indicator.fraction = processedFiles.toDouble() / totalFiles
                                        indicator.text = "Обработано файлов: $processedFiles из $totalFiles"
                                    }
                                }
                            } else {
                                // Пропускаем директории или обрабатываем рекурсивно, если необходимо
                                synchronized(indicator) {
                                    processedFiles++
                                    indicator.fraction = processedFiles.toDouble() / totalFiles
                                    indicator.text = "Обработано файлов: $processedFiles из $totalFiles"
                                }
                            }
                        }
                        executor.shutdown()
                        try {
                            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
                        } catch (ex: InterruptedException) {
                            Thread.currentThread().interrupt()
                            logger.warn("Ожидание завершения потоков было прервано", ex)
                        }
                    }

                    override fun onSuccess() {
                        // Формируем сообщение для пользователя
                        if (failedFiles.isNotEmpty()) {
                            val messageBuilder = StringBuilder("Конвертация завершена с ошибками.\n\nПроблемы возникли со следующими файлами:\n")
                            failedFiles.forEach { fileInfo ->
                                messageBuilder.append("- $fileInfo\n")
                            }
                            Messages.showWarningDialog(messageBuilder.toString(), "Конвертация завершена")
                        } else {
                            Messages.showInfoMessage("Все файлы успешно конвертированы.", "Конвертация завершена")
                        }
                    }

                    override fun onThrowable(error: Throwable) {
                        Messages.showErrorDialog("Ошибка при конвертации: ${error.message}", "Ошибка")
                    }
                }.queue()
            } else {
                // Пользователь нажал "Отмена"
                // Ничего не делаем
            }
        } else {
            Messages.showErrorDialog("Пожалуйста, выберите один или несколько файлов изображений.", "Файлы не выбраны")
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
        // Инициализируем логгер
        private val logger = com.intellij.openapi.diagnostic.Logger.getInstance(ConvertToWebPAction::class.java)
    }
}