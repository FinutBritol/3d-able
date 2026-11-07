package com.example.dbtest

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.dbtest.data.User
import com.example.dbtest.data.UserRepository
import kotlinx.coroutines.launch

class EditProfile : AppCompatActivity() {

    private val userRepository = UserRepository()
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private var loadedUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_profile)

        etUsername = findViewById(R.id.et_edit_username)
        etPassword = findViewById(R.id.et_edit_password)

        loadCurrentUser()

        findViewById<Button>(R.id.btn_save_profile).setOnClickListener {
            saveProfileChanges()
        }
    }

    private fun loadCurrentUser() {
        val savedUsername = SessionManager.getLoggedInUsername(this)
        if (savedUsername == null) {
            Toast.makeText(this, "No active session found, please log in again", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val user = userRepository.getUserByUsername(savedUsername)
            if (user != null) {
                loadedUser = user
                etUsername.setText(user.username)
                etPassword.setText(user.password)
            }
        }
    }

    private fun saveProfileChanges() {
        val current = loadedUser
        if (current == null) {
            Toast.makeText(this, "Couldn't find your account", Toast.LENGTH_SHORT).show()
            return
        }

        val newUsername = etUsername.text.toString().trim()
        val newPassword = etPassword.text.toString().trim()

        if (newUsername.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(this, "Username and password can't be empty", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val existingUser = userRepository.getUserByUsername(newUsername)
            if (existingUser != null && existingUser.id != current.id) {
                Toast.makeText(this@EditProfile, "That username is already taken", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val updatedUser = current.copy(username = newUsername, password = newPassword)
            userRepository.updateUser(updatedUser)

            SessionManager.saveSession(this@EditProfile, newUsername)

            Toast.makeText(this@EditProfile, "Profile updated!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
