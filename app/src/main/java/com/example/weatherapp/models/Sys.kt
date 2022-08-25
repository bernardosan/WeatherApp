package com.example.weatherapp.models

import java.io.Serializable

data class Sys(
    val id: Int,
    val name: String,
    val cod: Int,
    val country: String,
    val sunrise: Long,
    val sunset: Long
) : Serializable
