package com.example.healthieru.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.healthieru.R
import com.example.healthieru.model.HealthReportGenerator
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class DashboardActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // 1. Initialize all TextViews for the Dashboard
        val tvGreeting = findViewById<TextView>(R.id.tvGreeting)
        val tvGoal = findViewById<TextView>(R.id.tvGoal)
        val tvWeight = findViewById<TextView>(R.id.tvWeight)
        val tvHeart = findViewById<TextView>(R.id.tvHeart)
        val tvBmi = findViewById<TextView>(R.id.tvBmi)

        // 2. Initialize the Expandable Menu Views
        val expandedMenu = findViewById<LinearLayout>(R.id.expandedMenuContainer)
        val btnToggleMenu = findViewById<MaterialButton>(R.id.btnToggleMenu)

        // 3. Set up the Expand/Collapse Button logic
        btnToggleMenu.setOnClickListener {
            if (expandedMenu.visibility == View.GONE) {
                expandedMenu.visibility = View.VISIBLE
                btnToggleMenu.text = "Show Less"
            } else {
                expandedMenu.visibility = View.GONE
                btnToggleMenu.text = "View Progress & History"
            }
        }

        // 4. Link the Dashboard Cards to their respective Activities
        fun link(cardId: Int, target: Class<*>) {
            findViewById<View>(cardId).setOnClickListener {
                startActivity(Intent(this, target))
            }
        }

        link(R.id.cardRecord, RecordDataActivity::class.java)
        link(R.id.cardReport, ReportActivity::class.java)
        link(R.id.cardAdvice, RecommendationsActivity::class.java) // The new notification card
        link(R.id.cardProgress, ProgressActivity::class.java)
        link(R.id.cardHistory, HistoryActivity::class.java)
        link(R.id.cardProfile, ProfileActivity::class.java)

        // 5. Set up Logout Button
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            viewModel.logout()
            goToLogin()
        }

        // 6. Collect Data from ViewModel
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Collect User Info (Greeting and Goal)
                launch {
                    viewModel.currentUser.collect { user ->
                        if (user != null) {
                            tvGreeting.text = "Hello, ${user.name.substringBefore(' ')}"
                            tvGoal.text = "Goal: ${user.healthGoal}"
                        }
                    }
                }
                // Collect Health Records (Stats)
                launch {
                    viewModel.records.collect { list ->
                        val latest = list.lastOrNull()
                        if (latest == null) {
                            tvWeight.text = "--"
                            tvHeart.text = "--"
                            tvBmi.text = "--"
                        } else {
                            tvWeight.text = "%.1f kg".format(latest.weightKg)
                            tvHeart.text = "${latest.heartRate} bpm"
                            tvBmi.text = "%.1f".format(HealthReportGenerator.bmi(latest.weightKg, latest.heightCm))
                        }
                    }
                }
            }
        }
    }
}