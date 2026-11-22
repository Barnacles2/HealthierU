package com.example.healthieru.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.healthieru.R
import com.example.healthieru.data.local.HealthRecord
import com.example.healthieru.data.local.User
import com.example.healthieru.model.ProgressAnalyzer
import com.example.healthieru.model.formatDate
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.xml.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Size
import java.util.concurrent.TimeUnit

class ProgressActivity : BaseActivity() {

    private var user: User? = null
    private var records: List<HealthRecord> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)
        enableBack()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.currentUser.collect { user = it; render() } }
                launch { viewModel.records.collect { records = it; render() } }
            }
        }
    }

    private fun render() {
        val u = user ?: return

        val tvTrend = findViewById<TextView>(R.id.tvTrend)
        val tvSummary = findViewById<TextView>(R.id.tvSummary)
        val celebrationBanner = findViewById<View>(R.id.celebrationBanner)
        val konfettiView = findViewById<KonfettiView>(R.id.konfettiView)

        if (records.isEmpty()) {
            tvTrend.text = getString(R.string.not_enough_records)
            return
        }

        val weights = records.map { it.weightKg }
        val startWeight = records.first().weightKg
        val latestWeight = records.last().weightKg
        val change = latestWeight - startWeight

        // 1. Draw the Chart
        findViewById<ProgressChartView>(R.id.chartWeight).setData(
            records.map { formatDate(it.date) }, weights, "kg",
            ContextCompat.getColor(this, R.color.primary)
        )

        // 2. Update the Trend Text
        val trendText = ProgressAnalyzer.describeChange(change, "kg")
        tvTrend.text = getString(R.string.trend_summary_format, trendText, formatDate(records.first().date), records.size)

        // 3. Update the Summary Text
        val direction = if (change < 0) "down" else "up"
        tvSummary.text = getString(R.string.summary_text_format, direction, u.healthGoal)

        // 4. Check if Goal is Met & Celebrate
        val goalStr = u.healthGoal.lowercase()
        val goalAmount = goalStr.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0

        val isGoalMet = when {
            goalStr.contains("lose") && goalAmount > 0 -> change <= -goalAmount
            goalStr.contains("gain") && goalAmount > 0 -> change >= goalAmount
            else -> false
        }

        if (isGoalMet) {
            celebrationBanner.visibility = View.VISIBLE

            // FINAL CONFETTI CELEBRATION (All Floats)
            val party = Party(
                speed = 0f,
                maxSpeed = 50f,
                damping = 0.9f,
                spread = 360,
                colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def, 0x00bcd4),
                position = Position.Relative(0.5, 0.3),
                emitter = Emitter(duration = 4000L, TimeUnit.MILLISECONDS).max(300),
                size = listOf(Size(16, 32f, 1f)) // ALL FLOATS
            )
            konfettiView.start(party)
        } else {
            celebrationBanner.visibility = View.GONE
        }
    }
}