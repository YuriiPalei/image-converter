package palei.yurii.imageconverter

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel

class ConfirmationDialog(
    project: Project?,
    private val files: List<VirtualFile>
) : DialogWrapper(project) {

    private val checkBoxMap = mutableMapOf<VirtualFile, JBCheckBox>()
    private lateinit var selectAllCheckBox: JBCheckBox

    init {
        init()
        title = "Confirm Conversion"
    }

    override fun createCenterPanel(): JComponent? {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        selectAllCheckBox = JBCheckBox("Select All", true)
        selectAllCheckBox.addActionListener {
            val selected = selectAllCheckBox.isSelected
            checkBoxMap.values.forEach { it.isSelected = selected }
        }
        panel.add(selectAllCheckBox)

        files.forEach { file ->
            val checkBox = JBCheckBox(file.name, true)
            checkBoxMap[file] = checkBox
            panel.add(checkBox)
        }

        val scrollPane = JBScrollPane(panel)
        scrollPane.preferredSize = java.awt.Dimension(400, 300)
        return scrollPane
    }

    fun getSelectedFiles(): List<VirtualFile> {
        return checkBoxMap.filter { it.value.isSelected }.map { it.key }
    }
}