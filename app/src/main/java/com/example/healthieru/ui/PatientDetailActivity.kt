package com.example.healthieru.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.healthieru.R
import com.example.healthieru.data.local.AppDatabase
import com.example.healthieru.data.local.HealthRecord
import com.example.healthieru.model.AppData
import com.example.healthieru.model.DoctorNote
import com.example.healthieru.model.HealthEntry
import com.example.healthieru.model.HealthReportGenerator
import com.example.healthieru.model.NoteCategory
import com.example.healthieru.model.Patient
import com.example.healthieru.model.ProgressAnalyzer
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class PatientDetailActivity : BaseActivity() {
    private lateinit var patient: Patient
    private lateinit var tvNotes: TextView
    private lateinit var notesContainer: LinearLayout

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
        notesContainer = findViewById(R.id.notesContainer)

        tvName.text = patient.name
        tvGoal.text = "Goal: ${patient.healthGoal}"

        // Initialize Room
        val db = AppDatabase.getDatabase(this)
        val userDao = db.userDao()
        val healthDao = db.healthDao()

        // OBSERVE ROOM DATABASE FOR LATEST STATS AND CHART
        lifecycleScope.launch {
            // 1. Observe User Table for Summary
            userDao.getUser().collect { savedUser ->
                if (savedUser != null) {
                    // Update patient object in memory
                    patient.weightKg = savedUser.weight
                    patient.heightCm = savedUser.height
                    patient.heartRateBpm = savedUser.heartRate

                    tvSummary.text = HealthReportGenerator.generate(patient).toText()
                } else {
                    tvSummary.text = "This patient has not recorded any health data yet."
                }
            }
        }

        lifecycleScope.launch {
            // 2. Observe Health Records for the Chart
            healthDao.getAllRecords().collect { records ->
                if (records.isNotEmpty()) {
                    // Reverse so chart is left-to-right (oldest to newest)
                    val chronologicalRecords = records.reversed()

                    val labels = chronologicalRecords.map { it.date }
                    val weights = chronologicalRecords.map { it.weightKg }

                    chart.setData(labels, weights, "kg", ContextCompat.getColor(this@PatientDetailActivity, R.color.primary))

                    val trend = ProgressAnalyzer.trend(weights)
                    tvTrend.text = if (trend == null) {
                        "Not enough records to show a trend."
                    } else {
                        "${ProgressAnalyzer.describeChange(trend.change, "kg")} since ${labels.first()}, ${labels.size} records"
                    }
                } else {
                    tvTrend.text = "Not enough records to show a trend."
                    chart.setData(emptyList(), emptyList(), "kg", ContextCompat.getColor(this@PatientDetailActivity, R.color.primary))
                }
            }
        }

        when (patient.followUpStatus) {
            "Stable" -> statusGroup.check(R.id.chipStable)
            "Needs follow-up" -> statusGroup.check(R.id.chipFollowUp)
            "Urgent review" -> statusGroup.check(R.id.chipUrgent)
        }
        showNotes()

        // CREATE
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

    // READ: every note is a row with Edit and Delete buttons
    private fun showNotes() {
        notesContainer.removeAllViews()
        val notes = patient.getDoctorNotes()
        tvNotes.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE

        notes.forEach { note ->
            val row = layoutInflater.inflate(R.layout.item_note_row, notesContainer, false)
            row.findViewById<TextView>(R.id.tvNoteCategory).text = note.category
            row.findViewById<TextView>(R.id.tvNoteText).text = note.text
            row.findViewById<TextView>(R.id.tvNoteMeta).text = "${note.author}, ${note.date}"
            row.findViewById<Button>(R.id.btnNoteEdit).setOnClickListener { showEditDialog(note) }
            row.findViewById<Button>(R.id.btnNoteDelete).setOnClickListener { confirmDelete(note) }
            notesContainer.addView(row)
        }
    }

    // UPDATE
    private fun showEditDialog(note: DoctorNote) {
        val input = EditText(this).apply {
            setText(note.text)
            setSelection(text.length)
            minLines = 2
            gravity = android.view.Gravity.TOP
        }
        val pad = (20 * resources.displayMetrics.density).toInt()
        val frame = FrameLayout(this).apply {
            setPadding(pad, pad / 2, pad, 0)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle("Edit ${note.category} advice")
            .setView(frame)
            .setPositiveButton("Save") { _, _ ->
                val result = AppData.professional.editNote(patient, note.id, input.text.toString())
                if (result.isFailure) {
                    Toast.makeText(this, result.exceptionOrNull()?.message, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Advice updated.", Toast.LENGTH_SHORT).show()
                    showNotes()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // DELETE
    private fun confirmDelete(note: DoctorNote) {
        AlertDialog.Builder(this)
            .setTitle("Delete this advice?")
            .setMessage("${patient.name} will no longer see it.")
            .setPositiveButton("Delete") { _, _ ->
                AppData.professional.deleteNote(patient, note.id)
                Toast.makeText(this, "Advice deleted.", Toast.LENGTH_SHORT).show()
                showNotes()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}