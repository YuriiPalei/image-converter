/*
 * Copyright 2024 Yurii Palei
 *
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package palei.yurii.imageconverter

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.text.DecimalFormat
import java.util.concurrent.Executors
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.ImageWriter
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.SwingUtilities
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

class ConfirmationDialog(
    project: Project?,
    private val files: List<VirtualFile>,
    private val maxFiles: Int
) : DialogWrapper(project) {
    private val fileDataList = mutableListOf<FileData>()
    private val tableModel = FileTableModel()
    private val decimalFormat = DecimalFormat("#.##")
    private lateinit var selectAllCheckBox: JBCheckBox
    private var isUpdatingSelectAllCheckBox = false

    init {
        init()
        title = "Confirm Conversion"

        // Prepare data for the table
        files.forEach { file ->
            val fileSize = file.length
            val fileData = FileData(
                file = file,
                selected = true,
                fileSize = fileSize,
                estimatedSize = null,
                reductionPercentage = null,
                status = "Estimating..."
            )
            fileDataList.add(fileData)
        }

        estimateSizesInBackground()
        updateOkButton();
    }

    override fun createCenterPanel(): JComponent? {
        val panel = JPanel(BorderLayout())

        selectAllCheckBox = JBCheckBox("Select All", true)
        selectAllCheckBox.addChangeListener {
            if (!isUpdatingSelectAllCheckBox) {
                val selected = selectAllCheckBox.isSelected
                fileDataList.forEachIndexed { index, fileData ->
                    fileData.selected = selected
                    tableModel.fireTableCellUpdated(index, 0)
                }
                updateOkButton()
            }
        }
        panel.add(selectAllCheckBox, BorderLayout.NORTH)

        val table = JTable(tableModel)
        table.preferredScrollableViewportSize = Dimension(600, 400)
        table.fillsViewportHeight = true

        val centerRenderer = DefaultTableCellRenderer()
        centerRenderer.horizontalAlignment = javax.swing.JLabel.CENTER
        table.columnModel.getColumn(2).cellRenderer = centerRenderer
        table.columnModel.getColumn(3).cellRenderer = centerRenderer
        table.columnModel.getColumn(4).cellRenderer = centerRenderer

        table.columnModel.getColumn(0).preferredWidth = 50  // Select
        table.columnModel.getColumn(1).preferredWidth = 250 // File Name
        table.columnModel.getColumn(2).preferredWidth = 100 // Current Size
        table.columnModel.getColumn(3).preferredWidth = 100 // Estimated Size
        table.columnModel.getColumn(4).preferredWidth = 100 // Reduction

        val scrollPane = JBScrollPane(table)
        panel.add(scrollPane, BorderLayout.CENTER)

        return panel
    }

    fun getSelectedFiles(): List<VirtualFile> {
        return fileDataList.filter { it.selected }.map { it.file }
    }

    private fun updateOkButton() {
        val selectedCount = fileDataList.count { it.selected }
        isOKActionEnabled = selectedCount in 1..maxFiles
        if (selectedCount > maxFiles) {
            setErrorText("You can select up to $maxFiles files.")
        } else {
            setErrorText(null)
        }
    }

    private fun estimateSizesInBackground() {
        val executor = Executors.newFixedThreadPool(5)
        fileDataList.forEachIndexed { index, fileData ->
            executor.submit {
                val file = fileData.file
                if (isSupportedImage(file.extension?.lowercase())) {
                    val estimatedSize = estimateWebPSize(file)
                    fileData.estimatedSize = estimatedSize
                    if (estimatedSize != null) {
                        fileData.reductionPercentage =
                            ((fileData.fileSize - estimatedSize) / fileData.fileSize.toDouble()) * 100
                        fileData.status = "Estimated"
                    } else {
                        fileData.status = "Error"
                    }
                } else {
                    fileData.status = "Unsupported"
                }
                SwingUtilities.invokeLater {
                    tableModel.fireTableRowsUpdated(index, index)
                }
            }
        }
        executor.shutdown()
    }

    private fun formatSize(sizeInBytes: Long?): String {
        if (sizeInBytes == null) return "N/A"
        val kb = sizeInBytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1 -> "${decimalFormat.format(mb)} MB"
            kb >= 1 -> "${decimalFormat.format(kb)} KB"
            else -> "$sizeInBytes B"
        }
    }

    private fun formatPercentage(value: Double?): String {
        if (value == null) return "N/A"
        return "${decimalFormat.format(value)}%"
    }

    private fun isSupportedImage(extension: String?): Boolean {
        val supportedExtensions = setOf("jpg", "jpeg", "png")
        return extension != null && extension in supportedExtensions
    }

    private fun estimateWebPSize(file: VirtualFile): Long? {
        return try {
            val inputStream = file.inputStream
            val bufferedImage: BufferedImage = ImageIO.read(inputStream)
            inputStream.close()

            val byteArrayOutputStream = ByteArrayOutputStream()
            val writers = ImageIO.getImageWritersByFormatName("webp")
            if (!writers.hasNext()) {
                return null
            }
            val writer: ImageWriter = writers.next()
            val writeParam: ImageWriteParam = writer.defaultWriteParam

            val ios = ImageIO.createImageOutputStream(byteArrayOutputStream)
            writer.output = ios
            writer.write(null, IIOImage(bufferedImage, null, null), writeParam)
            ios.close()
            writer.dispose()

            byteArrayOutputStream.size().toLong()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    inner class FileTableModel : AbstractTableModel() {
        private val columnNames = arrayOf("Select", "File Name", "Current Size", "Estimated Size", "Reduction")

        override fun getRowCount(): Int = fileDataList.size

        override fun getColumnCount(): Int = columnNames.size

        override fun getColumnName(column: Int): String = columnNames[column]

        override fun getColumnClass(columnIndex: Int): Class<*> {
            return when (columnIndex) {
                0 -> java.lang.Boolean::class.java
                else -> String::class.java
            }
        }

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
            return columnIndex == 0
        }

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
            val fileData = fileDataList[rowIndex]
            return when (columnIndex) {
                0 -> fileData.selected
                1 -> fileData.file.name
                2 -> formatSize(fileData.fileSize)
                3 -> when (fileData.status) {
                    "Estimating..." -> "Estimating..."
                    "Error" -> "Error"
                    else -> formatSize(fileData.estimatedSize)
                }

                4 -> when (fileData.status) {
                    "Estimating..." -> "Estimating..."
                    "Error" -> "Error"
                    else -> formatPercentage(fileData.reductionPercentage)
                }

                else -> null
            }
        }

        override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
            if (columnIndex == 0) {
                fileDataList[rowIndex].selected = aValue as Boolean
                fireTableCellUpdated(rowIndex, columnIndex)
                updateSelectAllCheckBox()
                updateOkButton()
            }
        }

        private fun updateSelectAllCheckBox() {
            val allSelected = fileDataList.all { it.selected }
            val noneSelected = fileDataList.none { it.selected }

            isUpdatingSelectAllCheckBox = true
            selectAllCheckBox.isSelected = allSelected
            isUpdatingSelectAllCheckBox = false
        }
    }

    data class FileData(
        val file: VirtualFile,
        var selected: Boolean,
        val fileSize: Long,
        var estimatedSize: Long?,
        var reductionPercentage: Double?,
        var status: String
    )
}