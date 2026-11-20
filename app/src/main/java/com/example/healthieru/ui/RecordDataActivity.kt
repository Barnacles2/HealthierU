package com.example.healthieru.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.example.healthieru.R
import com.example.healthieru.databinding.ActivityRecordDataBinding
import com.example.healthieru.model.AppData

class RecordDataActivity : BaseActivity() {
    private lateinit var b: ActivityRecordDataBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRecordDataBinding.inflate(layoutInflater)
        setContentView(b.root)
        enableBack()

        val patient = AppData.currentUser ?: return finish()

        b.chipGroupActivity.check(
            when (patient.activityLevel) {
                "Low" -> R.id.chipLow
                "High" -> R.id.chipHigh
                else -> R.id.chipModerate
            }
        )

        b.btnSave.setOnClickListener {
            val weight = b.etWeight.text.toString().toDoubleOrNull()
            val height = b.etHeight.text.toString().toDoubleOrNull()
            val heartRate = b.etHeartRate.text.toString().toIntOrNull()
            val exercise = b.etExercise.text.toString().toIntOrNull()
            val fruitVeg = b.etFruitVeg.text.toString().toIntOrNull()
            val sleep = b.etSleep.text.toString().toDoubleOrNull()

            if (listOf(weight, height, heartRate, exercise, fruitVeg, sleep).any { it == null }) {
                showError("Enter a number in every field.")
                return@setOnClickListener
            }

            // Encapsulation: the Patient object validates every value
            val failure = listOf(
                patient.updateWeight(weight!!),
                patient.updateHeight(height!!),
                patient.recordHeartRate(heartRate!!),
                patient.recordLifestyle(exercise!!, fruitVeg!!, sleep!!)
            ).firstOrNull { it.isFailure }

            if (failure != null) {
                showError(failure.exceptionOrNull()?.message ?: "Check your entries.")
                return@setOnClickListener
            }

            patient.updateActivityLevel(
                when (b.chipGroupActivity.checkedChipId) {
                    R.id.chipLow -> "Low"
                    R.id.chipHigh -> "High"
                    else -> "Moderate"
                }
            )
            patient.logCurrentEntry()   // saves today's point for the progress chart
            Toast.makeText(this, "Health data saved.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showError(msg: String) {
        b.tvError.text = msg
        b.tvError.visibility = View.VISIBLE
    }
}