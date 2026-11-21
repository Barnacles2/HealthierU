package com.example.healthieru.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.healthieru.R
import com.example.healthieru.model.Validator
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class RecordDataActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_data)
        enableBack()

        val etWeight = findViewById<EditText>(R.id.etWeight)
        val etHeight = findViewById<EditText>(R.id.etHeight)
        val etHeart = findViewById<EditText>(R.id.etHeartRate)
        val etExercise = findViewById<EditText>(R.id.etExercise)
        val etFruitVeg = findViewById<EditText>(R.id.etFruitVeg)
        val etSleep = findViewById<EditText>(R.id.etSleep)
        val chips = findViewById<ChipGroup>(R.id.chipGroupActivity)
        val tvError = findViewById<TextView>(R.id.tvError)

        chips.check(R.id.chipModerate)

        // Pre-fill with the patient's latest record
        lifecycleScope.launch {
            viewModel.latestRecord()?.let { r ->
                etWeight.setText(r.weightKg.toString())
                etHeight.setText(r.heightCm.toString())
                etHeart.setText(r.heartRate.toString())
                etExercise.setText(r.exerciseMinutes.toString())
                etFruitVeg.setText(r.fruitVeg.toString())
                etSleep.setText(r.sleepHours.toString())
                chips.check(
                    when (r.activityLevel) {
                        "Low" -> R.id.chipLow
                        "High" -> R.id.chipHigh
                        else -> R.id.chipModerate
                    }
                )
            }
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            val weight = etWeight.text.toString().toDoubleOrNull()
            val height = etHeight.text.toString().toDoubleOrNull()
            val heart = etHeart.text.toString().toIntOrNull()
            val exercise = etExercise.text.toString().toIntOrNull()
            val fruitVeg = etFruitVeg.text.toString().toIntOrNull()
            val sleep = etSleep.text.toString().toDoubleOrNull()

            val error = Validator.checkMeasurements(weight, height, heart)
                ?: Validator.checkLifestyle(exercise, fruitVeg, sleep)
            if (error != null) {
                tvError.text = error
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            val activity = when (chips.checkedChipId) {
                R.id.chipLow -> "Low"
                R.id.chipHigh -> "High"
                else -> "Moderate"
            }
            viewModel.addRecord(weight!!, height!!, heart!!, activity, exercise!!, fruitVeg!!, sleep!!)
            Toast.makeText(this, "Health data saved.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}