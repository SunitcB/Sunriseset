package com.bright.sunriseset

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.icu.text.SimpleDateFormat
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bright.sunriseset.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

class PlanetInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var chineseLanguageCode = "zh"
    private var chineseCountryCode = "CN"
    private var chinaLat: Double = 35.8617
    private var chinaLong: Double = 104.1954
    private var isSwitchChangingLocale = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lat: Double = 0.0
    private var long: Double = 0.0

    /**
     * Called when the activity is first created. This function initializes the activity, inflates the layout,
     * retrieves the current time, and asynchronously fetches sunrise and sunset times from an API.
     * The fetched times are then dynamically localized and displayed in Chinese on TextViews.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     *                           this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Locale.getDefault().language
        // Inflate the layout using View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.languageSwicth.isChecked = isChineseLocale()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (checkLocationPermission()) {
            getLastLocation()
        } else {
            requestLocationPermission()
        }

        // Get the current time
        val currentTime = LocalDateTime.now()

        if (!isSwitchChangingLocale) {
            fetchSunriseSunSetTimes(resources.configuration.locale.language)
        }
        binding.languageSwicth.setOnCheckedChangeListener { buttonView, isChecked ->

            if (!isSwitchChangingLocale) {
                updateLocale(isChecked)
            }
        }
    }

    private fun isChineseLocale(): Boolean {
        val currentLocale = resources.configuration.locale
        return currentLocale.language == chineseLanguageCode
    }

    private fun updateLocale(isChinese: Boolean) {
        val targetLocale = if (isChinese) {
            fetchSunriseSunSetTimes(chineseLanguageCode)
            lat = chinaLat
            long = chinaLong
            Locale(chineseLanguageCode, chineseCountryCode)
        } else {
            fetchSunriseSunSetTimes(Locale.getDefault().language)
            getLastLocation()
            Locale.getDefault()
        }

        val currentLocale = resources.configuration.locale
        if (currentLocale != targetLocale) {
            isSwitchChangingLocale = true
            val configuration = resources.configuration
            configuration.setLocale(targetLocale)
            resources.updateConfiguration(configuration, resources.displayMetrics)
            recreate()
        }
    }

    fun fetchSunriseSunSetTimes(languageCode: String){
        GlobalScope.launch(Dispatchers.Main) {
            val sunriseDeferred = async(Dispatchers.IO) { fetchTime("sunrise") }
            val sunsetDeferred = async(Dispatchers.IO) { fetchTime("sunset") }

            // Await the results of asynchronous tasks
            val sunriseTime = sunriseDeferred.await()
            val sunsetTime = sunsetDeferred.await()

            // If both sunrise and sunset times are available, localize and display them in Chinese
            if (sunriseTime != null && sunsetTime != null) {
                // Localize sunrise and sunset times
                val localizedSunrise = getLocalizedTime(sunriseTime, this@PlanetInfoActivity, languageCode)
                val localizedSunset = getLocalizedTime(sunsetTime, this@PlanetInfoActivity, languageCode)

                // Display localized times on TextViews
                binding.textViewSunrise.text =
                    "${getString(R.string.SunriseTime)} $localizedSunrise"
                binding.textViewSunset.text =
                    "${getString(R.string.SunsetTime)} $localizedSunset"
            }
        }
    }

    /**
     * Retrieves a localized time string based on the user's preferred language.
     *
     * @param time The LocalDateTime to be formatted.
     * @param context The application context to access resources and preferences.
     * @return A string representation of the localized time.
     */
    private fun getLocalizedTime(time: LocalDateTime, context: Context, userPreferredLanguage: String): String {
        // Retrieve the user's preferred language from the device settings


        // Create a SimpleDateFormat with the user's preferred language
        val sdf = SimpleDateFormat("hh:mm a", Locale(userPreferredLanguage))

        // Format the LocalDateTime into a string using the specified SimpleDateFormat
        return sdf.format(
            time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
    }

    private fun checkLocationPermission(): Boolean {
        return (ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                // Got last known location
                location?.let {
                    lat= it.latitude
                    long = it.longitude
                }
            }
            .addOnFailureListener { exception ->
                // Handle failure to get location
            }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }


    // Coroutine function to fetch sunrise or sunset time from the Sunrise-Sunset API
    private suspend fun fetchTime(type: String): LocalDateTime? {
        return try {
            val apiUrl =
                URL("https://api.sunrise-sunset.org/json?lat=${lat}&lng=${long}&formatted=0")
            val urlConnection: HttpURLConnection = apiUrl.openConnection() as HttpURLConnection
            try {
                val reader = BufferedReader(InputStreamReader(urlConnection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }

                val jsonResponse = JSONObject(response.toString())
                val timeUTC = jsonResponse.getJSONObject("results").getString(type)
                val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.getDefault())
                val dateTime = formatter.parse(timeUTC)
                LocalDateTime.ofInstant(dateTime.toInstant(), ZoneId.systemDefault())
            } finally {
                urlConnection.disconnect()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}