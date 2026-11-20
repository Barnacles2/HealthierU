package com.example.healthieru.ui

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.healthieru.R
import com.example.healthieru.data.local.AppDatabase
import com.example.healthieru.data.local.HealthRecord
import com.example.healthieru.model.AppData
import com.example.healthieru.model.ProgressAnalyzer
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class ProgressActivity : BaseActivity() {

    private lateinit var db: AppDatabase
    private lateinit var tvHeadline: TextView
    private lateinit var tvSub: TextView
    private lateinit var tvStart: TextView
    private lateinit var tvLatest: TextView
    private lateinit var tvChange: TextView
    private lateinit var tvInsight: TextView
    private lateinit var chipGroup: ChipGroup
    private lateinit var chart: ProgressChartView

    private var currentRecords: List<HealthRecord> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)
        enableBack()

        val patient = AppData.currentUser ?: return finish()

        chart = findViewById(R.id.chart)
        tvHeadline = findViewById(R.id.tvHeadline)
        tvSub = findViewById(R.id.tvSub)
        tvStart = findViewById(R.id.tvStart)
        tvLatest = findViewById(R.id.tvLatest)
        tvChange = findViewById(R.id.tvChange)
        tvInsight = findViewById(R.id.tvInsight)
        chipGroup = findViewById(R.id.chipGroupMetric)

        db = AppDatabase.getDatabase(this)
        val healthDao = db.healthDao()

        lifecycleScope.launch {
            healthDao.getAllRecords().collect { records ->
                currentRecords = records.reversed()
                val isWeight = chipGroup.checkedChipId != R.id.chipHeart
                updateUI(isWeight, patient.healthGoal)
            }
        }

        chipGroup.setOnCheckedStateChangeListener { _, ids ->
            val isWeight = ids.firstOrNull() != R.id.chipHeart
            updateUI(isWeight, patient.healthGoal)
        }
    }

    private fun updateUI(isWeight: Boolean, goalString: String) {
        val labels = currentRecords.map { it.date }
        val values = if (isWeight) currentRecords.map { it.weightKg } else currentRecords.map { it.heartRate.toDouble() }

        val unit = if (isWeight) "kg" else "bpm"
        val decimals = if (isWeight) 1 else 0
        val defaultColor = ContextCompat.getColor(this, if (isWeight) R.color.primary else R.color.coral)

        chart.setData(labels, values, unit, defaultColor)

        val t = ProgressAnalyzer.trend(values)
        if (t == null) {
            tvHeadline.text = "Not enough data yet"
            tvSub.text = "Record your health data on at least two days."
            tvStart.text = "--"; tvLatest.text = "--"; tvChange.text = "--"
            tvInsight.text = "Your goal: $goalString"
            return
        }

        val change = ProgressAnalyzer.describeChange(t.change, unit, decimals)
        tvHeadline.text = change
        tvSub.text = "Since ${labels.first()}, ${values.size} records"
        tvStart.text = "%.${decimals}f $unit".format(t.start)
        tvLatest.text = "%.${decimals}f $unit".format(t.latest)
        tvChange.text = change

        // --- NEW GOAL LOGIC ---
        if (isWeight) {
            // Extract the target weight from the goal string (e.g., "Lose 5 kg" -> target is Start - 5)
            val goalAmount = Regex("\\d+(\\.\\d+)?").find(goalString)?.value?.toDoubleOrNull() ?: 0.0
            val isLosingWeight = goalString.contains("Lose", ignoreCase = true)

            // Example: Start 76.0, Goal "Lose 5kg" -> Target = 71.0
            val targetWeight = if (isLosingWeight) t.start - goalAmount else t.start + goalAmount

            // How far is the current weight from the target?
            val distanceToGoal = t.latest - targetWeight

            // Are they moving in the right direction?
            val isMovingTowardGoal = if (isLosingWeight) t.change < 0 else t.change > 0

            // 1. Change the colors to Red (error) if moving away, Green (primary) if moving toward
            val trendColor = if (isMovingTowardGoal) ContextCompat.getColor(this, R.color.primary) else Color.RED

            // Update Chart line color to reflect success/failure
            chart.setData(labels, values, unit, trendColor)

            // 2. Set the Insight Text dynamically
            tvInsight.text = when {
                Math.abs(distanceToGoal) < 0.5 -> "🎉 Congratulations! You have reached your goal of $targetWeight kg!"
                isMovingTowardGoal -> "You are on track! You have %.1f kg left to reach your goal of $targetWeight kg.".format(Math.abs(distanceToGoal))
                else -> "Warning: You are moving away from your goal. You need to %s %.1f kg to reach $targetWeight kg.".format(
                    if (isLosingWeight) "lose" else "gain",
                    Math.abs(distanceToGoal)
                )
            }

            // 3. Change headline color if they are failing
            tvHeadline.setTextColor(trendColor)

        } else {
            // Heart Rate logic remains the same
            tvInsight.text = when {
                t.change < -0.5 -> "Your resting heart rate is trending down, which often reflects improving fitness."
                t.change > 0.5 -> "Your resting heart rate is trending up. If this continues, mention it to your health professional."
                else -> "Your resting heart rate is steady."
            }
            tvHeadline.setTextColor(defaultColor)
        }
    }
}