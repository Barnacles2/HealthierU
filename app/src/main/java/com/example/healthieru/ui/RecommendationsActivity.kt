package com.example.healthieru.ui

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.example.healthieru.R
import com.example.healthieru.model.AppData
import com.example.healthieru.model.DoctorNote
import com.example.healthieru.model.HealthReportGenerator
import com.example.healthieru.model.NoteCategory
import com.example.healthieru.model.Patient
import com.google.android.material.chip.ChipGroup

class RecommendationsActivity : BaseActivity() {
    private lateinit var patient: Patient
    private lateinit var container: LinearLayout
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommendations)
        enableBack()

        patient = AppData.currentUser ?: return finish()
        container = findViewById(R.id.listContainer)
        tvEmpty = findViewById(R.id.tvEmpty)

        render(null)

        findViewById<ChipGroup>(R.id.chipGroupFilter).setOnCheckedStateChangeListener { _, ids ->
            render(
                when (ids.firstOrNull()) {
                    R.id.chipFilterExercise -> NoteCategory.EXERCISE
                    R.id.chipFilterDiet -> NoteCategory.DIET
                    R.id.chipFilterSleep -> NoteCategory.SLEEP
                    R.id.chipFilterGeneral -> NoteCategory.GENERAL
                    else -> null
                }
            )
        }
    }

    private fun render(filter: String?) {
        container.removeAllViews()
        tvEmpty.visibility = View.GONE

        // Automatic recommendations (polymorphism: each HealthData builds its own advice)
        val auto: Map<String, String> =
            if (patient.hasRecords()) HealthReportGenerator.generate(patient).recommendations.toMap()
            else emptyMap()

        var shown = 0

        if (filter == null && patient.followUpStatus != "Not reviewed") {
            val card = layoutInflater.inflate(R.layout.item_recommendation, container, false)
            card.findViewById<TextView>(R.id.tvCategory).text = "Follow-up status"
            card.findViewById<TextView>(R.id.tvAdvice).text = patient.followUpStatus
            container.addView(card)
            shown++
        }

        NoteCategory.all.forEach { category ->
            if (filter != null && filter != category) return@forEach
            val autoText = auto[category]
            val notes = patient.getDoctorNotes(category)
            if (autoText == null && notes.isEmpty()) return@forEach
            addCategoryCard(category, autoText, notes)
            shown++
        }

        if (shown == 0) {
            tvEmpty.text =
                if (filter == null) "No health data yet. Record your health data to get recommendations."
                else "No $filter advice yet."
            tvEmpty.visibility = View.VISIBLE
        }
    }

    private fun addCategoryCard(category: String, autoText: String?, notes: List<DoctorNote>) {
        val card = layoutInflater.inflate(R.layout.item_category_card, container, false)
        card.findViewById<TextView>(R.id.tvCategory).text = category

        val tvAuto = card.findViewById<TextView>(R.id.tvAuto)
        if (autoText != null) tvAuto.text = autoText else tvAuto.visibility = View.GONE

        val label = card.findViewById<TextView>(R.id.tvDoctorLabel)
        val notesBox = card.findViewById<LinearLayout>(R.id.notesContainer)
        if (notes.isEmpty()) {
            label.visibility = View.GONE
        } else {
            notes.forEach { note ->
                val row = layoutInflater.inflate(R.layout.item_doctor_note, notesBox, false)
                row.findViewById<TextView>(R.id.tvNoteText).text = note.text
                row.findViewById<TextView>(R.id.tvNoteMeta).text = "${note.author}, ${note.date}"
                notesBox.addView(row)
            }
        }
        container.addView(card)
    }
}