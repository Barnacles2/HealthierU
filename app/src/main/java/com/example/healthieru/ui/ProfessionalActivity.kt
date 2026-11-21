package com.example.healthieru.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.healthieru.R
import com.example.healthieru.data.local.HealthRecord
import com.example.healthieru.data.local.User
import com.example.healthieru.model.Doctor // <-- THIS IS THE CRUCIAL IMPORT
import com.example.healthieru.model.HealthReportGenerator
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ProfessionalActivity : BaseActivity() {

    private lateinit var container: LinearLayout
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_professional)

        val tvDoctorName = findViewById<TextView>(R.id.tvDoctorName)
        container = findViewById(R.id.patientContainer)
        tvEmpty = findViewById(R.id.tvEmpty)

        tvDoctorName.text = "${Doctor.account.name} • ${Doctor.account.specialty}"

        findViewById<Button>(R.id.btnLogout).setOnClickListener { goToLogin() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(viewModel.allPatients, viewModel.latestRecords) { p, r -> p to r }
                    .collect { (patients, latest) -> render(patients, latest) }
            }
        }
    }

    private fun render(patients: List<User>, latest: List<HealthRecord>) {
        container.removeAllViews()
        tvEmpty.visibility = if (patients.isEmpty()) View.VISIBLE else View.GONE

        patients.forEach { patient ->
            val record = latest.firstOrNull { it.userId == patient.id }
            val item = layoutInflater.inflate(R.layout.item_patient, container, false)

            item.findViewById<TextView>(R.id.tvPatientName).text = patient.name
            item.findViewById<TextView>(R.id.tvPatientGoal).text = "Goal: ${patient.healthGoal}"

            val healthMetrics = if (record != null) {
                val bmi = HealthReportGenerator.bmi(record.weightKg, record.heightCm)
                "Last log: ${record.weightKg} kg, ${record.heartRate} bpm (BMI: ${"%.1f".format(bmi)})"
            } else {
                "Last log: No data recorded yet"
            }
            item.findViewById<TextView>(R.id.tvPatientMetrics).text = healthMetrics

            val statusBadge = item.findViewById<TextView>(R.id.tvPatientStatus)
            if (patient.followUpStatus == "Urgent review" || patient.followUpStatus == "Needs follow-up") {
                statusBadge.text = patient.followUpStatus
                statusBadge.setTextColor(Color.parseColor("#EF233C"))
                statusBadge.setBackgroundColor(Color.parseColor("#FFEBEE"))
            } else {
                statusBadge.text = "Stable"
                statusBadge.setTextColor(Color.parseColor("#2A9D8F"))
                statusBadge.setBackgroundColor(Color.parseColor("#E8F5E9"))
            }

            item.findViewById<Button>(R.id.btnViewPatient).setOnClickListener {
                startActivity(Intent(this, PatientDetailActivity::class.java).putExtra("userId", patient.id))
            }

            item.setOnClickListener {
                startActivity(Intent(this, PatientDetailActivity::class.java).putExtra("userId", patient.id))
            }

            container.addView(item)
        }
    }
}