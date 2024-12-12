package com.example.badeapp.viewmodel


import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.badeapp.api.HavvarselFrostDto
import com.example.badeapp.api.LocationForecast
import com.example.badeapp.api.OceanForecast
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitString
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*


class KartActivityViewModel : ViewModel() {


    val forecastModel = MutableLiveData<MutableList<String>>()
    val linkModel = MutableLiveData<Boolean>()


    fun hentKoordinater(sharedPreferences: SharedPreferences) {
        /**Sjekker lagret API-data.*/
        val tidSiste = sharedPreferences.getString("tidString", "")
        val tidArraySiste = tidSiste?.split(":")?.toTypedArray()
        val tidNaa = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val tidArrayNaa = tidNaa.split(":").toTypedArray()
        val dato: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val dataString = sharedPreferences.getString("dataString", "")
        var oppdater = true //Standard true. Sett false for å tvinge lagret data.
        if (dataString != "" && tidArraySiste!![0].toInt() == tidArrayNaa[0].toInt() && tidArraySiste[1].toInt() <= tidArrayNaa[1].toInt()) {
            oppdater = false //Standard false. Sett true for å tvinge nytt nettverkskall.
        }

        /**Hvis det skal gjøres nytt nettverkskall.*/
        if (oppdater) {
            Log.d("Oppdatert", oppdater.toString())
            //Bygger tid-token.
            val tidToken = "${dato}T${tidArrayNaa[0]}%3A00%3A00Z&incobs=true"
            val frostUrl = "http://havvarsel-frost.met.no/api/v1/obs/badevann/get?time=2020-01-01T00%3A00%3A00Z%2F$tidToken"
            //Nettverkskall.
            var responsFrost: HavvarselFrostDto
            runBlocking {
                responsFrost = Gson().fromJson(Fuel.get(frostUrl).awaitString(), HavvarselFrostDto::class.java)
            }
            hentNyttForecast(responsFrost, sharedPreferences)
        }

        /**Hvis det ikke skal gjøres nytt nettverkskall.*/
        else if (!oppdater) {
            Log.d("Oppdatert", oppdater.toString())
            hentGammeltForecast(sharedPreferences)
        }
    }


    private fun hentGammeltForecast(sharedPreferences: SharedPreferences) {
        /**Henter ut lagret data om temperaturer fra SharedPreferences.*/
        viewModelScope.launch {
            val dataString = sharedPreferences.getString("dataString", "")
            val responsForecasts = mutableListOf<String>()
            val array = dataString?.split("@")?.toTypedArray()
            for (x in array!!) {
                val params = x.split("#").toTypedArray()
                responsForecasts.add("${params[0]}#${params[1]}#${params[2]}#${params[3]}#${params[4]}#${params[5]}#${params[6]}#${params[7]}#${params[8]}")
            }
            forecastModel.postValue(responsForecasts)
        }
    }


    private fun hentNyttForecast(responsFrost: HavvarselFrostDto, sharedPreferences: SharedPreferences) {
        /**Retrofit.*/
        val retrofit = Retrofit.Builder()
            .baseUrl("https://in2000-apiproxy.ifi.uio.no/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val serviceLocation: LocationForecast = retrofit.create(LocationForecast::class.java)
        val serviceOcean: OceanForecast = retrofit.create(OceanForecast::class.java)

        /**Coroutine som henter ut informasjon via nettverkskall.*/
        viewModelScope.launch {
            val tid = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val tidArray = tid.split(":").toTypedArray()
            val dato: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val responsForecasts = mutableListOf<String>()
            var nyDataString = ""

            /**Henter respons fra hvert koordinat i responsFrost.*/
            for (x in responsFrost.data.tseries) {
                val navn = x.header.extra.name
                val lat = x.header.extra.pos.lat.toDouble()
                val lon = x.header.extra.pos.lon.toDouble()

                /**Henter fra Locationforecast-API.*/
                val locationString = hentLocationForecast(serviceLocation, lat, lon, tidArray, dato)

                /**Henter fra Oceanforecast-API.*/
                val oceanString = hentOceanForecast(serviceOcean, lat, lon, tidArray, dato)

                /**Gjør om data til strenger slik at det kan lagres.*/
                val streng = "$navn#$lat#$lon#${tidArray[0]}#$locationString#$oceanString"
                responsForecasts.add(streng)
                nyDataString += if (nyDataString == "") {
                    streng
                } else {
                    "@$streng"
                }
            } //Slutt for-løkke.

            /**Lagrer data.*/
            val editor = sharedPreferences.edit()
            editor.clear()
            editor.putString("tidString", tid)
            editor.putString("dataString", nyDataString)
            editor.apply()

            /**Sender data til KartActivity.*/
            forecastModel.postValue(responsForecasts)
        }
    }





    private suspend fun hentLocationForecast(serviceLocation: LocationForecast, lat: Double, lon: Double, tidArray: Array<String>, dato: String): String {
        val responsLocation = serviceLocation.fetchForecast(lat, lon)
        var lufttemp = "NULL"
        var lufttid = "NULL"
        var varsel = ""
        if (responsLocation.properties.timeseries.isNotEmpty()) {
            var i = 0
            for (y in responsLocation.properties.timeseries) {
                val array = y.time.split("T").toTypedArray()
                if (array[0] == dato && array[1].split(":").toTypedArray()[0] == tidArray[0]) {
                    lufttemp = y.data.instant.details.air_temperature
                    val datoArray = array[0].split("-").toTypedArray()
                    lufttid = "Sist oppdatert ${datoArray[2]}.${datoArray[1]}, kl. ${tidArray[0]}"
                    for (j in 1 until 7) {
                        varsel += if (j == 1) {
                            responsLocation.properties.timeseries[j].data.instant.details.air_temperature
                        } else {
                            "¤${responsLocation.properties.timeseries[j].data.instant.details.air_temperature}"
                        }
                    }
                    break
                }
                i++
            }
        }
        return "$lufttemp#$lufttid#$varsel"
    }





    private suspend fun hentOceanForecast(serviceOcean: OceanForecast, lat: Double, lon: Double, tidArray: Array<String>, dato: String): String {
        val responsOcean = serviceOcean.fetchForecast(lat, lon)
        var vanntemp = "NULL"
        var vanntid = "NULL"
        if (responsOcean.properties.timeseries.isNotEmpty()) {
            for (y in responsOcean.properties.timeseries) {
                val array = y.time.split("T").toTypedArray()
                if (array[0] == dato && array[1].split(":").toTypedArray()[0] == tidArray[0]) {
                    vanntemp = y.data.instant.details.sea_water_temperature
                    val datoArray = array[0].split("-").toTypedArray()
                    vanntid = "Sist oppdatert ${datoArray[2]}.${datoArray[1]}, kl. ${tidArray[0]}"
                    break
                }
            }
        }
        return "$vanntemp#$vanntid"
    }





    @Suppress("BlockingMethodInNonBlockingContext")
    fun sjekkLink(url: String) {
        /**Sjekker om det finnes mer informasjon om badestedet på Oslo kommune sine nettisder.*/
        CoroutineScope(Dispatchers.Default).launch {
            val gyldig: Boolean
            val urlConnection = URL(url).openConnection() as HttpURLConnection
            when (urlConnection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    gyldig = true
                }
                HttpURLConnection.HTTP_BAD_GATEWAY -> {
                    gyldig = false
                }
                else -> {
                    gyldig = false
                }
            }
            linkModel.postValue(gyldig)
        }
    }

}