package com.mubs.mobile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import com.mubs.mobile.di.AppModule
import com.mubs.mobile.navigation.DeepLink
import com.mubs.mobile.ui.detail.TicketDetailScreen
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
            Navigator(screen as cafe.adriel.voyager.core.screen.Screen) { navigator ->
                // Handle deep link navigation
                val pendingTicketId by DeepLink.pendingTicketId.collectAsState()
                LaunchedEffect(pendingTicketId) {
                    val ticketId = DeepLink.consume() ?: return@LaunchedEffect
                    if (navigator.lastItem !is LoginScreen) {
                        navigator.push(TicketDetailScreen(ticketId))
                    }
                }
                CurrentScreen()
            }
        }
    }
}
