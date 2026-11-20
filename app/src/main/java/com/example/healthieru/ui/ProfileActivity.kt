package com.example.healthieru.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.healthieru.R
import com.example.healthieru.model.AppData

class ProfileActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        enableBack()

        val patient = AppData.currentUser ?: return finish()

        val etName = findViewById<EditText>(R.id.etName)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etAge = findViewById<EditText>(R.id.etAge)
        val etGoal = findViewById<EditText>(R.id.etGoal)
        val tvError = findViewById<TextView>(R.id.tvError)

        // READ
        findViewById<TextView>(R.id.tvEmail).text = patient.email
        etName.setText(patient.name)
        etPhone.setText(patient.phone)
        etAge.setText(patient.age.toString())
        etGoal.setText(patient.healthGoal)

        // UPDATE
        findViewById<Button>(R.id.btnSave).setOnClickListener {
            val age = etAge.text.toString().toIntOrNull()
            if (age == null) {
                tvError.text = "Enter a valid age."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            val result = patient.updateProfile(
                etName.text.toString(), etPhone.text.toString(), age, etGoal.text.toString()
            )
            if (result.isFailure) {
                tvError.text = result.exceptionOrNull()?.message
                tvError.visibility = View.VISIBLE
            } else {
                tvError.visibility = View.GONE
                Toast.makeText(this, "Profile updated.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // DELETE
        findViewById<Button>(R.id.btnDelete).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete your account?")
                .setMessage("Your profile, health history and advice from your health professional will be permanently removed.")
                .setPositiveButton("Delete") { _, _ ->
                    AppData.deletePatient(patient)
                    Toast.makeText(this, "Account deleted.", Toast.LENGTH_SHORT).show()
                    startActivity(
                        Intent(this, LoginActivity::class.java).addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        )
                    )
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}