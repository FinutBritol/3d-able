package com.example.dbtest

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.dbtest.data.UserRepository
import kotlinx.coroutines.launch

class ProfileInfo : AppCompatActivity() {

    private val userRepository = UserRepository()
    private lateinit var tvFullName: TextView
    private lateinit var tvUsername: TextView
    private lateinit var tvPassword: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_info)

        tvFullName = findViewById(R.id.tv_full_name)
        tvUsername = findViewById(R.id.tv_username)
        tvPassword = findViewById(R.id.tv_password)

        findViewById<Button>(R.id.btn_change_details).setOnClickListener {
            startActivity(Intent(this, EditProfile::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadProfile()
    }

    private fun loadProfile() {
        val savedUsername = SessionManager.getLoggedInUsername(this) ?: return

        lifecycleScope.launch {
            val user = userRepository.getUserByUsername(savedUsername)
            if (user != null) {
                tvFullName.text = user.fullName
                tvUsername.text = user.username
                tvPassword.text = user.password
            }
        }
    }
}
