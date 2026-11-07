package com.example.dbtest

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.dbtest.data.UserRepository
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If a session is already saved, skip the login form entirely and go straight to
        // where that user belongs. This is what makes login survive the app being fully
        // closed (back button, swiped from Recents, killed for memory) - only an explicit
        // Logout (Settings.kt, which already clears the session) should ever land here.
        val savedUsername = SessionManager.getLoggedInUsername(this)
        if (savedUsername != null) {
            lifecycleScope.launch {
                val user = userRepository.getUserByUsername(savedUsername)
                if (user != null) {
                    val target = if (user.admin) MainActivity::class.java else CampusMapActivity::class.java
                    startActivity(Intent(this@LoginActivity, target))
                    finish()
                } else {
                    // Saved session points at a user that no longer exists (e.g. deleted
                    // from Firebase) - clear the stale session and fall back to a real login.
                    SessionManager.clearSession(this@LoginActivity)
                    setUpLoginScreen()
                }
            }
            return
        }

        setUpLoginScreen()
    }

    private fun setUpLoginScreen() {
        setContentView(R.layout.activity_login)

        val tilUsername = findViewById<TextInputLayout>(R.id.til_username)
        val etUsername = findViewById<EditText>(R.id.et_username)
        val tilPassword = findViewById<TextInputLayout>(R.id.til_password)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val btnLogin = findViewById<Button>(R.id.btn_login)
        val tvSignup = findViewById<TextView>(R.id.tv_signup)

        tvSignup.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        etUsername.doOnTextChanged { _, _, _, _ -> tilUsername.error = null }
        etPassword.doOnTextChanged { _, _, _, _ -> tilPassword.error = null }

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty()) {
                tilUsername.error = "Username is required"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                tilPassword.error = "Password is required"
                return@setOnClickListener
            }

            btnLogin.isEnabled = false

            lifecycleScope.launch {
                val user = userRepository.getUserByUsername(username)
                btnLogin.isEnabled = true

                if (user != null && user.password == password) {
                    Toast.makeText(this@LoginActivity, "Login Successful!", Toast.LENGTH_SHORT).show()

                    SessionManager.saveSession(this@LoginActivity, user.username)

                    if (user.admin) {
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    } else {
                        startActivity(Intent(this@LoginActivity, CampusMapActivity::class.java))
                    }
                    finish()
                } else {
                    if (user == null) {
                        tilUsername.error = "Username not found"
                    } else {
                        tilPassword.error = "Incorrect password"
                    }
                }
            }
        }
    }
}