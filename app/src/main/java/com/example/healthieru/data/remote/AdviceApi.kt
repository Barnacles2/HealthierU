package com.example.healthieru.data.remote

import retrofit2.http.GET

interface AdviceApi {
    @GET("advice")
    suspend fun getAdvice(): AdviceResponse
}