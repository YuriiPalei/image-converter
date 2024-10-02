package palei.yurii.imageconverter

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import javax.swing.JOptionPane

class ConvertToWebPAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        // Получение выбранного файла
        val file: VirtualFile? = e.getData(CommonDataKeys.VIRTUAL_FILE)
        if (file != null && !file.isDirectory) {
            val inputFile = File(file.path)
            val outputPath = "${inputFile.parent}/${getFileNameWithoutExtension(inputFile)}.webp"
            val outputFile = File(outputPath)

            try {
                WebPConverter.convertToWebP(inputFile, outputFile)
                Messages.showInfoMessage(
                    "Изображение успешно конвертировано в ${outputFile.path}",
                    "Конвертация завершена"
                )
            } catch (ex: Exception) {
                Messages.showErrorDialog(
                    "Ошибка при конвертации: ${ex.message}",
                    "Ошибка"
                )
            }
        } else {
            Messages.showErrorDialog(
                "Пожалуйста, выберите файл изображения.",
                "Файл не выбран"
            )
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
}