package com.example.healthieru.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.healthieru.databinding.ActivityDashboardBinding
import com.example.healthieru.model.AppData
import com.example.healthieru.model.HealthReportGenerator

class DashboardActivity : AppCompatActivity() {
    private lateinit var b: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.cardRecord.setOnClickListener { startActivity(Intent(this, RecordDataActivity::class.java)) }
        b.cardReport.setOnClickListener { startActivity(Intent(this, ReportActivity::class.java)) }
        b.cardAdvice.setOnClickListener { startActivity(Intent(this, RecommendationsActivity::class.java)) }
        b.cardProgress.setOnClickListener { startActivity(Intent(this, ProgressActivity::class.java)) }
        b.btnLogout.setOnClickListener {
            AppData.currentUser = null
            startActivity(
                Intent(this, LoginActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()
        val patient = AppData.currentUser ?: return
        b.tvGreeting.text = "Hello, ${patient.name.substringBefore(' ')}"
        b.tvGoal.text = "Goal: ${patient.healthGoal}"

        if (patient.hasRecords()) {
            val report = HealthReportGenerator.generate(patient)
            b.tvWeight.text = "%.1f kg".format(patient.getWeight())
            b.tvHeart.text = "${patient.getHeartRate()} bpm"
            b.tvBmi.text = "%.1f".format(report.bmi)
        } else {
            b.tvWeight.text = "--"; b.tvHeart.text = "--"; b.tvBmi.text = "--"
        }
    }
}