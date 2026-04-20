package com.mubs.mobile.di

import com.mubs.mobile.data.api.AuthApi
import com.mubs.mobile.data.api.TicketApi
import com.mubs.mobile.data.api.createHttpClient
import com.mubs.mobile.data.repository.AuthRepository
import com.mubs.mobile.data.repository.TicketRepository
import com.mubs.mobile.domain.SessionManager
import io.ktor.client.HttpClient

class AppModule(sessionManager: SessionManager) {
    val session: SessionManager = sessionManager

    val httpClient: HttpClient = createHttpClient(session)

    val authApi: AuthApi = AuthApi(httpClient)
    val ticketApi: TicketApi = TicketApi(httpClient)

    val authRepository: AuthRepository = AuthRepository(authApi, session)
    val ticketRepository: TicketRepository = TicketRepository(ticketApi)

    companion object {
        lateinit var instance: AppModule
            private set

        fun init(sessionManager: SessionManager) {
            instance = AppModule(sessionManager)
        }
    }
}
