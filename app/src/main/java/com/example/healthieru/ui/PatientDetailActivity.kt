package com.example.healthieru.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.healthieru.R
import com.example.healthieru.data.local.DoctorNote
import com.example.healthieru.data.local.HealthRecord
import com.example.healthieru.data.local.User
import com.example.healthieru.model.HealthReportGenerator
import com.example.healthieru.model.NoteCategory
import com.example.healthieru.model.ProgressAnalyzer
import com.example.healthieru.model.formatDate
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import org.json.JSONObject

class PatientDetailActivity : BaseActivity() {
    private var patientId = -1L
    private var statusPreselected = false

    private lateinit var tvNotes: TextView
    private lateinit var notesContainer: LinearLayout
    private lateinit var statusGroup: ChipGroup

    private lateinit var layoutGeneralInput: View
    private lateinit var layoutDietInput: View
    private lateinit var layoutWorkoutInput: View

    private lateinit var etBreakfast: EditText
    private lateinit var etLunch: EditText
    private lateinit var etDinner: EditText
    private lateinit var etDietLink: EditText

    private lateinit var etEx1: EditText
    private lateinit var etSets1: EditText
    private lateinit var etReps1: EditText
    private lateinit var etEx2: EditText
    private lateinit var etSets2: EditText
    private lateinit var etReps2: EditText
    private lateinit var etWorkoutLink: EditText

    private lateinit var etGeneralAdvice: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_detail)
        enableBack()

        patientId = intent.getLongExtra("userId", -1L)
        if (patientId == -1L) { finish(); return }

        tvNotes = findViewById(R.id.tvNotes)
        notesContainer = findViewById(R.id.notesContainer)
        statusGroup = findViewById(R.id.chipGroupStatus)
        val categoryGroup = findViewById<ChipGroup>(R.id.chipGroupCategory)
        val tvError = findViewById<TextView>(R.id.tvError)

        layoutGeneralInput = findViewById(R.id.layoutGeneralInput)
        layoutDietInput = findViewById(R.id.layoutDietInput)
        layoutWorkoutInput = findViewById(R.id.layoutWorkoutInput)

        etGeneralAdvice = findViewById(R.id.etGeneralAdvice)

        etBreakfast = findViewById(R.id.etBreakfast)
        etLunch = findViewById(R.id.etLunch)
        etDinner = findViewById(R.id.etDinner)
        etDietLink = findViewById(R.id.etDietLink)

        etEx1 = findViewById(R.id.etEx1)
        etSets1 = findViewById(R.id.etSets1)
        etReps1 = findViewById(R.id.etReps1)
        etEx2 = findViewById(R.id.etEx2)
        etSets2 = findViewById(R.id.etSets2)
        etReps2 = findViewById(R.id.etReps2)
        etWorkoutLink = findViewById(R.id.etWorkoutLink)

        categoryGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            layoutGeneralInput.visibility = View.GONE
            layoutDietInput.visibility = View.GONE
            layoutWorkoutInput.visibility = View.GONE

            when (checkedIds.firstOrNull()) {
                R.id.chipDiet -> layoutDietInput.visibility = View.VISIBLE
                R.id.chipWorkout -> layoutWorkoutInput.visibility = View.VISIBLE
                else -> layoutGeneralInput.visibility = View.VISIBLE
            }
        }

        findViewById<Button>(R.id.btnSendToPatient).setOnClickListener {
            val status = when (statusGroup.checkedChipId) {
                R.id.chipStable -> "Stable"
                R.id.chipFollowUp -> "Needs follow-up"
                R.id.chipUrgent -> "Urgent review"
                else -> null
            }
            val category = when (categoryGroup.checkedChipId) {
                R.id.chipWorkout -> NoteCategory.EXERCISE
                R.id.chipDiet -> NoteCategory.DIET
                else -> NoteCategory.GENERAL
            }

            var textAdvice = ""
            var jsonData: String? = null

            when (category) {
                NoteCategory.DIET -> {
                    val breakfast = etBreakfast.text.toString().trim()
                    val lunch = etLunch.text.toString().trim()
                    val dinner = etDinner.text.toString().trim()
                    val link = etDietLink.text.toString().trim()

                    if (breakfast.isEmpty() && lunch.isEmpty() && dinner.isEmpty()) {
                        tvError.text = "Please fill in at least one meal."
                        tvError.visibility = View.VISIBLE
                        return@setOnClickListener
                    }

                    val json = JSONObject()
                    json.put("breakfast", breakfast)
                    json.put("lunch", lunch)
                    json.put("dinner", dinner)
                    json.put("link", link)
                    jsonData = json.toString()
                    textAdvice = "Diet Schedule"
                }
                NoteCategory.EXERCISE -> {
                    val ex1 = etEx1.text.toString().trim()
                    val sets1 = etSets1.text.toString().trim()
                    val reps1 = etReps1.text.toString().trim()
                    val ex2 = etEx2.text.toString().trim()
                    val sets2 = etSets2.text.toString().trim()
                    val reps2 = etReps2.text.toString().trim()
                    val link = etWorkoutLink.text.toString().trim()

                    if (ex1.isEmpty() && ex2.isEmpty()) {
                        tvError.text = "Please fill in at least one exercise."
                        tvError.visibility = View.VISIBLE
                        return@setOnClickListener
                    }

                    val json = JSONObject()
                    json.put("ex1", ex1)
                    json.put("sets1", sets1)
                    json.put("reps1", reps1)
                    json.put("ex2", ex2)
                    json.put("sets2", sets2)
                    json.put("reps2", reps2)
                    json.put("link", link)
                    jsonData = json.toString()
                    textAdvice = "Workout Schedule"
                }
                else -> {
                    textAdvice = etGeneralAdvice.text.toString().trim()
                    if (textAdvice.isEmpty()) {
                        tvError.text = "Please write some advice."
                        tvError.visibility = View.VISIBLE
                        return@setOnClickListener
                    }
                }
            }

            if (status == null && textAdvice.isEmpty()) {
                tvError.text = "Choose a follow-up status or write some advice."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            tvError.visibility = View.GONE
            if (status != null) viewModel.setStatus(patientId, status)
            if (textAdvice.isNotEmpty()) {
                viewModel.addStructuredNote(patientId, category, textAdvice, jsonData)
            }

            clearAllInputs(categoryGroup)
            Toast.makeText(this, "Sent to patient.", Toast.LENGTH_SHORT).show()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.patient(patientId).collect { showPatient(it) } }
                launch { viewModel.patientRecords(patientId).collect { showRecords(it) } }
                launch { viewModel.patientNotes(patientId).collect { showNotes(it) } }
            }
        }
    }

    private fun clearAllInputs(categoryGroup: ChipGroup) {
        etGeneralAdvice.text?.clear()
        etBreakfast.text?.clear()
        etLunch.text?.clear()
        etDinner.text?.clear()
        etDietLink.text?.clear()
        etEx1.text?.clear()
        etSets1.text?.clear()
        etReps1.text?.clear()
        etEx2.text?.clear()
        etSets2.text?.clear()
        etReps2.text?.clear()
        etWorkoutLink.text?.clear()

        categoryGroup.clearCheck()
        layoutGeneralInput.visibility = View.GONE
        layoutDietInput.visibility = View.GONE
        layoutWorkoutInput.visibility = View.GONE
    }

    private var summaryUser: User? = null
    private var summaryRecord: HealthRecord? = null

    private fun showPatient(user: User?) {
        if (user == null) { finish(); return }
        summaryUser = user
        findViewById<TextView>(R.id.tvName).text = user.name
        findViewById<TextView>(R.id.tvGoal).text = "Goal: ${user.healthGoal}"

        if (!statusPreselected) {
            statusPreselected = true
            when (user.followUpStatus) {
                "Stable" -> statusGroup.check(R.id.chipStable)
                "Needs follow-up" -> statusGroup.check(R.id.chipFollowUp)
                "Urgent review" -> statusGroup.check(R.id.chipUrgent)
            }
        }
        showSummary()
    }

    private fun showRecords(records: List<HealthRecord>) {
        summaryRecord = records.lastOrNull()
        showSummary()

        if (records.isNotEmpty()) {
            val weights = records.map { it.weightKg }
            findViewById<ProgressChartView>(R.id.chartWeight).setData(
                records.map { formatDate(it.date) }, weights, "kg",
                ContextCompat.getColor(this, R.color.primary)
            )
            val trend = ProgressAnalyzer.trend(weights)
            findViewById<TextView>(R.id.tvTrend).text =
                if (trend == null) "Not enough records to show a trend."
                else "${ProgressAnalyzer.describeChange(trend.change, "kg")} since ${formatDate(records.first().date)}, ${records.size} records"
        }
    }

    private fun showSummary() {
        val user = summaryUser
        val record = summaryRecord
        findViewById<TextView>(R.id.tvSummary).text =
            if (user != null && record != null) HealthReportGenerator.generate(user, record).toText()
            else "This patient has not recorded any health data yet."
    }

    private fun showNotes(notes: List<DoctorNote>) {
        notesContainer.removeAllViews()
        tvNotes.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE

        notes.forEach { note ->
            val row = layoutInflater.inflate(R.layout.item_note_row, notesContainer, false)
            row.findViewById<TextView>(R.id.tvNoteCategory).text = note.category
            row.findViewById<TextView>(R.id.tvNoteText).text = note.text
            row.findViewById<TextView>(R.id.tvNoteMeta).text = "${note.author}, ${formatDate(note.date)}"
            row.findViewById<Button>(R.id.btnNoteEdit).setOnClickListener { showEditDialog(note) }
            row.findViewById<Button>(R.id.btnNoteDelete).setOnClickListener { confirmDelete(note) }
            notesContainer.addView(row)
        }
    }

    private fun showEditDialog(note: DoctorNote) {
        val input = EditText(this).apply {
            setText(note.text)
            setSelection(text.length)
            minLines = 2
            gravity = Gravity.TOP
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
                viewModel.editNote(patientId, note.id, input.text.toString())
                Toast.makeText(this, "Advice updated.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(note: DoctorNote) {
        AlertDialog.Builder(this)
            .setTitle("Delete this advice?")
            .setMessage("The patient will no longer see it.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteNote(patientId, note.id)
                Toast.makeText(this, "Advice deleted.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}