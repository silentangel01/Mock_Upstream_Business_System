package com.mubs.mobile

import androidx.compose.ui.window.ComposeUIViewController
import com.mubs.mobile.di.AppModule
import com.mubs.mobile.domain.SessionManager

fun MainViewController() = ComposeUIViewController {
    AppModule.init(SessionManager())
    App()
}
