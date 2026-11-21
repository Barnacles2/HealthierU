package com.example.healthieru.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.healthieru.HealthierUApp
import com.example.healthieru.R // <-- ADDED THIS
import com.example.healthieru.data.SessionManager // <-- ADDED THIS
import com.example.healthieru.databinding.ActivityDashboardBinding
import com.example.healthieru.model.HealthReportGenerator
import com.example.healthieru.viewmodel.HealthViewModel
import com.example.healthieru.viewmodel.HealthViewModelFactory
import com.example.healthieru.work.ReminderWorker
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class DashboardActivity : BaseActivity() {

    private lateinit var b: ActivityDashboardBinding
    private lateinit var healthViewModel: HealthViewModel

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(b.root)

        // 1. Initialize ViewModel (Fixed to pass SessionManager)
        val repository = (application as HealthierUApp).repository
        val session = SessionManager(this) // <-- ADDED THIS
        val factory = HealthViewModelFactory(repository, session) // <-- UPDATED THIS
        healthViewModel = ViewModelProvider(this, factory).get(HealthViewModel::class.java)

        // 2. Set up the Expandable Menu Logic
        val expandedMenu = findViewById<LinearLayout>(R.id.expandedMenuContainer)
        val btnToggleMenu = findViewById<MaterialButton>(R.id.btnToggleMenu)

        btnToggleMenu.setOnClickListener {
            if (expandedMenu.visibility == View.GONE) {
                expandedMenu.visibility = View.VISIBLE
                btnToggleMenu.text = "Show Less"
            } else {
                expandedMenu.visibility = View.GONE
                btnToggleMenu.text = "View Progress & History"
            }
        }

        // 3. Set up Navigation Cards
        b.cardProfile.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }
        b.cardRecord.setOnClickListener { startActivity(Intent(this, RecordDataActivity::class.java)) }
        b.cardReport.setOnClickListener { startActivity(Intent(this, ReportActivity::class.java)) }
        b.cardAdvice.setOnClickListener { startActivity(Intent(this, RecommendationsActivity::class.java)) }
        b.cardHistory.setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }
        b.cardProgress.setOnClickListener { startActivity(Intent(this, ProgressActivity::class.java)) }

        // 4. Logout Button
        b.btnLogout.setOnClickListener {
            healthViewModel.logout()
            goToLogin()
        }

        // 5. Ask for notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= 33) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // 6. Schedule Daily Reminder using WorkManager
        val reminder = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_health_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            reminder
        )

        // 7. Observe Data from Room Database
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    healthViewModel.currentUser.collect { user ->
                        if (user != null) {
                            b.tvGreeting.text = "Hello, ${user.name.substringBefore(' ')}"
                            b.tvGoal.text = "Goal: ${user.healthGoal}"
                        }
                    }
                }

                launch {
                    healthViewModel.records.collect { list ->
                        val latest = list.lastOrNull()
                        if (latest == null) {
                            b.tvWeight.text = "--"
                            b.tvHeart.text = "--"
                            b.tvBmi.text = "--"
                        } else {
                            b.tvWeight.text = "%.1f kg".format(latest.weightKg)
                            b.tvHeart.text = "${latest.heartRate} bpm"
                            b.tvBmi.text = "%.1f".format(HealthReportGenerator.bmi(latest.weightKg, latest.heightCm))
                        }
                    }
                }
            }
        }
    }
}