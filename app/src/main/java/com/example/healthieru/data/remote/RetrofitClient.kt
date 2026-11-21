package com.example.healthieru.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    val adviceApi: AdviceApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.adviceslip.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AdviceApi::class.java)
    }
}