package com.example.healthieru.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.healthieru.R
import com.example.healthieru.databinding.ActivityProfessionalBinding
import com.example.healthieru.model.AppData
import com.example.healthieru.model.HealthReportGenerator

class ProfessionalActivity : AppCompatActivity() {
    private lateinit var b: ActivityProfessionalBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityProfessionalBinding.inflate(layoutInflater)
        setContentView(b.root)

        val pro = AppData.professional
        b.tvWelcome.text = pro.name
        b.tvSpecialty.text = pro.specialty

        b.btnLogout.setOnClickListener {
            startActivity(
                Intent(this, LoginActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()
        b.patientContainer.removeAllViews()
        b.tvEmpty.visibility = if (AppData.patients.isEmpty()) View.VISIBLE else View.GONE

        AppData.patients.forEach { patient ->
            val item = layoutInflater.inflate(R.layout.item_patient, b.patientContainer, false)

            item.findViewById<TextView>(R.id.tvPatientName).text = patient.name
            item.findViewById<TextView>(R.id.tvPatientGoal).text = "Goal: ${patient.healthGoal}"

            val health =
                if (patient.hasRecords()) {
                    val r = HealthReportGenerator.generate(patient)
                    "BMI ${"%.1f".format(r.bmi)} (${r.bmiCategory}), ${r.heartRate} bpm"
                } else "No data recorded yet"
            item.findViewById<TextView>(R.id.tvPatientStatus).text =
                "$health\nStatus: ${patient.followUpStatus}"

            item.setOnClickListener {
                startActivity(
                    Intent(this, PatientDetailActivity::class.java)
                        .putExtra("email", patient.email)
                )
            }
            b.patientContainer.addView(item)
        }
    }
}