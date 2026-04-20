package com.mubs.mobile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.navigator.Navigator
import com.mubs.mobile.di.AppModule
import com.mubs.mobile.ui.login.LoginScreen
import com.mubs.mobile.ui.theme.MubsTheme
import com.mubs.mobile.ui.tickets.TicketListScreen

@Composable
fun App() {
    var startScreen by remember { mutableStateOf<Any?>(null) }

    LaunchedEffect(Unit) {
        val isLoggedIn = AppModule.instance.authRepository.isLoggedIn()
        startScreen = if (isLoggedIn) TicketListScreen() else LoginScreen()
    }

    MubsTheme {
        val screen = startScreen
        if (screen != null) {
            Navigator(screen as cafe.adriel.voyager.core.screen.Screen)
        }
    }
}
