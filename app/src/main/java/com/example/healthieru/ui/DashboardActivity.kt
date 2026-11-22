package com.example.healthieru.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.healthieru.HealthierUApp
import com.example.healthieru.R
import com.example.healthieru.data.SessionManager
import com.example.healthieru.databinding.ActivityDashboardBinding
import com.example.healthieru.model.HealthReportGenerator
import com.example.healthieru.viewmodel.HealthViewModel
import com.example.healthieru.viewmodel.HealthViewModelFactory
import com.example.healthieru.work.ReminderWorker
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

        // 1. Initialize ViewModel
        val repository = (application as HealthierUApp).repository
        val session = SessionManager(this)
        val factory = HealthViewModelFactory(repository, session)
        healthViewModel = ViewModelProvider(this, factory).get(HealthViewModel::class.java)

        // 2. Expandable Menu Logic
        b.btnToggleMenu.setOnClickListener {
            if (b.expandedMenuContainer.isGone) {
                b.expandedMenuContainer.isVisible = true
                b.btnToggleMenu.text = getString(R.string.show_less)
            } else {
                b.expandedMenuContainer.isVisible = false
                b.btnToggleMenu.text = getString(R.string.view_progress_history)
            }
        }

        // 3. Load the Exercise Tip from the API
        healthViewModel.loadTip()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                healthViewModel.tip.collect { tip ->
                    // Display "Exercise: Barbell Bench Press"
                    b.tvTip.text = if (tip != null) {
                        getString(R.string.exercise_tip_format, tip)
                    } else {
                        getString(R.string.loading)
                    }
                }
            }
        }

        // 4. Navigation Cards
        b.cardProfile.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }
        b.cardRecord.setOnClickListener { startActivity(Intent(this, RecordDataActivity::class.java)) }
        b.cardReport.setOnClickListener { startActivity(Intent(this, ReportActivity::class.java)) }
        b.cardAdvice.setOnClickListener { startActivity(Intent(this, RecommendationsActivity::class.java)) }
        b.cardHistory.setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }
        b.cardProgress.setOnClickListener { startActivity(Intent(this, ProgressActivity::class.java)) }

        // 5. Logout Button
        b.btnLogout.setOnClickListener {
            healthViewModel.logout()
            goToLogin()
        }

        // 6. Ask for notification permission
        if (Build.VERSION.SDK_INT >= 33) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // 7. Schedule Daily Reminder
        val reminder = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_health_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            reminder
        )

        // 8. Observe Data from Room Database
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    healthViewModel.currentUser.collect { user ->
                        if (user != null) {
                            b.tvGreeting.text = getString(R.string.greeting_format, user.name.substringBefore(' '))
                            b.tvGoal.text = getString(R.string.goal_format, user.healthGoal)
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
                            b.tvWeight.text = getString(R.string.weight_format, latest.weightKg)
                            b.tvHeart.text = getString(R.string.heart_format, latest.heartRate)
                            b.tvBmi.text = getString(R.string.bmi_format, HealthReportGenerator.bmi(latest.weightKg, latest.heightCm))
                        }
                    }
                }
            }
        }
    }
}