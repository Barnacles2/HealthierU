
package com.example.healthieru.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.example.healthieru.HealthierUApp
import com.example.healthieru.R
import com.example.healthieru.data.local.HealthRecord
import com.example.healthieru.databinding.ActivityHistoryBinding
import com.example.healthieru.viewmodel.HealthViewModel
import com.example.healthieru.viewmodel.HealthViewModelFactory
import kotlinx.coroutines.launch

class HistoryActivity : BaseActivity() {

    private lateinit var b: ActivityHistoryBinding
    private lateinit var container: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var healthViewModel: HealthViewModel

    private var currentRecords: List<HealthRecord> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(b.root)
        enableBack()

        container = findViewById(R.id.listContainer)
        tvEmpty = findViewById(R.id.tvEmpty)

        val repository = (application as HealthierUApp).repository
        val factory = HealthViewModelFactory(repository)

        healthViewModel = ViewModelProvider(this, factory)
            .get(HealthViewModel::class.java)

        // Observe health records through the ViewModel
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                healthViewModel.allRecords.collect { records ->
                    currentRecords = records
                    render(records)
                }
            }
        }

        // Observe ViewModel errors
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                healthViewModel.errorMessage.collect { message ->
                    if (message != null) {
                        Toast.makeText(
                            this@HistoryActivity,
                            message,
                            Toast.LENGTH_SHORT
                        ).show()

                        healthViewModel.clearError()
                    }
                }
            }
        }
    }

    // DISPLAY HEALTH RECORDS
    private fun render(records: List<HealthRecord>) {
        container.removeAllViews()

        tvEmpty.visibility =
            if (records.isEmpty()) View.VISIBLE else View.GONE

        records.forEach { record ->
            val row = layoutInflater.inflate(
                R.layout.item_history,
                container,
                false
            )

            row.findViewById<TextView>(R.id.tvDate).text = record.date

            row.findViewById<TextView>(R.id.tvValues).text =
                getString(
                    R.string.history_values_format,
                    record.weightKg,
                    record.heartRate
                )

            row.findViewById<Button>(R.id.btnEdit).setOnClickListener {
                showEditDialog(record)
            }

            row.findViewById<Button>(R.id.btnDelete).setOnClickListener {
                confirmDelete(record)
            }

            container.addView(row)
        }
    }

    // UPDATE HEALTH RECORD
    private fun showEditDialog(record: HealthRecord) {
        val view = layoutInflater.inflate(
            R.layout.dialog_edit_entry,
            null
        )

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
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {

                    val weight = etWeight.text.toString().toDoubleOrNull()
                    val heart = etHeart.text.toString().toIntOrNull()

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

                    val updatedRecord = record.copy(
                        weightKg = weight,
                        heartRate = heart
                    )

                    healthViewModel.updateRecord(updatedRecord)

                    updatePatientCurrentStats(updatedRecord)

                    dialog.dismiss()

                    Toast.makeText(
                        this@HistoryActivity,
                        getString(R.string.entry_updated),
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        dialog.show()
    }

    // DELETE HEALTH RECORD
    private fun confirmDelete(record: HealthRecord) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_entry_title))
            .setMessage(
                getString(R.string.delete_entry_message, record.date)
            )
            .setPositiveButton("Delete") { _, _ ->

                healthViewModel.deleteRecord(record)

                Toast.makeText(
                    this@HistoryActivity,
                    getString(R.string.entry_deleted),
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // UPDATE CURRENT PATIENT STATISTICS
    private fun updatePatientCurrentStats(editedRecord: HealthRecord) {
        val patient =
            com.example.healthieru.model.AppData.currentUser ?: return

        if (
            currentRecords.isNotEmpty() &&
            currentRecords.first().id == editedRecord.id
        ) {
            patient.weightKg = editedRecord.weightKg
            patient.heartRateBpm = editedRecord.heartRate
        }
    }
}
