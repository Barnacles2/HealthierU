
package com.example.healthieru.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.example.healthieru.HealthierUApp
import com.example.healthieru.R
import com.example.healthieru.data.local.User
import com.example.healthieru.model.AppData
import com.example.healthieru.model.HealthReportGenerator
import com.example.healthieru.viewmodel.HealthViewModel
import com.example.healthieru.viewmodel.HealthViewModelFactory
import kotlinx.coroutines.launch

class ProfileActivity : BaseActivity() {

    private lateinit var healthViewModel: HealthViewModel
    private lateinit var tvError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        enableBack()

        val repository = (application as HealthierUApp).repository
        val factory = HealthViewModelFactory(repository)

        healthViewModel = ViewModelProvider(this, factory)
            .get(HealthViewModel::class.java)

        val etName = findViewById<EditText>(R.id.etName)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etAge = findViewById<EditText>(R.id.etAge)
        val etGoal = findViewById<EditText>(R.id.etGoal)
        tvError = findViewById(R.id.tvError)

        val patient = AppData.currentUser ?: return finish()

        // Observe errors from ViewModel
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                healthViewModel.errorMessage.collect { message ->
                    if (message != null) {
                        tvError.text = message
                        tvError.visibility = View.VISIBLE
                        healthViewModel.clearError()
                    }
                }
            }
        }

        // READ PROFILE
        findViewById<TextView>(R.id.tvEmail).text = patient.email
        etName.setText(patient.name)
        etPhone.setText(patient.phone)
        etAge.setText(patient.age.toString())
        etGoal.setText(patient.healthGoal)

        // UPDATE PROFILE
        findViewById<Button>(R.id.btnSave).setOnClickListener {

            val age = etAge.text.toString().toIntOrNull()

            if (age == null) {
                tvError.text = "Enter a valid age."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            val result = patient.updateProfile(
                etName.text.toString(),
                etPhone.text.toString(),
                age,
                etGoal.text.toString()
            )

            if (result.isFailure) {
                tvError.text =
                    result.exceptionOrNull()?.message ?: "Unable to update profile."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // Prepare updated Room user entity
            val report = HealthReportGenerator.generate(patient)

            val updatedUser = User(
                id = 1,
                name = patient.name,
                age = patient.age,
                weight = patient.weightKg,
                height = patient.heightCm,
                heartRate = patient.heartRateBpm,
                bmi = report.bmi
            )

            tvError.visibility = View.GONE

            // Save through ViewModel
            healthViewModel.saveUser(updatedUser) {
                Toast.makeText(
                    this@ProfileActivity,
                    "Profile updated.",
                    Toast.LENGTH_SHORT
                ).show()

                finish()
            }
        }

        // DELETE PROFILE
        findViewById<Button>(R.id.btnDelete).setOnClickListener {

            AlertDialog.Builder(this)
                .setTitle("Delete your account?")
                .setMessage(
                    "Your profile, health history and advice from your health professional will be permanently removed."
                )
                .setPositiveButton("Delete") { _, _ ->

                    AppData.deletePatient(patient)

                    Toast.makeText(
                        this,
                        "Account deleted.",
                        Toast.LENGTH_SHORT
                    ).show()

                    startActivity(
                        Intent(this, LoginActivity::class.java).addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                        )
                    )
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
