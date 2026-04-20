package com.mubs.mobile.ui.components

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
actual fun PhotoPickerButton(
    onPhotoPicked: (fileName: String, bytes: ByteArray) -> Unit
) {
    // iOS photo picker - placeholder, requires UIKit integration
    Button(onClick = {
        // TODO: Integrate PHPickerViewController via UIKit interop
    }) {
        Text("上传照片")
    }
}
