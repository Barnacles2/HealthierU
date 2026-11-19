package com.example.healthieru.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.healthieru.R
import com.example.healthieru.databinding.ActivityLoginBinding
import com.example.healthieru.model.AppData

class LoginActivity : AppCompatActivity() {
    private lateinit var b: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnSignIn.setOnClickListener {
            val email = b.etEmail.text.toString()
            val password = b.etPassword.text.toString()
            val isProfessional = b.toggleRole.checkedButtonId == R.id.btnRoleProfessional

            if (isProfessional) {
                if (AppData.professional.checkLogin(email, password)) {
                    startActivity(Intent(this, ProfessionalActivity::class.java))
                    finish()
                } else showError()
            } else {
                val patient = AppData.patients.find { it.checkLogin(email, password) }
                if (patient != null) {
                    AppData.currentUser = patient
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                } else showError()
            }
        }

        b.btnCreateAccount.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun showError() {
        b.tvError.text = "Incorrect email or password. Check your details and try again."
        b.tvError.visibility = View.VISIBLE
    }
}