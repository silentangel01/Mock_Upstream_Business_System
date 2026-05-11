package com.mubs.mobile.data.api

class ApiException(
    val statusCode: Int,
    message: String
) : Exception(message)
