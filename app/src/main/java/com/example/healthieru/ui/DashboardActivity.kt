
package com.example.healthieru.ui

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.example.healthieru.HealthierUApp
import com.example.healthieru.databinding.ActivityDashboardBinding
import com.example.healthieru.model.AppData
import com.example.healthieru.model.HealthReportGenerator
import com.example.healthieru.viewmodel.HealthViewModel
import com.example.healthieru.viewmodel.HealthViewModelFactory
import kotlinx.coroutines.launch
import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.healthieru.work.ReminderWorker
import java.util.concurrent.TimeUnit
import androidx.work.OneTimeWorkRequestBuilder

class DashboardActivity : BaseActivity() {

    private lateinit var b: ActivityDashboardBinding
    private lateinit var healthViewModel: HealthViewModel
    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Initialise ViewModel
        val repository = (application as HealthierUApp).repository
        val factory = HealthViewModelFactory(repository)

        healthViewModel = ViewModelProvider(this, factory)
            .get(HealthViewModel::class.java)
        healthViewModel.loadTip()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                healthViewModel.tip.collect { tip ->
                    b.tvTip.text = tip ?: "Loading tip..."
                }
            }
        }
        // Ask for notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= 33) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Schedule the daily reminder
        val reminder = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_health_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            reminder
        )

        // NAVIGATION
        b.cardProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        b.cardRecord.setOnClickListener {
            startActivity(Intent(this, RecordDataActivity::class.java))
        }

        b.cardReport.setOnClickListener {
            startActivity(Intent(this, ReportActivity::class.java))
        }

        b.cardAdvice.setOnClickListener {
            startActivity(Intent(this, RecommendationsActivity::class.java))
        }

        b.cardHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        b.cardProgress.setOnClickListener {
            startActivity(Intent(this, ProgressActivity::class.java))
        }

        // LOGOUT
        b.btnLogout.setOnClickListener {
            AppData.currentUser = null

            startActivity(
                Intent(this, LoginActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                )
            )
        }

        // OBSERVE USER PROFILE THROUGH VIEWMODEL
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                healthViewModel.user.collect { savedUser ->

                    val patient = AppData.currentUser
                        ?: return@collect

                    // Update greeting and goal
                    b.tvGreeting.text =
                        "Hello, ${patient.name.substringBefore(' ')}"

                    b.tvGoal.text = "Goal: ${patient.healthGoal}"

                    if (savedUser != null) {

                        // Display saved database information
                        b.tvWeight.text =
                            "%.1f kg".format(savedUser.weight)

                        b.tvHeart.text =
                            "${savedUser.heartRate} bpm"

                        b.tvBmi.text =
                            "%.1f".format(savedUser.bmi)

                        // Synchronise AppData for other existing screens
                        patient.weightKg = savedUser.weight
                        patient.heightCm = savedUser.height
                        patient.heartRateBpm = savedUser.heartRate

                    } else {

                        // Fallback when no database profile exists
                        if (patient.hasRecords()) {

                            val report =
                                HealthReportGenerator.generate(patient)

                            b.tvWeight.text =
                                "%.1f kg".format(patient.getWeight())

                            b.tvHeart.text =
                                "${patient.getHeartRate()} bpm"

                            b.tvBmi.text =
                                "%.1f".format(report.bmi)

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
}
