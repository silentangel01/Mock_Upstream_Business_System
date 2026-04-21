package com.mubs.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mubs.mobile.di.AppModule
import com.mubs.mobile.domain.SessionManager
import com.mubs.mobile.navigation.DeepLink

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppModule.init(SessionManager(applicationContext))
        handleDeepLink(intent)
        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        // mubs://tickets/{ticketId}
        val uri = intent?.data ?: return
        if (uri.scheme == "mubs" && uri.host == "tickets") {
            val ticketId = uri.pathSegments?.firstOrNull() ?: return
            DeepLink.offer(ticketId)
        }
    }
}
