package com.example.weatherapplicatio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.weatherapplicatio.Models.WeatherModel
import com.example.weatherapplicatio.Utilites.ApiUtilities
import com.example.weatherapplicatio.databinding.ActivityMainBinding
//import com.example.weatherapplicatio.databinding.ActivityMainBindingImpl
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

//    lateinit var binding: ActivityMainBinding
    private val binding by lazy {
ActivityMainBinding.inflate(layoutInflater)
}

    private lateinit var currentLocation: Location

    private lateinit var fusedLocationProvider: FusedLocationProviderClient

    private val LOCATION_REQUEST_CODE = 101

    private val apiKey = "3b1422f5dbb3074f308bab3be8ffa98a"

    private var mInterstitialAd: InterstitiatlAd? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

//        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this)

        getCurrentLocation()

        binding.citySearch.setOnEditorActionListener { v, actionId, event ->

            if (actionId == EditorInfo.IME_ACTION_SEARCH) {

                getCityWeather(binding.citySearch.text.toString())

                val view = this.currentFocus

                if (view != null) {

                    val imm: InputMethodManager =
                        getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)


                    binding.citySearch.clearFocus()
                }
                return@setOnEditorActionListener true
            } else {

                return@setOnEditorActionListener false
            }
        }

        binding.currentLocation.setOnClickListener {

            getCurrentLocation()
        }


    }

    private fun getCityWeather(city: String) {
        binding.progressBar.visibility = View.VISIBLE

        ApiUtilities.getApiInterfce()?.getCityWeatherData(city, apiKey)?.enqueue(
            object : Callback<WeatherModel> {

                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(
                    call: Call<WeatherModel>,
                    response: Response<WeatherModel>
                ) {
                    if (response.isSuccessful) {


                        binding.progressBar.visibility = View.GONE

                       response.body()?.let {
                           setData(it)
                        }
                    }
                    else {
                        Toast.makeText(this@MainActivity, "NO CITY FOUND", Toast.LENGTH_SHORT)
                            .show()

                        binding.progressBar.visibility = View.GONE
                    }

                }

                override fun onFailure(call: Call<WeatherModel>, t: Throwable) {


                }

            })
    }


    private fun fetchCurrentLocationWeather(latitude: String, longitude: String) {

        ApiUtilities.getApiInterfce()?.CurrentWeatherData(latitude, longitude, apiKey)
            ?.enqueue(object : Callback<WeatherModel> {

                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(
                    call: Call<WeatherModel>,
                    response: Response<WeatherModel>
                ) {

                    if (response.isSuccessful) {
                        binding.progressBar.visibility = View.GONE

                        response.body()?.let {
                            setData(it)
                        }
                    }
                }

                override fun onFailure(call: Call<WeatherModel>, t: Throwable) {
                }

            })

    }

    private fun getCurrentLocation() {
        if (checkPermissions()) {

            if (isLocationEnabled()) {

                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions()

                    return
                }

                fusedLocationProvider.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            currentLocation = location
                            binding.progressBar.visibility = View.VISIBLE

                            fetchCurrentLocationWeather(
                                location.latitude.toString(),
                                location.longitude.toString()
                            )
                        }
                    }
            }
            else {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        }
        else {
            requestPermissions()
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            LOCATION_REQUEST_CODE
        )
    }

    private fun isLocationEnabled(): Boolean {

        val locationManager: LocationManager = getSystemService(LOCATION_SERVICE)
                as LocationManager

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }


    private fun checkPermissions(): Boolean {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            return true
        }

        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_REQUEST_CODE) {

            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {

            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setData(body: WeatherModel) {
        binding.apply {
            val currentData = SimpleDateFormat("dd/MM/YYYY hh:mm").format(Date())

            dateTime.text = currentData.toString()

            maxTemp.text = "Max " + k2c(body?.main?.temp_max!!) + "°"

            minTemp.text = "Min " + k2c(body?.main?.temp_min!!) + "°"

            temp.text = "" + k2c(body?.main?.temp!!) + "°"

            weatherTitle.text = body.weather[0].main

//            if(binding.weatherTitle.text.toString().equals("Clear")){
//                Toast.makeText(this@MainActivity, "clear", Toast.LENGTH_SHORT).show()
//                binding.animationView1.visibility=View.VISIBLE
//
//
//
//            }
            sunriseValue.text = tst2d(body.sys.sunrise.toLong())

            sunsetValue.text = tst2d(body.sys.sunset.toLong())

            pressureValue.text = body.main.pressure.toString()

            humidityValue.text = body.main.humidity.toString() + "%"

            tempFValue.text = " " + k2c(body.main.temp).times(1.8).plus(32)
                .roundToInt() + ""

            citySearch.setText(body.name)

            feelsLike.text = "" + k2c(body?.main?.feels_like!!) + "°"

            windValue.text = body.wind.speed.toString() + "m/s"

            groundValue.text = body.main.grnd_level.toString()

            seaValue.text = body.main.sea_level.toString()

            countryValue.text = body.sys.country
        }

        updateUI(body.weather[0].id)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun tst2d(ts: Long): String {
        val localTime = ts.let {
            Instant.ofEpochSecond(it)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
        }

        return localTime.toString()
    }

    private fun k2c(t: Double): Double {

        var inTemp = t

        inTemp = inTemp.minus(273)

        return inTemp.toBigDecimal().setScale(1, RoundingMode.UP).toDouble()
    }


    private fun updateUI(id: Int) {
        binding.apply {

            when (id) {
                //Thunderstorm
                in 200..232 -> {

                    weatherimg.setImageResource(R.drawable.ic_storm_weather)

                    mainLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.thunderstrom_bg)
                    optionsLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.thunderstrom_bg)
//                    animationView.visibility=View.VISIBLE
//                    animationView1.visibility=View.VISIBLE
                    binding.rain.visibility=View.GONE
                    binding.clear.visibility=View.GONE
                    binding.drizleday.visibility=View.GONE
                    binding.thnderstrom.visibility=View.VISIBLE
                    binding.cloud.visibility=View.GONE
                    binding.snow.visibility=View.GONE
                    binding.haze.visibility=View.GONE





                }

                //Drizzle
                in 300..321 -> {

                    weatherimg.setImageResource(R.drawable.ic_few_clouds)

                    mainLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.drizzle_bg)
                    optionsLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.drizzle_bg)
//                    animationView.visibility=View.VISIBLE
//                    animationView1.visibility=View.VISIBLE
                    binding.rain.visibility=View.GONE
                    binding.clear.visibility=View.GONE
                    binding.drizleday.visibility=View.VISIBLE
                    binding.thnderstrom.visibility=View.GONE
                    binding.cloud.visibility=View.GONE
                    binding.snow.visibility=View.GONE
                    binding.haze.visibility=View.GONE






                }

                //Rain
                in 500..531 -> {

                    weatherimg.setImageResource(R.drawable.ic_rainy_weather)

                    mainLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.rain_bg)
                    optionsLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.rain_bg)
//                    animationView.visibility=View.VISIBLE
//                    animationView1.visibility=View.VISIBLE
                    binding.rain.visibility=View.VISIBLE
                    binding.clear.visibility=View.GONE
                    binding.drizleday.visibility=View.GONE
                    binding.thnderstrom.visibility=View.GONE
                    binding.cloud.visibility=View.GONE

                    binding.snow.visibility=View.GONE
                    binding.haze.visibility=View.GONE






                }

                //Snow
                in 600..622 -> {

                    weatherimg.setImageResource(R.drawable.ic_snow_weather)

                    mainLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.snow_bg)
                    optionsLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.snow_bg)
//                    animationView.visibility=View.VISIBLE
//                    animationView1.visibility=View.VISIBLE
                    binding.rain.visibility=View.GONE
                    binding.clear.visibility=View.GONE
                    binding.drizleday.visibility=View.GONE
                    binding.thnderstrom.visibility=View.GONE
                    binding.cloud.visibility=View.GONE
                    binding.snow.visibility=View.VISIBLE
                    binding.haze.visibility=View.GONE






                }

                //Atmosphere
                in 701..781 -> {

                    weatherimg.setImageResource(R.drawable.ic_broken_clouds)

                    mainLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.atmosphere_bg)
                    optionsLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.atmosphere_bg)
//                    animationView.visibility=View.VISIBLE
//                    animationView1.visibility=View.VISIBLE
                    binding.rain.visibility=View.GONE
                    binding.clear.visibility=View.GONE
                    binding.drizleday.visibility=View.GONE
                    binding.thnderstrom.visibility=View.GONE
                    binding.cloud.visibility=View.GONE
                    binding.snow.visibility=View.GONE
                    binding.haze.visibility=View.VISIBLE






                }

                //Clear
                800 -> {
//                    animationView.visibility=View.VISIBLE
                    weatherimg.setImageResource(R.drawable.ic_clear_day)

                    mainLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.clear_bg)
                    optionsLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.clear_bg)
//                    binding.animationView1.visibility=View.VISIBLE
//                    animationView1.visibility=View.VISIBLE
                    binding.clear.visibility=View.VISIBLE
                    binding.rain.visibility=View.GONE
                    binding.drizleday.visibility=View.GONE
                    binding.thnderstrom.visibility=View.GONE
                    binding.cloud.visibility=View.GONE
                    binding.snow.visibility=View.GONE
                    binding.haze.visibility=View.GONE






                }

                //Clouds
                in 801..804 -> {
                    weatherimg.setImageResource(R.drawable.ic_cloudy_weather)

                    mainLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.clouds_bg)
                    optionsLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.clouds_bg)
//                    animationView.visibility=View.VISIBLE
//                    animationView1.visibility=View.VISIBLE
                    binding.clear.visibility=View.GONE
                    binding.rain.visibility=View.GONE
                    binding.drizleday.visibility=View.GONE
                    binding.thnderstrom.visibility=View.GONE
                    binding.cloud.visibility=View.VISIBLE
                    binding.snow.visibility=View.GONE
                    binding.haze.visibility=View.GONE





                }

                //Unknown
                else -> {
                    weatherimg.setImageResource(R.drawable.ic_unknown)

                    mainLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.unknown_bg)
                    optionsLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.unknown_bg)
//                    animationView1.visibility=View.VISIBLE
                    binding.clear.visibility=View.GONE
                    binding.rain.visibility=View.GONE
                    binding.drizleday.visibility=View.GONE
                    binding.thnderstrom.visibility=View.GONE
                    binding.cloud.visibility=View.GONE
                    binding.snow.visibility=View.GONE
                    binding.haze.visibility=View.GONE






                }

            }

        }
    }
}


