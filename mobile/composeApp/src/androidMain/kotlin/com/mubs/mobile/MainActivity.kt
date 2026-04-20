package com.mubs.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mubs.mobile.di.AppModule
import com.mubs.mobile.domain.SessionManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppModule.init(SessionManager(applicationContext))
        setContent {
            App()
        }
    }
}
