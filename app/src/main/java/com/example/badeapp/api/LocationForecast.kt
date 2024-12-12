package com.example.badeapp.api

import retrofit2.http.GET
import retrofit2.http.Query

interface LocationForecast {

    //Eksempel-URL: "https://in2000-apiproxy.ifi.uio.no/weatherapi/locationforecast/2.0/compact?lat=60.10&lon=9.58"
    @GET("/weatherapi/locationforecast/2.0/compact")
    suspend fun fetchForecast(@Query("lat") lat: Double, @Query("lon") lon: Double): LocationForecastDto
}

data class LocationForecastDto(val properties: LocationProperties)

data class LocationProperties(val timeseries: List<LocationTimeseries>)

data class LocationTimeseries(val time: String, val data: LocationData)

data class LocationData(val instant: LocationInstant)

data class LocationInstant(val details: LocationDetails)

data class LocationDetails(val air_temperature: String)