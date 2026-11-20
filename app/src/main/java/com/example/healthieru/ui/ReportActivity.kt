package com.example.healthieru.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.example.healthieru.databinding.ActivityReportBinding
import com.example.healthieru.model.AppData
import com.example.healthieru.model.HealthReportGenerator

class ReportActivity : BaseActivity() {
    private lateinit var b: ActivityReportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityReportBinding.inflate(layoutInflater)
        setContentView(b.root)
        enableBack()

        val patient = AppData.currentUser ?: return finish()

        if (!patient.hasRecords()) {
            b.reportContent.visibility = View.GONE
            b.tvEmpty.visibility = View.VISIBLE
            return
        }

        val r = HealthReportGenerator.generate(patient)
        b.tvPatient.text = r.patientName
        b.tvGoal.text = "Goal: ${r.goal}"
        b.tvBmiValue.text = "%.1f".format(r.bmi)
        b.tvBmiCategory.text = r.bmiCategory
        b.tvHeartValue.text = "${r.heartRate} bpm"
        b.tvHeartStatus.text = r.heartStatus
        b.tvActivity.text = r.activityLevel

        b.btnRecommendations.setOnClickListener {
            startActivity(Intent(this, RecommendationsActivity::class.java))
        }
    }
}