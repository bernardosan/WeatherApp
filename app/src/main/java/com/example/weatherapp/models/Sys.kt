package com.example.weatherapp.models

import java.io.Serializable

data class Sys(
    val id: Int,
    val name: String,
    val cod: Int,
    val sunrise: Int,
    val sunset: Int
) : Serializable
