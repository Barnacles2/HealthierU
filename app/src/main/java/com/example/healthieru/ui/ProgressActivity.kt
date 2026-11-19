package com.example.healthieru.ui

import android.os.Bundle
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.healthieru.R
import com.example.healthieru.model.AppData
import com.example.healthieru.model.ProgressAnalyzer
import com.google.android.material.chip.ChipGroup

class ProgressActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)
        enableBack()

        val patient = AppData.currentUser ?: return finish()

        val chart = findViewById<ProgressChartView>(R.id.chart)
        val tvHeadline = findViewById<TextView>(R.id.tvHeadline)
        val tvSub = findViewById<TextView>(R.id.tvSub)
        val tvStart = findViewById<TextView>(R.id.tvStart)
        val tvLatest = findViewById<TextView>(R.id.tvLatest)
        val tvChange = findViewById<TextView>(R.id.tvChange)
        val tvInsight = findViewById<TextView>(R.id.tvInsight)
        val chipGroup = findViewById<ChipGroup>(R.id.chipGroupMetric)

        fun show(weight: Boolean) {
            val history = patient.getHistory()
            val labels = history.map { it.label }
            val values = if (weight) history.map { it.weightKg } else history.map { it.heartRate.toDouble() }
            val unit = if (weight) "kg" else "bpm"
            val decimals = if (weight) 1 else 0
            val color = ContextCompat.getColor(this, if (weight) R.color.primary else R.color.coral)

            chart.setData(labels, values, unit, color)

            val t = ProgressAnalyzer.trend(values)
            if (t == null) {
                tvHeadline.text = "Not enough data yet"
                tvSub.text = "Record your health data on at least two days."
                tvStart.text = "--"; tvLatest.text = "--"; tvChange.text = "--"
                tvInsight.text = "Your goal: ${patient.healthGoal}"
                return
            }

            val change = ProgressAnalyzer.describeChange(t.change, unit, decimals)
            tvHeadline.text = change
            tvSub.text = "Since ${labels.first()}, ${values.size} records"
            tvStart.text = "%.${decimals}f $unit".format(t.start)
            tvLatest.text = "%.${decimals}f $unit".format(t.latest)
            tvChange.text = change

            tvInsight.text = if (weight) {
                when {
                    t.change < -0.1 -> "Your weight is trending down. Compare this with your goal: ${patient.healthGoal}."
                    t.change > 0.1 -> "Your weight is trending up. Compare this with your goal: ${patient.healthGoal}."
                    else -> "Your weight is steady. Your goal: ${patient.healthGoal}."
                }
            } else {
                when {
                    t.change < -0.5 -> "Your resting heart rate is trending down, which often reflects improving fitness."
                    t.change > 0.5 -> "Your resting heart rate is trending up. If this continues, mention it to your health professional."
                    else -> "Your resting heart rate is steady."
                }
            }
        }

        show(true)
        chipGroup.setOnCheckedStateChangeListener { _, ids ->
            show(ids.firstOrNull() != R.id.chipHeart)
        }
    }
}