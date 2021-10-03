package com.asterisk.covidtracker

import android.os.Bundle
import android.util.Log
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.asterisk.covidtracker.Constants.ALL_STATES
import com.asterisk.covidtracker.Constants.BASE_URL
import com.asterisk.covidtracker.data.CovidData
import com.asterisk.covidtracker.data.CovidService
import com.asterisk.covidtracker.databinding.ActivityMainBinding
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var currentlyShownData: List<CovidData>
    private lateinit var adapter: CovidDataSparkAdapter
    private lateinit var perStateDailyData: Map<String, List<CovidData>>
    private lateinit var binding: ActivityMainBinding
    private lateinit var nationalDailyData: List<CovidData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        val covidService = retrofit.create(CovidService::class.java)

        // Fetch National data
        CoroutineScope(Dispatchers.Main).launch {
            try {
                setUpEventListeners()
                nationalDailyData = covidService.getNationalData().reversed()
                updateDisplayWithData(nationalDailyData)
            } catch (e: Exception) {
                Log.d(TAG, "${e.cause} from error national data")
                e.printStackTrace()
            }

        }


        //Fetch State Data
        CoroutineScope(Dispatchers.Main).launch {
            try {
                perStateDailyData = covidService.getStatesData().reversed().groupBy { it.state }
                // Update spinner with state names
                updateSpinnerWithStateData(perStateDailyData.keys)
            } catch (e: Exception) {
                Log.d(TAG, "${e.message} from error per state data")
                e.printStackTrace()
            }

        }


    }

    private fun updateSpinnerWithStateData(stateNames: Set<String>) {
        val stateAbbreviationList = stateNames.toMutableList()
        stateAbbreviationList.sort()
        stateAbbreviationList.add(0, ALL_STATES)

        binding.spinnerSelect.apply {
            attachDataSource(stateAbbreviationList)
            setOnSpinnerItemSelectedListener { parent, _, position, _ ->
                val selectedState = parent.getItemAtPosition(position) as String
                val selectedData = perStateDailyData[selectedState] ?: nationalDailyData
                updateDisplayWithData(selectedData)
            }
        }
    }

    private fun setUpEventListeners() {
        // Add a listener for the user scrubbing on the chart
        binding.sparkView.isScrubEnabled = true
        binding.sparkView.setScrubListener { itemData ->
            if (itemData is CovidData) {
                updateInfoForDate(itemData)
            }
        }
        //Response to radio button event selection
        binding.radioGroupTimeSelection.setOnCheckedChangeListener { _, checkedId ->
            adapter.daysAgo = when (checkedId) {
                R.id.radioButtonWeek -> TimeScale.WEEK
                R.id.radioButtonMonth -> TimeScale.MONTH
                else -> TimeScale.MAX
            }
            adapter.notifyDataSetChanged()
        }

        binding.radioGroupMetricSelection.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioButtonPositive -> updateDisplayMetric(Metric.POSITIVE)
                R.id.radioButtonNegative -> updateDisplayMetric(Metric.NEGATIVE)
                R.id.radioButtonDeath -> updateDisplayMetric(Metric.DEATH)
            }
        }
    }

    private fun updateDisplayMetric(metric: Metric) {
        // Update the color of the chart
        val colorRes = when (metric) {
            Metric.NEGATIVE -> R.color.negative_case
            Metric.POSITIVE -> R.color.positive_case
            Metric.DEATH -> R.color.death_case
        }

        @ColorInt val colorInt = ContextCompat.getColor(this, colorRes)
        binding.sparkView.lineColor = colorInt
        binding.tvMetricLabel.setTextColor(colorInt)


        // Update the metric on the adapter
        adapter.metric = metric
        adapter.notifyDataSetChanged()

        // Reset the number an data shown in the bottom textView
        updateInfoForDate(currentlyShownData.last())
    }

    private fun updateDisplayWithData(dailyData: List<CovidData>) {
        currentlyShownData = dailyData

        // Create a new SparkAdapter with the data
        adapter = CovidDataSparkAdapter(dailyData)
        binding.sparkView.adapter = adapter

        // Update radio buttons to select the positive cases and max time by default
        binding.apply {
            radioButtonPositive.isChecked = true
            radioButtonMax.isChecked = true
        }
        // Display metric for the most recent date
        updateDisplayMetric(Metric.POSITIVE)
    }

    private fun updateInfoForDate(covidData: CovidData) {
        val numCases = when (adapter.metric) {
            Metric.NEGATIVE -> covidData.negativeIncrease
            Metric.POSITIVE -> covidData.positiveIncrease
            Metric.DEATH -> covidData.deathIncrease
        }
        binding.tvMetricLabel.text = NumberFormat.getInstance().format(numCases)
        val outputDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        binding.tvDateLabel.text = outputDateFormat.format(covidData.dateChecked)
    }
}

private const val TAG = "MainActivityy"