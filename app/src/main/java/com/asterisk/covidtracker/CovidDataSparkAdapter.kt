package com.asterisk.covidtracker

import android.graphics.RectF
import com.asterisk.covidtracker.data.CovidData
import com.robinhood.spark.SparkAdapter

class CovidDataSparkAdapter(private val dailyData: List<CovidData>) : SparkAdapter() {

    var metric = Metric.POSITIVE
    var daysAgo = TimeScale.MAX

    override fun getCount(): Int = dailyData.size

    override fun getItem(index: Int) = dailyData[index]

    override fun getY(index: Int): Float {
        val choosenData = dailyData[index]
        return when (metric) {
            Metric.NEGATIVE -> choosenData.negativeIncrease.toFloat()
            Metric.POSITIVE -> choosenData.positiveIncrease.toFloat()
            Metric.DEATH -> choosenData.deathIncrease.toFloat()
        }
    }


    override fun getDataBounds(): RectF {
        val bounds = super.getDataBounds()
        if (daysAgo != TimeScale.MAX) {
            bounds.left = count - daysAgo.numDays.toFloat()
        }
        return bounds
    }

}
