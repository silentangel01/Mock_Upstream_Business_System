package com.mubs.mobile

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.mubs.mobile.di.AppModule
import com.mubs.mobile.domain.SessionManager

fun main() = application {
    AppModule.init(SessionManager())
    Window(
        onCloseRequest = ::exitApplication,
        title = "MUBS 市政城管工单系统"
    ) {
        App()
    }
}
