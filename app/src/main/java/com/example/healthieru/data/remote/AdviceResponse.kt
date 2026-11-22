package com.example.healthieru.data.remote

import com.google.gson.annotations.SerializedName

data class ExerciseResponse(
    val count: Int,
    val results: List<Exercise>
)

data class Exercise(
    val id: Int,
    val name: String,
    val description: String,
    @SerializedName("category") val categoryId: Int
)