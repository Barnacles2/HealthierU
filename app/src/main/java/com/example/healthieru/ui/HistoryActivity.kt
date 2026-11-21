package com.example.healthieru.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.healthieru.R
import com.example.healthieru.data.local.HealthRecord
import com.example.healthieru.model.Validator
import com.example.healthieru.model.formatDate
import kotlinx.coroutines.launch

class HistoryActivity : BaseActivity() {
    private lateinit var container: LinearLayout
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        enableBack()

        container = findViewById(R.id.listContainer)
        tvEmpty = findViewById(R.id.tvEmpty)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.records.collect { render(it) }
            }
        }
    }

    // READ
    private fun render(records: List<HealthRecord>) {
        container.removeAllViews()
        val entries = records.reversed()
        tvEmpty.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE

        entries.forEach { entry ->
            val row = layoutInflater.inflate(R.layout.item_history, container, false)
            row.findViewById<TextView>(R.id.tvDate).text = formatDate(entry.date)
            row.findViewById<TextView>(R.id.tvValues).text =
                "%.1f kg, %d bpm".format(entry.weightKg, entry.heartRate)
            row.findViewById<Button>(R.id.btnEdit).setOnClickListener { showEditDialog(entry) }
            row.findViewById<Button>(R.id.btnDelete).setOnClickListener { confirmDelete(entry) }
            container.addView(row)
        }
    }

    // UPDATE
    private fun showEditDialog(entry: HealthRecord) {
        val view = layoutInflater.inflate(R.layout.dialog_edit_entry, null)
        val etWeight = view.findViewById<EditText>(R.id.etEntryWeight)
        val etHeart = view.findViewById<EditText>(R.id.etEntryHeart)
        val tvError = view.findViewById<TextView>(R.id.tvEntryError)
        etWeight.setText(entry.weightKg.toString())
        etHeart.setText(entry.heartRate.toString())

        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit entry for ${formatDate(entry.date)}")
            .setView(view)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val weight = etWeight.text.toString().toDoubleOrNull()
                val heart = etHeart.text.toString().toIntOrNull()
                val error = Validator.checkMeasurements(weight, entry.heightCm, heart)
                if (error != null) {
                    tvError.text = error
                    tvError.visibility = View.VISIBLE
                } else {
                    viewModel.editRecord(entry.id, weight!!, heart!!)
                    dialog.dismiss()
                    Toast.makeText(this, "Entry updated.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.show()
    }

    // DELETE
    private fun confirmDelete(entry: HealthRecord) {
        AlertDialog.Builder(this)
            .setTitle("Delete this entry?")
            .setMessage("The record for ${formatDate(entry.date)} will be removed from your history and progress chart.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteRecord(entry.id)
                Toast.makeText(this, "Entry deleted.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}