
package com.example.healthieru.ui

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.example.healthieru.HealthierUApp
import com.example.healthieru.R
import com.example.healthieru.data.local.HealthRecord
import com.example.healthieru.model.AppData
import com.example.healthieru.model.ProgressAnalyzer
import com.example.healthieru.viewmodel.HealthViewModel
import com.example.healthieru.viewmodel.HealthViewModelFactory
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class ProgressActivity : BaseActivity() {

    private lateinit var tvHeadline: TextView
    private lateinit var tvSub: TextView
    private lateinit var tvStart: TextView
    private lateinit var tvLatest: TextView
    private lateinit var tvChange: TextView
    private lateinit var tvInsight: TextView
    private lateinit var chipGroup: ChipGroup
    private lateinit var chart: ProgressChartView
    private lateinit var healthViewModel: HealthViewModel

    private var currentRecords: List<HealthRecord> = emptyList()
    private var currentGoal: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)
        enableBack()

        val patient = AppData.currentUser ?: return finish()
        currentGoal = patient.healthGoal

        chart = findViewById(R.id.chart)
        tvHeadline = findViewById(R.id.tvHeadline)
        tvSub = findViewById(R.id.tvSub)
        tvStart = findViewById(R.id.tvStart)
        tvLatest = findViewById(R.id.tvLatest)
        tvChange = findViewById(R.id.tvChange)
        tvInsight = findViewById(R.id.tvInsight)
        chipGroup = findViewById(R.id.chipGroupMetric)

        // Initialise ViewModel
        val repository = (application as HealthierUApp).repository
        val factory = HealthViewModelFactory(repository)

        healthViewModel = ViewModelProvider(this, factory)
            .get(HealthViewModel::class.java)

        // Observe records through ViewModel
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                healthViewModel.allRecords.collect { records ->
                    currentRecords = records.reversed()

                    val isWeight =
                        chipGroup.checkedChipId != R.id.chipHeart

                    updateUI(isWeight, currentGoal)
                }
            }
        }

        // Listen for metric selection
        chipGroup.setOnCheckedStateChangeListener { _, ids ->
            val isWeight =
                ids.firstOrNull() != R.id.chipHeart

            updateUI(isWeight, currentGoal)
        }
    }

    private fun updateUI(isWeight: Boolean, goalString: String) {

        val labels = currentRecords.map { it.date }

        val values = if (isWeight) {
            currentRecords.map { it.weightKg }
        } else {
            currentRecords.map { it.heartRate.toDouble() }
        }

        val unit = if (isWeight) "kg" else "bpm"
        val decimals = if (isWeight) 1 else 0

        val defaultColor = ContextCompat.getColor(
            this,
            if (isWeight) R.color.primary else R.color.coral
        )

        chart.setData(labels, values, unit, defaultColor)

        val t = ProgressAnalyzer.trend(values)

        if (t == null) {
            tvHeadline.text = "Not enough data yet"
            tvSub.text = "Record your health data on at least two days."
            tvStart.text = "--"
            tvLatest.text = "--"
            tvChange.text = "--"
            tvInsight.text = "Your goal: $goalString"
            return
        }

        val change = ProgressAnalyzer.describeChange(
            t.change,
            unit,
            decimals
        )

        tvHeadline.text = change
        tvSub.text = "Since ${labels.first()}, ${values.size} records"
        tvStart.text = "%.${decimals}f $unit".format(t.start)
        tvLatest.text = "%.${decimals}f $unit".format(t.latest)
        tvChange.text = change

        // WEIGHT GOAL LOGIC
        if (isWeight) {

            val goalAmount = Regex("\\d+(\\.\\d+)?")
                .find(goalString)
                ?.value
                ?.toDoubleOrNull() ?: 0.0

            val isLosingWeight =
                goalString.contains("Lose", ignoreCase = true)

            val targetWeight = if (isLosingWeight) {
                t.start - goalAmount
            } else {
                t.start + goalAmount
            }

            val distanceToGoal = t.latest - targetWeight

            val isMovingTowardGoal = if (isLosingWeight) {
                t.change < 0
            } else {
                t.change > 0
            }

            val trendColor = if (isMovingTowardGoal) {
                ContextCompat.getColor(this, R.color.primary)
            } else {
                Color.RED
            }

            chart.setData(labels, values, unit, trendColor)

            tvInsight.text = when {
                kotlin.math.abs(distanceToGoal) < 0.5 ->
                    "You have reached your recorded goal of $targetWeight kg!"

                isMovingTowardGoal ->
                    "Your recorded measurements are moving toward your goal. " +
                            "%.1f kg difference from the target.".format(
                                kotlin.math.abs(distanceToGoal)
                            )

                else ->
                    "Your recent measurements are moving away from the recorded target. " +
                            "%.1f kg difference from the target.".format(
                                kotlin.math.abs(distanceToGoal)
                            )
            }

            tvHeadline.setTextColor(trendColor)

        } else {

            // HEART RATE INSIGHTS
            tvInsight.text = when {
                t.change < -0.5 ->
                    "Your resting heart rate is trending down, which often reflects improving fitness."

                t.change > 0.5 ->
                    "Your resting heart rate is trending up. If this continues, mention it to your health professional."

                else ->
                    "Your resting heart rate is steady."
            }

            tvHeadline.setTextColor(defaultColor)
        }
    }
}
