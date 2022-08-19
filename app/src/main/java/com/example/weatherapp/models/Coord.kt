package com.example.weatherapp.models

import java.io.Serializable

data class Coord(
   val lon: Double = 0.0,
   val lat: Double = 0.0
) : Serializable
