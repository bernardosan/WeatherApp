package com.example.weatherapp

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.CountDownTimer
import android.util.Log
import com.example.weatherapp.network.WeatherResponse
import java.text.SimpleDateFormat
import java.util.*

object Constants {

    const val APP_ID: String = "8c97687b5211d1cd7cafcf69cc30b84e"
    const val BASE_URL: String = "http://api.openweathermap.org/data/"
    const val METRIC_UNIT: String = "metric"
    const val PREFERENCE_NAME = "WeatherAppPreference"
    const val WEATHER_RESPONSE_DATE = "weather_response_data"
    const val LAST_UPDATE_DATE_KEY = "calendar"
    const val LATITUDE_KEY = "latitude"
    const val LONGITUDE_KEY = "longitude"

    /**
     * This function is used check the weather the device is connected to the Internet or not.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        // It answers the queries about the state of network connectivity.
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network      = connectivityManager.activeNetwork ?: return false
            val activeNetWork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetWork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetWork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                //for other device how are able to connect with Ethernet
                activeNetWork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            // Returns details about the currently active default data network.
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnectedOrConnecting
        }
    }

     fun getUnit(value: String): String {
        Log.i("unit", value)
        var value = "°C"
        if ("US" == value || "LR" == value || "MM" == value) {
            value = "°F"
        }
        return value
    }

     fun unixTime(timex: Long): String? {
        val date = Date(timex * 1000L)
        @SuppressLint("SimpleDateFormat") val sdf =
            SimpleDateFormat("HH:mm:ss")
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }


    fun getCurrentDateTime(): String? {
        val formatter = SimpleDateFormat("HH:mm:ss dd/MM/yyyy ", Locale.UK)
        return formatter.format(Calendar.getInstance().time)
    }


    fun hasPassedMinutes(dateString: String, secondsThreshold: Int): Boolean {
        val dateFormat = SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.UK)

        try {
            // Convert the input date string to a Date object
            val inputDate = dateFormat.parse(dateString)

            // Get the current time
            val currentTime = Calendar.getInstance().time

            // Calculate the time difference between the current time and the input date in milliseconds
            val timeDifferenceMillis = currentTime.time - inputDate.time

            // Convert the time difference to minutes
            val secondsPassed = (timeDifferenceMillis) / 1000

            // Compare with the specified threshold
            return secondsPassed > secondsThreshold

        } catch (e: java.text.ParseException) {
            println("Invalid date format or input.")
            return false
        }
    }


}