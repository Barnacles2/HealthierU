package com.example.healthieru.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.healthieru.R
import com.example.healthieru.model.HealthReportGenerator
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ReportActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)
        enableBack()

        val content = findViewById<View>(R.id.reportContent)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)

        findViewById<Button>(R.id.btnRecommendations).setOnClickListener {
            startActivity(Intent(this, RecommendationsActivity::class.java))
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(viewModel.currentUser, viewModel.records) { user, records -> user to records.lastOrNull() }
                    .collect { (user, latest) ->
                        if (user == null || latest == null) {
                            content.visibility = View.GONE
                            tvEmpty.visibility = View.VISIBLE
                        } else {
                            content.visibility = View.VISIBLE
                            tvEmpty.visibility = View.GONE
                            val r = HealthReportGenerator.generate(user, latest)
                            findViewById<TextView>(R.id.tvPatient).text = r.patientName
                            findViewById<TextView>(R.id.tvGoal).text = "Goal: ${r.goal}"
                            findViewById<TextView>(R.id.tvBmiValue).text = "%.1f".format(r.bmi)
                            findViewById<TextView>(R.id.tvBmiCategory).text = r.bmiCategory
                            findViewById<TextView>(R.id.tvHeartValue).text = "${r.heartRate} bpm"
                            findViewById<TextView>(R.id.tvHeartStatus).text = r.heartStatus
                            findViewById<TextView>(R.id.tvActivity).text = r.activityLevel
                        }
                    }
            }
        }
    }
}