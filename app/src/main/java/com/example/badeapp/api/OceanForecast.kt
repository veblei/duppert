package com.example.badeapp.api

import retrofit2.http.GET
import retrofit2.http.Query

interface OceanForecast {

    //Eksempel-URL: "https://in2000-apiproxy.ifi.uio.no/weatherapi/oceanforecast/2.0/complete?lat=60.10&lon=5"
    @GET("/weatherapi/oceanforecast/2.0/complete")
    suspend fun fetchForecast(@Query("lat") lat: Double, @Query("lon") lon: Double): OceanForecastDto
}

data class OceanForecastDto(val properties: OceanProperties)

data class OceanProperties(val timeseries: List<OceanTimeseries>)

data class OceanTimeseries(val time: String, val data: OceanData)

data class OceanData(val instant: OceanInstant)

data class OceanInstant(val details: OceanDetails)

data class OceanDetails(val sea_water_temperature: String)