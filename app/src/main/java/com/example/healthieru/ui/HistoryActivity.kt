package com.example.healthieru.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.healthieru.R
import com.example.healthieru.data.local.AppDatabase
import com.example.healthieru.data.local.HealthRecord
import com.example.healthieru.databinding.ActivityHistoryBinding
import kotlinx.coroutines.launch

class HistoryActivity : BaseActivity() {
    private lateinit var b: ActivityHistoryBinding
    private lateinit var container: LinearLayout
    private lateinit var tvEmpty: TextView

    private val db by lazy { AppDatabase.getDatabase(this) }
    private val healthDao by lazy { db.healthDao() }

    // Keep track of the current list of records so we don't have to query the DB again
    private var currentRecords: List<HealthRecord> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(b.root)
        enableBack()

        container = findViewById(R.id.listContainer)
        tvEmpty = findViewById(R.id.tvEmpty)

        // Observe the database. Whenever data changes, this will re-draw the list.
        lifecycleScope.launch {
            healthDao.getAllRecords().collect { records ->
                currentRecords = records
                render(records)
            }
        }
    }

    // READ
    private fun render(records: List<HealthRecord>) {
        container.removeAllViews()
        tvEmpty.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE

        records.forEach { record ->
            val row = layoutInflater.inflate(R.layout.item_history, container, false)
            row.findViewById<TextView>(R.id.tvDate).text = record.date

            // Format string: e.g. "45.0 kg, 200 bpm"
            row.findViewById<TextView>(R.id.tvValues).text =
                getString(R.string.history_values_format, record.weightKg, record.heartRate)

            row.findViewById<Button>(R.id.btnEdit).setOnClickListener { showEditDialog(record) }
            row.findViewById<Button>(R.id.btnDelete).setOnClickListener { confirmDelete(record) }
            container.addView(row)
        }
    }

    // UPDATE
    private fun showEditDialog(record: HealthRecord) {
        val view = layoutInflater.inflate(R.layout.dialog_edit_entry, null)
        val etWeight = view.findViewById<EditText>(R.id.etEntryWeight)
        val etHeart = view.findViewById<EditText>(R.id.etEntryHeart)
        val tvError = view.findViewById<TextView>(R.id.tvEntryError)

        etWeight.setText(record.weightKg.toString())
        etHeart.setText(record.heartRate.toString())

        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.edit_entry_title, record.date))
            .setView(view)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val weight = etWeight.text.toString().toDoubleOrNull()
                val heart = etHeart.text.toString().toIntOrNull()

                // Validation
                if (weight == null || heart == null) {
                    tvError.text = getString(R.string.error_enter_numbers)
                    tvError.visibility = View.VISIBLE
                    return@setOnClickListener
                }
                if (weight !in 20.0..300.0 || heart !in 30..220) {
                    tvError.text = getString(R.string.error_invalid_range)
                    tvError.visibility = View.VISIBLE
                    return@setOnClickListener
                }

                // Save to Room Database
                val updatedRecord = record.copy(weightKg = weight, heartRate = heart)
                lifecycleScope.launch {
                    healthDao.insert(updatedRecord) // Upsert replaces the old row
                    dialog.dismiss()
                    Toast.makeText(this@HistoryActivity, getString(R.string.entry_updated), Toast.LENGTH_SHORT).show()

                    // Also update the patient's current stats if this was the most recent entry
                    updatePatientCurrentStats(updatedRecord)
                }
            }
        }
        dialog.show()
    }

    // DELETE
    private fun confirmDelete(record: HealthRecord) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_entry_title))
            .setMessage(getString(R.string.delete_entry_message, record.date))
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    healthDao.delete(record)
                    Toast.makeText(this@HistoryActivity, getString(R.string.entry_deleted), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private suspend fun updatePatientCurrentStats(editedRecord: HealthRecord) {
        val patient = com.example.healthieru.model.AppData.currentUser ?: return

        // Since we already have the list of records, we check if the edited one is the newest
        if (currentRecords.isNotEmpty() && currentRecords.first().id == editedRecord.id) {
            patient.weightKg = editedRecord.weightKg
            patient.heartRateBpm = editedRecord.heartRate

            // Optional but recommended: Update the User table as well
            val userDao = AppDatabase.getDatabase(this).userDao()
            // You would call userDao.upsert() here if you want the Home screen to instantly update
        }
    }
}