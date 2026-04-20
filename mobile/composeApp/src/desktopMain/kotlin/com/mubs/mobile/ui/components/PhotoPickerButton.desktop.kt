package com.mubs.mobile.ui.components

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun PhotoPickerButton(
    onPhotoPicked: (fileName: String, bytes: ByteArray) -> Unit
) {
    Button(onClick = {
        val chooser = JFileChooser().apply {
            fileFilter = FileNameExtensionFilter("Images", "jpg", "jpeg", "png")
        }
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            val file: File = chooser.selectedFile
            onPhotoPicked(file.name, file.readBytes())
        }
    }) {
        Text("上传照片")
    }
}
