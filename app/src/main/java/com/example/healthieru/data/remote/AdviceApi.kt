package com.example.healthieru.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface AdviceApi {
    // Fetch a list of exercises in English (language=2)
    @GET("api/v2/exercise/")
    suspend fun getExercises(
        @Query("language") language: Int = 2,
        @Query("limit") limit: Int = 1
    ): ExerciseResponse
}