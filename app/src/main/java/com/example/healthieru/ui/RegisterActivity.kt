package com.example.healthieru.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.healthieru.R
import com.example.healthieru.data.local.User
import com.example.healthieru.model.Validator

class RegisterActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        enableBack()

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etAge = findViewById<EditText>(R.id.etAge)
        val etGoal = findViewById<EditText>(R.id.etGoal)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val tvError = findViewById<TextView>(R.id.tvError)

        fun showError(message: String) {
            tvError.text = message
            tvError.visibility = View.VISIBLE
        }

        findViewById<Button>(R.id.btnCreate).setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val age = etAge.text.toString().toIntOrNull()
            val goal = etGoal.text.toString().trim()
            val password = etPassword.text.toString()

            val error = when {
                email.isEmpty() -> "Enter your email."
                else -> Validator.checkProfile(name, phone, age, goal)
                    ?: if (password.length < 4) "Use a password with at least 4 characters." else null
            }
            if (error != null) {
                showError(error)
                return@setOnClickListener
            }

            viewModel.register(
                User(name = name, email = email, phone = phone, password = password, age = age!!, healthGoal = goal)
            ) { result ->
                if (result.isSuccess) {
                    Toast.makeText(this, "Account created. Sign in to continue.", Toast.LENGTH_LONG).show()
                    finish()
                } else showError(result.exceptionOrNull()?.message ?: "Could not create account.")
            }
        }
    }
}