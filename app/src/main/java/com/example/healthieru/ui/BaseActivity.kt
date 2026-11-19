package com.example.healthieru.ui

import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {
    protected fun enableBack() { supportActionBar?.setDisplayHomeAsUpEnabled(true) }
    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}