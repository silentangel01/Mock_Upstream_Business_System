package com.mubs.mobile.ui.components

import androidx.compose.runtime.Composable

@Composable
expect fun PhotoPickerButton(
    onPhotoPicked: (fileName: String, bytes: ByteArray) -> Unit
)
