package com.example.healthieru.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.healthieru.databinding.ActivityDashboardBinding
import com.example.healthieru.model.AppData
import com.example.healthieru.model.HealthReportGenerator
import com.example.healthieru.data.local.AppDatabase
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {
    private lateinit var b: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Set up click listeners
        b.cardProfile.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }
        b.cardRecord.setOnClickListener { startActivity(Intent(this, RecordDataActivity::class.java)) }
        b.cardReport.setOnClickListener { startActivity(Intent(this, ReportActivity::class.java)) }
        b.cardAdvice.setOnClickListener { startActivity(Intent(this, RecommendationsActivity::class.java)) }
        b.cardHistory.setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }
        b.cardProgress.setOnClickListener { startActivity(Intent(this, ProgressActivity::class.java)) }

        b.btnLogout.setOnClickListener {
            AppData.currentUser = null
            startActivity(
                Intent(this, LoginActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                )
            )
        }

        // 1. Initialize the Database and DAO once in onCreate
        val db = AppDatabase.getDatabase(this)
        val userDao = db.userDao()

        // 2. Observe the database continuously. This handles updates automatically.
        lifecycleScope.launch {
            userDao.getUser().collect { savedUser ->
                val patient = AppData.currentUser ?: return@collect

                // Update greeting and goal
                b.tvGreeting.text = "Hello, ${patient.name.substringBefore(' ')}"
                b.tvGoal.text = "Goal: ${patient.healthGoal}"

                if (savedUser != null) {
                    // Update the UI with the data FROM THE DATABASE
                    b.tvWeight.text = "%.1f kg".format(savedUser.weight)
                    b.tvHeart.text = "${savedUser.heartRate} bpm"
                    b.tvBmi.text = "%.1f".format(savedUser.bmi)

                    // 3. Sync AppData so other screens that still use it (like the Report screen) get the latest values
                    patient.weightKg = savedUser.weight
                    patient.heightCm = savedUser.height
                    patient.heartRateBpm = savedUser.heartRate

                } else {
                    // No data in database yet (first launch / empty DB)
                    if (patient.hasRecords()) {
                        val report = HealthReportGenerator.generate(patient)
                        b.tvWeight.text = "%.1f kg".format(patient.getWeight())
                        b.tvHeart.text = "${patient.getHeartRate()} bpm"
                        b.tvBmi.text = "%.1f".format(report.bmi)
                    } else {
                        b.tvWeight.text = "--"
                        b.tvHeart.text = "--"
                        b.tvBmi.text = "--"
                    }
                }
            }
        }
    }
}