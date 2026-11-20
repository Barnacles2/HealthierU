package com.example.healthieru.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.healthieru.R
import com.example.healthieru.databinding.ActivityRecordDataBinding
import com.example.healthieru.model.AppData
import com.example.healthieru.model.HealthReportGenerator
import com.example.healthieru.data.local.AppDatabase
import com.example.healthieru.data.local.HealthRecord
import com.example.healthieru.data.local.User
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordDataActivity : BaseActivity() {
    private lateinit var b: ActivityRecordDataBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRecordDataBinding.inflate(layoutInflater)
        setContentView(b.root)
        enableBack()

        val db = AppDatabase.getDatabase(this)
        val healthDao = db.healthDao()
        val userDao = db.userDao()

        val patient = AppData.currentUser ?: return finish()

        b.chipGroupActivity.check(
            when (patient.activityLevel) {
                "Low" -> R.id.chipLow
                "High" -> R.id.chipHigh
                else -> R.id.chipModerate
            }
        )

        b.btnSave.setOnClickListener {
            val weightInput = b.etWeight.text.toString().toDoubleOrNull()
            val heightInput = b.etHeight.text.toString().toDoubleOrNull()
            val heartRateInput = b.etHeartRate.text.toString().toIntOrNull()
            val exercise = b.etExercise.text.toString().toIntOrNull()
            val fruitVeg = b.etFruitVeg.text.toString().toIntOrNull()
            val sleep = b.etSleep.text.toString().toDoubleOrNull()

            if (listOf(weightInput, heightInput, heartRateInput, exercise, fruitVeg, sleep).any { it == null }) {
                showError("Enter a number in every field.")
                return@setOnClickListener
            }

            val failure = listOf(
                patient.updateWeight(weightInput!!),
                patient.updateHeight(heightInput!!),
                patient.recordHeartRate(heartRateInput!!),
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
            patient.logCurrentEntry()

            // Save to User Table (Current Stats)
            val report = HealthReportGenerator.generate(patient)
            val updatedUserEntity = User(
                id = 1,
                name = patient.name,
                age = patient.age,
                weight = patient.weightKg,
                height = patient.heightCm,
                heartRate = patient.heartRateBpm,
                bmi = report.bmi
            )

            // Save to HealthRecord Table (History Chart)
            val dateString = SimpleDateFormat("d MMM HH:mm", Locale.getDefault()).format(Date())
            val historyRecord = HealthRecord(
                id = 0,
                date = dateString,
                weightKg = patient.weightKg,
                heartRate = patient.heartRateBpm,
                caloriesIntake = fruitVeg
            )

            lifecycleScope.launch {
                try {
                    healthDao.insert(historyRecord)
                    userDao.upsert(updatedUserEntity)
                    Toast.makeText(this@RecordDataActivity, "Health data saved.", Toast.LENGTH_SHORT).show()
                    finish()
                } catch (e: Exception) {
                    showError("Failed to save data: ${e.message}")
                }
            }
        }
    }

    private fun showError(msg: String) {
        b.tvError.text = msg
        b.tvError.visibility = View.VISIBLE
    }
}