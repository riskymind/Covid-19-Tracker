package com.asterisk.covidtracker

import com.asterisk.covidtracker.data.CovidData
import com.robinhood.spark.SparkAdapter

class CovidDataSparkAdapter(private val dailyData: List<CovidData>) : SparkAdapter() {
    override fun getCount(): Int = dailyData.size

    override fun getItem(index: Int) = dailyData[index]

    override fun getY(index: Int): Float {
        val choosenData = dailyData[index]
        return choosenData.positiveIncrease.toFloat()
    }

}
