package com.example.healthieru.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
    }
    protected fun enableBack() { supportActionBar?.setDisplayHomeAsUpEnabled(true) }
    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}