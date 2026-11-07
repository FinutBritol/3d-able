package com.example.dbtest.fragments.add

import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.dbtest.R
import com.example.dbtest.data.User
import com.example.dbtest.data.UserRepository
import com.example.dbtest.data.UserViewModel
import androidx.navigation.fragment.findNavController

class AddFragment : Fragment() {

    private lateinit var mUserViewModel: UserViewModel
    private lateinit var rootView: View
    private var editingUserId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add, container, false)
        rootView = view

        mUserViewModel = ViewModelProvider(this).get(UserViewModel::class.java)

        val etFullName = view.findViewById<EditText>(R.id.FullName)
        val etEmail = view.findViewById<EditText>(R.id.Email)
        val etUsername = view.findViewById<EditText>(R.id.UserName)
        val etPassword = view.findViewById<EditText>(R.id.Password)
        val btnSave = view.findViewById<Button>(R.id.addbutton)

        editingUserId = arguments?.getString("userId") ?: ""
        if (editingUserId.isNotEmpty()) {
            etFullName.setText(arguments?.getString("fullName"))
            etEmail.setText(arguments?.getString("email"))
            etUsername.setText(arguments?.getString("username"))
            etPassword.setText(arguments?.getString("password"))
            btnSave.text = "Update"
        }

        btnSave.setOnClickListener {
            saveUser()
        }
        return view
    }

    private fun saveUser() {
        val fullName = rootView.findViewById<EditText>(R.id.FullName).text.toString().trim()
        val email = rootView.findViewById<EditText>(R.id.Email).text.toString().trim()
        val username = rootView.findViewById<EditText>(R.id.UserName).text.toString().trim()
        val password = rootView.findViewById<EditText>(R.id.Password).text.toString().trim()

        if (!inputCheck(username, fullName, email, password)) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_LONG).show()
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Enter a valid email address", Toast.LENGTH_LONG).show()
            return
        }

        if (editingUserId.isNotEmpty()) {
            // Editing an existing record: keep its existing key as-is, just update the fields.
            val updatedUser = User(
                id = editingUserId, fullName = fullName, username = username,
                email = email, password = password
            )
            mUserViewModel.updateUser(updatedUser)
            Toast.makeText(requireContext(), "Updated", Toast.LENGTH_LONG).show()
        } else {
            // New record: key it by email (same convention as self-signup) so there's
            // still only ever one account per Gmail address, however it was created.
            val newUser = User(
                id = UserRepository.emailToKey(email), fullName = fullName, username = username,
                email = email, password = password
            )
            mUserViewModel.addUser(newUser)
            Toast.makeText(requireContext(), "Added", Toast.LENGTH_LONG).show()
        }

        findNavController().navigate(R.id.action_addFragment_to_listFragment)
    }

    private fun inputCheck(username: String, fullName: String, email: String, password: String): Boolean {
        return !(TextUtils.isEmpty(fullName)) && !(TextUtils.isEmpty(email)) &&
                !(TextUtils.isEmpty(username)) && !(TextUtils.isEmpty(password))
    }
}