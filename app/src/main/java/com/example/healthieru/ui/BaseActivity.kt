package com.example.healthieru.ui

import android.content.Intent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.healthieru.HealthierUApp
import com.example.healthieru.viewmodel.HealthViewModel
import com.example.healthieru.viewmodel.HealthViewModelFactory

open class BaseActivity : AppCompatActivity() {

    protected val viewModel: HealthViewModel by viewModels {
        val app = application as HealthierUApp
        HealthViewModelFactory(app.repository, app.session)
    }

    protected fun enableBack() { supportActionBar?.setDisplayHomeAsUpEnabled(true) }

    protected fun goToLogin() {
        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}