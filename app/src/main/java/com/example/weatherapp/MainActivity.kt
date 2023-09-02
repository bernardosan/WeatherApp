package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActionBar
import android.app.Dialog
import android.content.*
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
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
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import retrofit.*
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.drawable.ColorDrawable
import android.os.CountDownTimer
import android.text.method.LinkMovementMethod
import android.widget.Button
import android.widget.TextView
import com.example.weatherapp.Constants.LAST_UPDATE_DATE_KEY
import com.example.weatherapp.Constants.LATITUDE_KEY
import com.example.weatherapp.Constants.LONGITUDE_KEY
import com.example.weatherapp.Constants.getCurrentDate
import com.example.weatherapp.Constants.getCurrentDateTime
import com.example.weatherapp.Constants.getUnit
import com.example.weatherapp.Constants.hasPassedMinutes
import com.example.weatherapp.Constants.unixTime
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.reference_dialog.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {


    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    private var mProgressDialog: Dialog? = null

    private var mLatitude: Double = 0.0

    private var mLongitude: Double = 0.0

    private lateinit var mSharedPreferences: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mSharedPreferences = getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE)

        setupUI(false)

        srl_layout.setOnRefreshListener {

            getLocationWeatherDetails()

        }


        if (!isLocationEnabled()) {
            Toast.makeText(this, "Your location provider is turned off. Please turn it on.", Toast.LENGTH_SHORT).show()

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
                            Toast.makeText(this@MainActivity, "You have denied location permission. Please allow it is mandatory.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>?, token: PermissionToken?) {
                        showRationalDialogForPermissions()
                    }
                }).onSameThread().check()
        }


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.btn_refresh -> {
                getLocationWeatherDetails()
                true
            }
            R.id.btn_about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun isLocationEnabled(): Boolean {

        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this).setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings").setPositiveButton("GO TO SETTINGS") { _, _ ->
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
            }
        }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {

        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {

            val mLastLocation: Location? = locationResult.lastLocation
            if (mLastLocation != null) {
                mLatitude = mLastLocation.latitude
            }
            Log.e("Current Latitude", "$mLatitude")
            if (mLastLocation != null) {
                mLongitude = mLastLocation.longitude
            }
            Log.e("Current Longitude", "$mLongitude")


            setupUI(true)


        }
    }


    private fun getLocationWeatherDetails() {

        if (Constants.isNetworkAvailable(this@MainActivity)) {

            val retrofit: Retrofit = Retrofit.Builder().baseUrl(Constants.BASE_URL).addConverterFactory(GsonConverterFactory.create()).build()

            val service: WeatherService = retrofit.create(WeatherService::class.java)

            val listCall: Call<WeatherResponse> = service.getWeather(mLatitude, mLongitude, Constants.METRIC_UNIT, Constants.APP_ID)

            showCustomProgressDialog()

            listCall.enqueue(object : Callback<WeatherResponse> {
                @RequiresApi(Build.VERSION_CODES.N)
                @SuppressLint("SetTextI18n")
                override fun onResponse(
                    response: Response<WeatherResponse>,
                    retrofit: Retrofit
                ) {

                    if (response.isSuccess) {

                        hideProgressDialog()

                        val weatherList: WeatherResponse = response.body()
                        Log.i("Response Result", "$weatherList")

                        val weatherResponseJsonString = Gson().toJson(weatherList)

                        val editor = mSharedPreferences.edit()
                        editor.putString(Constants.WEATHER_RESPONSE_DATE, weatherResponseJsonString)
                        editor.apply()


                        if(srl_layout.isRefreshing){
                            srl_layout.isRefreshing = false
                        }

                        setupUI(true)

                        mSharedPreferences.edit().putString(LAST_UPDATE_DATE_KEY, tv_last_update.text.toString()).apply()

                    } else {

                        when (response.code()) {
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

                        if(srl_layout.isRefreshing){
                            srl_layout.isRefreshing = false
                        }
                    }
                }

                override fun onFailure(t: Throwable) {
                    hideProgressDialog()
                    Log.e("Error", t.message.toString())
                }
            })
        } else {
            Toast.makeText(this@MainActivity, "No internet connection available.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCustomProgressDialog() {

        mProgressDialog = Dialog(this)

        mProgressDialog!!.setContentView(R.layout.custom_dialog)

        mProgressDialog!!.show()
    }

    private fun hideProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog!!.dismiss()
        }
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.N)
    private fun setupUI(refresh: Boolean = false) {

        val weatherResponseJsonString = mSharedPreferences.getString(Constants.WEATHER_RESPONSE_DATE, "")

        if (!weatherResponseJsonString.isNullOrEmpty()) {

            val weatherList = Gson().fromJson(weatherResponseJsonString, WeatherResponse::class.java)

            for (z in weatherList.weather.indices) {
                Log.i("NAME", weatherList.weather[z].main)

                tv_main.text = weatherList.weather[z].main
                tv_main_description.text = weatherList.weather[z].description
                tv_temp.text = weatherList.main.temp.roundToInt().toString() + getUnit(application.resources.configuration.locales.toString())
                tv_humidity.text = weatherList.main.humidity.toString() + "%"
                tv_min.text = floor(weatherList.main.temp_min).roundToInt().toString() + getUnit(application.resources.configuration.locales.toString())
                tv_max.text = ceil(weatherList.main.temp_max).roundToInt().toString() + getUnit(application.resources.configuration.locales.toString())
                tv_speed.text = String.format("%.3s", weatherList.wind.speed.toString())
                tv_name.text = weatherList.name
                tv_country.text = weatherList.sys.country
                tv_sunrise_time.text = unixTime(weatherList.sys.sunrise)
                tv_sunset_time.text = unixTime(weatherList.sys.sunset)

                when (weatherList.weather[z].icon) {
                    "01d" -> iv_main.setImageResource(R.drawable.sunny)
                    "02d" -> iv_main.setImageResource(R.drawable.cloud)
                    "03d" -> iv_main.setImageResource(R.drawable.cloud)
                    "04d" -> iv_main.setImageResource(R.drawable.cloud)
                    "04n" -> iv_main.setImageResource(R.drawable.cloud)
                    "10d" -> iv_main.setImageResource(R.drawable.rain)
                    "11d" -> iv_main.setImageResource(R.drawable.storm)
                    "13d" -> iv_main.setImageResource(R.drawable.snowflake)
                    "01n" -> iv_main.setImageResource(R.drawable.cloud)
                    "02n" -> iv_main.setImageResource(R.drawable.cloud)
                    "03n" -> iv_main.setImageResource(R.drawable.cloud)
                    "10n" -> iv_main.setImageResource(R.drawable.cloud)
                    "11n" -> iv_main.setImageResource(R.drawable.rain)
                    "13n" -> iv_main.setImageResource(R.drawable.snowflake)
                }
            }

            tv_date.text = getCurrentDate()

            if(refresh){
                tv_last_update.text = "Last update: ${getCurrentDateTime()}"
            } else{
                tv_last_update.text = mSharedPreferences.getString(LAST_UPDATE_DATE_KEY, "0")
            }
        }
    }

    private fun showAboutDialog() {
        val view = layoutInflater.inflate(R.layout.reference_dialog, null)
        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .show()
        val btn_about_close = view.findViewById<Button>(R.id.btn_about_close)
        val tv_openWeather = view.findViewById<TextView>(R.id.tv_about)

        tv_openWeather.setMovementMethod(LinkMovementMethod.getInstance());

        btn_about_close.setOnClickListener {
            dialog.dismiss()
        }

    }



}