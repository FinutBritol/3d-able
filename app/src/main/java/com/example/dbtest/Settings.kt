package com.example.dbtest

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class Settings : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)

        val btnProfileInfo = findViewById<Button>(R.id.btn_profile_info)
        val btnLogout = findViewById<Button>(R.id.btn_logout)

        btnProfileInfo.setOnClickListener {
            startActivity(Intent(this, ProfileInfo::class.java))
        }

        btnLogout.setOnClickListener { confirmLogout() }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Log Out") { _, _ -> logout() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logout() {
        SessionManager.clearSession(this)
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}