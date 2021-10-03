package com.asterisk.covidtracker.data

import retrofit2.Call
import retrofit2.http.GET

interface CovidService {

    @GET("us/daily.json")
    suspend fun getNationalData(): List<CovidData>

    @GET("states/daily.json")
    suspend fun getStatesData(): List<CovidData>
}