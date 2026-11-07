package com.example.dbtest

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.dbtest.data.User
import com.example.dbtest.data.UserRepository
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        val tilName = findViewById<TextInputLayout>(R.id.til_name)
        val etName = findViewById<EditText>(R.id.et_name)
        val tilEmail = findViewById<TextInputLayout>(R.id.til_email)
        val etEmail = findViewById<EditText>(R.id.et_email)
        val tilUsername = findViewById<TextInputLayout>(R.id.til_username)
        val etUsername = findViewById<EditText>(R.id.et_username)
        val tilPassword = findViewById<TextInputLayout>(R.id.til_password)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val cbTerms = findViewById<CheckBox>(R.id.cb_terms)
        val btnSignup = findViewById<Button>(R.id.btn_signup)
        val tvLogin = findViewById<TextView>(R.id.tv_login)

        tvLogin.setOnClickListener { finish() }

        cbTerms.setOnCheckedChangeListener { _, isChecked ->
            btnSignup.isEnabled = isChecked
        }
        btnSignup.isEnabled = false

        etName.doOnTextChanged { _, _, _, _ -> tilName.error = null }
        etEmail.doOnTextChanged { _, _, _, _ -> tilEmail.error = null }
        etUsername.doOnTextChanged { _, _, _, _ -> tilUsername.error = null }
        etPassword.doOnTextChanged { _, _, _, _ -> tilPassword.error = null }

        btnSignup.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (name.isEmpty()) { tilName.error = "Full Name is required"; return@setOnClickListener }
            if (email.isEmpty()) {
                tilEmail.error = "Gmail address is required"; return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.error = "Enter a valid email address"; return@setOnClickListener
            }
            if (!email.endsWith("@gmail.com", ignoreCase = true)) {
                tilEmail.error = "Must be a Gmail address (@gmail.com)"; return@setOnClickListener
            }
            if (username.isEmpty()) { tilUsername.error = "Username is required"; return@setOnClickListener }
            if (password.isEmpty()) { tilPassword.error = "Password is required"; return@setOnClickListener }
            if (password.length < 6) { tilPassword.error = "Minimum 6 characters"; return@setOnClickListener }

            btnSignup.isEnabled = false

            lifecycleScope.launch {
                // The email becomes the record's own key, so an existing node at that
                // key IS the duplicate check - no separate query needed.
                val existingByEmail = userRepository.getUserByEmail(email)
                if (existingByEmail != null) {
                    tilEmail.error = "An account with this Gmail address already exists"
                    btnSignup.isEnabled = true
                    return@launch
                }

                // Room's REPLACE-on-conflict is gone, so check for a duplicate username ourselves.
                val existingByUsername = userRepository.getUserByUsername(username)
                if (existingByUsername != null) {
                    tilUsername.error = "That username is already taken"
                    btnSignup.isEnabled = true
                    return@launch
                }

                val newUser = User(
                    id = UserRepository.emailToKey(email),
                    fullName = name,
                    username = username,
                    email = email,
                    password = password
                )
                userRepository.addUser(newUser)

                Toast.makeText(this@SignUpActivity, "Account Created!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}