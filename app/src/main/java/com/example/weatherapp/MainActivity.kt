package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.example.weatherapp.network.WeatherResponse
import com.example.weatherapp.network.WeatherService
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the Fused location variable
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (!isLocationEnabled()) {
            Toast.makeText(
                this,
                "Your location provider is turned off. Please turn it on.",
                Toast.LENGTH_SHORT
            ).show()

            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            Dexter.withActivity(this)
                .withPermissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report!!.areAllPermissionsGranted()) {
                            requestLocationData()
                        }

                        if (report.isAnyPermissionPermanentlyDenied) {
                            Toast.makeText(
                                this@MainActivity,
                                "You have denied location permission. Please allow it is mandatory.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogForPermissions()
                    }
                }).onSameThread()
                .check()
        }
    }
    private fun isLocationEnabled(): Boolean {

        // This provides access to the system location services.
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton(
                "GO TO SETTINGS"
            ) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog,
                                           _ ->
                dialog.dismiss()
            }.show()
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {

        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation!!
            val latitude = mLastLocation.latitude
            Log.i("Current Latitude", "$latitude")

            val longitude = mLastLocation.longitude
            Log.i("Current Longitude", "$longitude")

            getLocationWeatherDetails(latitude, longitude)
        }
    }

    private fun getLocationWeatherDetails(latitude: Double, longitude: Double) {

        if (Constants.isNetworkAvailable(this@MainActivity)) {

            val customProgressDialog = Dialog(this)
            customProgressDialog.setContentView(R.layout.custom_dialog)
            customProgressDialog.setCancelable(false)
            customProgressDialog.show()

            val retrofit: Retrofit = Retrofit.Builder()
                // API base URL.
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service: WeatherService =
                retrofit.create<WeatherService>(WeatherService::class.java)

            val listCall: Call<WeatherResponse> = service.getWeather(latitude, longitude, Constants.METRIC_UNIT, Constants.APP_ID)

            listCall.enqueue(object : Callback<WeatherResponse> {
                @SuppressLint("SetTextI18n")

                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {

                    if (response.isSuccessful) {

                        val weatherList: WeatherResponse = response.body()!!
                        Log.i("Response Result", "$weatherList")
                        customProgressDialog.dismiss()
                        setupUi(weatherList)

                    } else {
                        // If the response is not success then we check the response code.
                        val sc = response.code()
                        when (sc) {
                            400 -> {
                                Log.e("Error 400", "Bad Request")
                            }
                            404 -> {
                                Log.e("Error 404", "Not Found")
                            }
                            else -> {
                                Log.e("Error", "Generic Error")
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Log.e("Errorrrrr", t.message.toString())
                }
            })

        } else {
            Toast.makeText(
                this@MainActivity,
                "No internet connection available.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupUi(weatherList: WeatherResponse){
        for(i in weatherList.weather.indices){
            Log.e("weather name", weatherList.weather.toString())
            tv_main.text = weatherList.weather[i].main
            tv_main_description.text = weatherList.weather[i].description
            tv_temp.text = weatherList.main.temp.toString() + getUnit(application.resources.configuration.locales.toString())
            tv_speed.text = weatherList.wind.speed.toString()
            tv_humidity.text = weatherList.main.humidity.toString() + "% RH"
            tv_max.text =  weatherList.main.temp_max.toString() + "º max"
            tv_min.text =  weatherList.main.temp_min.toString() + "º min"
            tv_country.text = weatherList.sys.country.toString()
            tv_name.text = weatherList.name
            tv_sunrise_time.text = timetoDateConverter(weatherList.sys.sunrise)
            tv_sunset_time.text = timetoDateConverter(weatherList.sys.sunset)

            when(weatherList.weather[i].icon){
                "01d" -> iv_main.setImageResource(R.drawable.sunny)
                "02d" -> iv_main.setImageResource(R.drawable.cloud)
                "03d" -> iv_main.setImageResource(R.drawable.cloud)
                "04d" -> iv_main.setImageResource(R.drawable.cloud)
                "04n" -> iv_main.setImageResource(R.drawable.cloud)
                "10d" -> iv_main.setImageResource(R.drawable.rain)
                "11d" -> iv_main.setImageResource(R.drawable.storm)
                "13d" -> iv_main.setImageResource(R.drawable.snowflake)
                "01n" -> iv_main.setImageResource(R.drawable.sunny)
                "02n" -> iv_main.setImageResource(R.drawable.cloud)
                "03n" -> iv_main.setImageResource(R.drawable.cloud)
                "10n" -> iv_main.setImageResource(R.drawable.cloud)
                "11n" -> iv_main.setImageResource(R.drawable.storm)
                "13n" -> iv_main.setImageResource(R.drawable.snowflake)
            }
        }
    }

    private fun getUnit(parameter: String): String?{
        var unit  = "ºC"
        if(parameter ==  "US" || parameter == "LR" || parameter == "MM" ){
            unit  = "ºF"
        }
        return unit
    }

    private fun timetoDateConverter(time: Long) : String?{
        val date = Date(time * 1000L)
        val sdf = SimpleDateFormat("HH:mm", Locale.UK)
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.btn_refresh ->{
                requestLocationData()
                true
            } else -> return super.onOptionsItemSelected(item)
        }
    }

}