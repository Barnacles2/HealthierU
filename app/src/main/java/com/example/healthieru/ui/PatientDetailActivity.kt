package com.example.healthieru.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.healthieru.R
import com.example.healthieru.model.AppData
import com.example.healthieru.model.HealthReportGenerator
import com.example.healthieru.model.NoteCategory
import com.example.healthieru.model.Patient
import com.example.healthieru.model.ProgressAnalyzer
import com.google.android.material.chip.ChipGroup

class PatientDetailActivity : BaseActivity() {
    private lateinit var patient: Patient
    private lateinit var tvNotes: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_detail)
        enableBack()

        val email = intent.getStringExtra("email") ?: return finish()
        patient = AppData.patients.find { it.email == email } ?: return finish()

        val tvName = findViewById<TextView>(R.id.tvName)
        val tvGoal = findViewById<TextView>(R.id.tvGoal)
        val tvSummary = findViewById<TextView>(R.id.tvSummary)
        val tvTrend = findViewById<TextView>(R.id.tvTrend)
        val chart = findViewById<ProgressChartView>(R.id.chartWeight)
        val statusGroup = findViewById<ChipGroup>(R.id.chipGroupStatus)
        val categoryGroup = findViewById<ChipGroup>(R.id.chipGroupCategory)
        val etNote = findViewById<EditText>(R.id.etNote)
        val tvError = findViewById<TextView>(R.id.tvError)
        val btnSend = findViewById<Button>(R.id.btnSend)
        tvNotes = findViewById(R.id.tvNotes)

        tvName.text = patient.name
        tvGoal.text = "Goal: ${patient.healthGoal}"
        tvSummary.text =
            if (patient.hasRecords()) HealthReportGenerator.generate(patient).toText()
            else "This patient has not recorded any health data yet."

        // Weight progress chart
        val history = patient.getHistory()
        val weights = history.map { it.weightKg }
        chart.setData(history.map { it.label }, weights, "kg", ContextCompat.getColor(this, R.color.primary))
        val trend = ProgressAnalyzer.trend(weights)
        tvTrend.text =
            if (trend == null) "Not enough records to show a trend."
            else "${ProgressAnalyzer.describeChange(trend.change, "kg")} since ${history.first().label}, ${history.size} records"

        // Pre-select the current status
        when (patient.followUpStatus) {
            "Stable" -> statusGroup.check(R.id.chipStable)
            "Needs follow-up" -> statusGroup.check(R.id.chipFollowUp)
            "Urgent review" -> statusGroup.check(R.id.chipUrgent)
        }
        showNotes()

        btnSend.setOnClickListener {
            val advice = etNote.text.toString().trim()
            val status = when (statusGroup.checkedChipId) {
                R.id.chipStable -> "Stable"
                R.id.chipFollowUp -> "Needs follow-up"
                R.id.chipUrgent -> "Urgent review"
                else -> null
            }
            val category = when (categoryGroup.checkedChipId) {
                R.id.chipExercise -> NoteCategory.EXERCISE
                R.id.chipDiet -> NoteCategory.DIET
                R.id.chipSleep -> NoteCategory.SLEEP
                else -> NoteCategory.GENERAL
            }

            if (status == null && advice.isEmpty()) {
                tvError.text = "Choose a follow-up status or write some advice."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            tvError.visibility = View.GONE
            AppData.professional.reviewPatient(patient, status ?: patient.followUpStatus, category, advice)
            etNote.text?.clear()
            showNotes()
            Toast.makeText(this, "Sent to ${patient.name}.", Toast.LENGTH_SHORT).show()
        }
    }

    // Advice grouped by category
    private fun showNotes() {
        val sb = StringBuilder()
        NoteCategory.all.forEach { category ->
            val notes = patient.getDoctorNotes(category)
            if (notes.isNotEmpty()) {
                sb.append(category).append('\n')
                notes.forEach { sb.append("• ${it.text} (${it.date})\n") }
                sb.append('\n')
            }
        }
        tvNotes.text = if (sb.isEmpty()) "No advice sent yet." else sb.toString().trim()
    }
}