package com.mubs.mobile.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun PhotoPickerButton(
    onPhotoPicked: (fileName: String, bytes: ByteArray) -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bytes = context.contentResolver.openInputStream(it)?.readBytes()
            if (bytes != null) {
                val name = "photo_${System.currentTimeMillis()}.jpg"
                onPhotoPicked(name, bytes)
            }
        }
    }

    Button(onClick = { launcher.launch("image/*") }) {
        Text("上传照片")
    }
}
