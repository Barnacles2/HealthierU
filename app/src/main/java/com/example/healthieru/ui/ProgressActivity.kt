package com.example.healthieru.ui

import android.os.Bundle
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.healthieru.R
import com.example.healthieru.data.local.HealthRecord
import com.example.healthieru.model.ProgressAnalyzer
import com.example.healthieru.model.formatDate
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ProgressActivity : BaseActivity() {
    private var goal = ""
    private var records: List<HealthRecord> = emptyList()
    private var showWeight = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)
        enableBack()

        findViewById<ChipGroup>(R.id.chipGroupMetric).setOnCheckedStateChangeListener { _, ids ->
            showWeight = ids.firstOrNull() != R.id.chipHeart
            render()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(viewModel.currentUser, viewModel.records) { u, r -> (u?.healthGoal ?: "") to r }
                    .collect { (g, r) -> goal = g; records = r; render() }
            }
        }
    }

    private fun render() {
        val chart = findViewById<ProgressChartView>(R.id.chart)
        val tvHeadline = findViewById<TextView>(R.id.tvHeadline)
        val tvSub = findViewById<TextView>(R.id.tvSub)
        val tvStart = findViewById<TextView>(R.id.tvStart)
        val tvLatest = findViewById<TextView>(R.id.tvLatest)
        val tvChange = findViewById<TextView>(R.id.tvChange)
        val tvInsight = findViewById<TextView>(R.id.tvInsight)

        val labels = records.map { formatDate(it.date) }
        val values = if (showWeight) records.map { it.weightKg } else records.map { it.heartRate.toDouble() }
        val unit = if (showWeight) "kg" else "bpm"
        val decimals = if (showWeight) 1 else 0
        val color = ContextCompat.getColor(this, if (showWeight) R.color.primary else R.color.coral)

        chart.setData(labels, values, unit, color)

        val t = ProgressAnalyzer.trend(values)
        if (t == null) {
            tvHeadline.text = "Not enough data yet"
            tvSub.text = "Record your health data on at least two days."
            tvStart.text = "--"; tvLatest.text = "--"; tvChange.text = "--"
            tvInsight.text = "Your goal: $goal"
            return
        }

        val change = ProgressAnalyzer.describeChange(t.change, unit, decimals)
        tvHeadline.text = change
        tvSub.text = "Since ${labels.first()}, ${values.size} records"
        tvStart.text = "%.${decimals}f $unit".format(t.start)
        tvLatest.text = "%.${decimals}f $unit".format(t.latest)
        tvChange.text = change

        tvInsight.text = if (showWeight) {
            when {
                t.change < -0.1 -> "Your weight is trending down. Compare this with your goal: $goal."
                t.change > 0.1 -> "Your weight is trending up. Compare this with your goal: $goal."
                else -> "Your weight is steady. Your goal: $goal."
            }
        } else {
            when {
                t.change < -0.5 -> "Your resting heart rate is trending down, which often reflects improving fitness."
                t.change > 0.5 -> "Your resting heart rate is trending up. If this continues, mention it to your health professional."
                else -> "Your resting heart rate is steady."
            }
        }
    }
}