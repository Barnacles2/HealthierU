package com.example.healthieru.data.remote


data class AdviceResponse(val slip: Slip)

data class Slip(val id: Int, val advice: String)