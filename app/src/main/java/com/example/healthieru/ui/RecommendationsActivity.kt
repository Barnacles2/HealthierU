package com.example.healthieru.ui

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.healthieru.R
import com.example.healthieru.data.local.DoctorNote
import com.example.healthieru.data.local.HealthRecord
import com.example.healthieru.data.local.User
import com.example.healthieru.model.HealthReportGenerator
import com.example.healthieru.model.NoteCategory
import com.example.healthieru.model.formatDate
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import org.json.JSONObject

class RecommendationsActivity : BaseActivity() {
    private lateinit var container: LinearLayout
    private lateinit var tvEmpty: TextView

    private var user: User? = null
    private var latest: HealthRecord? = null
    private var notes: List<DoctorNote> = emptyList()
    private var filter: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommendations)
        enableBack()

        container = findViewById(R.id.listContainer)
        tvEmpty = findViewById(R.id.tvEmpty)

        findViewById<ChipGroup>(R.id.chipGroupFilter).setOnCheckedStateChangeListener { _, ids ->
            filter = when (ids.firstOrNull()) {
                R.id.chipFilterExercise -> NoteCategory.EXERCISE
                R.id.chipFilterDiet -> NoteCategory.DIET
                R.id.chipFilterSleep -> NoteCategory.SLEEP
                R.id.chipFilterGeneral -> NoteCategory.GENERAL
                else -> null
            }
            render()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.currentUser.collect { user = it; render() } }
                launch { viewModel.records.collect { latest = it.lastOrNull(); render() } }
                launch { viewModel.notes.collect { notes = it; render() } }
            }
        }
    }

    private fun render() {
        container.removeAllViews()
        tvEmpty.visibility = View.GONE
        var shown = 0

        val auto: Map<String, String> = latest?.let { rec ->
            user?.let { HealthReportGenerator.generate(it, rec).recommendations.toMap() }
        } ?: emptyMap()

        val status = user?.followUpStatus
        if (filter == null && status != null && status != "Not reviewed") {
            val card = layoutInflater.inflate(R.layout.item_recommendation, container, false)
            card.findViewById<TextView>(R.id.tvCategory).text = "Follow-up status"
            card.findViewById<TextView>(R.id.tvAdvice).text = status
            container.addView(card)
            shown++
        }

        NoteCategory.all.forEach { category ->
            if (filter != null && filter != category) return@forEach
            val autoText = auto[category]
            val categoryNotes = notes.filter { it.category == category }
            if (autoText == null && categoryNotes.isEmpty()) return@forEach

            val card = layoutInflater.inflate(R.layout.item_category_card, container, false)
            card.findViewById<TextView>(R.id.tvCategory).text = category

            val tvAuto = card.findViewById<TextView>(R.id.tvAuto)
            if (autoText != null) tvAuto.text = autoText else tvAuto.visibility = View.GONE

            val label = card.findViewById<TextView>(R.id.tvDoctorLabel)
            val box = card.findViewById<LinearLayout>(R.id.notesContainer)

            if (categoryNotes.isEmpty()) {
                label.visibility = View.GONE
            } else {
                categoryNotes.forEach { note ->
                    // Check if it's a structured schedule (Diet)
                    if (note.structuredData != null && note.category == NoteCategory.DIET) {
                        val tableRow = layoutInflater.inflate(R.layout.item_diet_schedule, box, false)
                        val json = JSONObject(note.structuredData!!)

                        tableRow.findViewById<TextView>(R.id.tvBreakfastVal).text = json.optString("breakfast", "-")
                        tableRow.findViewById<TextView>(R.id.tvLunchVal).text = json.optString("lunch", "-")
                        tableRow.findViewById<TextView>(R.id.tvDinnerVal).text = json.optString("dinner", "-")

                        val link = json.optString("link", "")
                        val tvLink = tableRow.findViewById<TextView>(R.id.tvDietLink)
                        if (link.isNotEmpty()) {
                            tvLink.movementMethod = LinkMovementMethod.getInstance()
                            tvLink.text = "▶️ Recipe Video: $link"
                        } else {
                            tvLink.visibility = View.GONE
                        }
                        box.addView(tableRow)

                    } else {
                        // Standard text note (General Advice or fallback)
                        val row = layoutInflater.inflate(R.layout.item_doctor_note, box, false)
                        val tvNoteText = row.findViewById<TextView>(R.id.tvNoteText)
                        tvNoteText.text = note.text
                        tvNoteText.movementMethod = LinkMovementMethod.getInstance()
                        row.findViewById<TextView>(R.id.tvNoteMeta).text = "${note.author}, ${formatDate(note.date)}"
                        box.addView(row)
                    }
                }
            }
            container.addView(card)
            shown++
        }

        if (shown == 0) {
            tvEmpty.text =
                if (filter == null) "No health data yet. Record your health data to get recommendations."
                else "No $filter advice yet."
            tvEmpty.visibility = View.VISIBLE
        }
    }
}