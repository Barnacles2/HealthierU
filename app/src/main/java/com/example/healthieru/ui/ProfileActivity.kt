package com.example.healthieru.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.healthieru.R
import com.example.healthieru.model.Validator
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProfileActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        enableBack()

        val etName = findViewById<EditText>(R.id.etName)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etAge = findViewById<EditText>(R.id.etAge)
        val etGoal = findViewById<EditText>(R.id.etGoal)
        val tvError = findViewById<TextView>(R.id.tvError)

        // READ (fills the form once, when the profile has loaded)
        lifecycleScope.launch {
            val user = viewModel.currentUser.filterNotNull().first()
            findViewById<TextView>(R.id.tvEmail).text = user.email
            etName.setText(user.name)
            etPhone.setText(user.phone)
            etAge.setText(user.age.toString())
            etGoal.setText(user.healthGoal)
        }

        // UPDATE
        findViewById<Button>(R.id.btnSave).setOnClickListener {
            val age = etAge.text.toString().toIntOrNull()
            val error = Validator.checkProfile(
                etName.text.toString(), etPhone.text.toString(), age, etGoal.text.toString()
            )
            if (error != null) {
                tvError.text = error
                tvError.visibility = View.VISIBLE
            } else {
                viewModel.updateProfile(
                    etName.text.toString(), etPhone.text.toString(), age!!, etGoal.text.toString()
                )
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
                    viewModel.deleteAccount {
                        Toast.makeText(this, "Account deleted.", Toast.LENGTH_SHORT).show()
                        goToLogin()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}