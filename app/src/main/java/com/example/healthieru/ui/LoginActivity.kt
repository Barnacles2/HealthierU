package com.example.healthieru.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.example.healthieru.R
import com.example.healthieru.model.Doctor // <-- THIS IS THE CRUCIAL IMPORT
import com.google.android.material.button.MaterialButtonToggleGroup

class LoginActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val tvError = findViewById<TextView>(R.id.tvError)
        val toggle = findViewById<MaterialButtonToggleGroup>(R.id.toggleRole)

        fun showError() {
            tvError.text = "Incorrect email or password. Check your details and try again."
            tvError.visibility = View.VISIBLE
        }

        findViewById<Button>(R.id.btnSignIn).setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (toggle.checkedButtonId == R.id.btnRoleProfessional) {
                if (Doctor.account.checkLogin(email, password)) {
                    startActivity(Intent(this, ProfessionalActivity::class.java))
                    finish()
                } else showError()
            } else {
                viewModel.login(email, password) { ok ->
                    if (ok) {
                        startActivity(Intent(this, DashboardActivity::class.java))
                        finish()
                    } else showError()
                }
            }
        }

        findViewById<Button>(R.id.btnCreateAccount).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}