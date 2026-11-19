package com.example.healthieru.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.example.healthieru.databinding.ActivityRegisterBinding
import com.example.healthieru.model.AppData
import com.example.healthieru.model.Patient

class RegisterActivity : BaseActivity() {
    private lateinit var b: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(b.root)
        enableBack()

        b.btnCreate.setOnClickListener {
            val name = b.etName.text.toString().trim()
            val email = b.etEmail.text.toString().trim()
            val phone = b.etPhone.text.toString().trim()
            val age = b.etAge.text.toString().toIntOrNull()
            val goal = b.etGoal.text.toString().trim()
            val password = b.etPassword.text.toString()

            val error = when {
                name.isEmpty() || email.isEmpty() || phone.isEmpty() || goal.isEmpty() ->
                    "Fill in your name, email, phone number and health goal."
                age == null || age !in 1..120 -> "Enter an age between 1 and 120."
                password.length < 4 -> "Use a password with at least 4 characters."
                AppData.patients.any { it.email.equals(email, true) } -> "An account with this email already exists."
                else -> null
            }

            if (error != null) {
                b.tvError.text = error
                b.tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            AppData.patients.add(Patient(name, email, phone, password, age!!, goal))
            Toast.makeText(this, "Account created. Sign in to continue.", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}