package com.example.dbtest.fragments.list

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dbtest.LoginActivity
import com.example.dbtest.R
import com.example.dbtest.data.User
import com.example.dbtest.data.UserViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.dbtest.SessionManager //

class ListFragment : Fragment() {

    private lateinit var mUserViewModel: UserViewModel
    private lateinit var adapter: UserAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_list, container, false)

        mUserViewModel = ViewModelProvider(this).get(UserViewModel::class.java)

        val recyclerView = view.findViewById<RecyclerView>(R.id.RecylerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = UserAdapter(
            onEditClick = { user -> openEditScreen(user) },
            onDeleteClick = { user -> confirmDelete(user) }
        )
        recyclerView.adapter = adapter

        mUserViewModel.allUsers.observe(viewLifecycleOwner) { users ->
            adapter.submitList(users)
        }

        val fab = view.findViewById<FloatingActionButton>(R.id.floatingActionButton)
        fab.setOnClickListener {
            findNavController().navigate(R.id.action_listFragment_to_addFragment)
        }

        val btnBack = view.findViewById<ImageButton>(R.id.btnBack)//
        btnBack.setOnClickListener {
            SessionManager.clearSession(requireContext())

            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
        return view
    } //

    private fun openEditScreen(user: User) {
        val bundle = Bundle().apply {
            putString("userId", user.id)
            putString("fullName", user.fullName)
            putString("email", user.email)
            putString("username", user.username)
            putString("password", user.password)
        }
        findNavController().navigate(R.id.action_listFragment_to_addFragment, bundle)
    }

    private fun confirmDelete(user: User) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete user")
            .setMessage("Delete ${user.fullName}?")
            .setPositiveButton("Delete") { _, _ -> mUserViewModel.deleteUser(user) }
            .setNegativeButton("Cancel", null)
            .show()
    }
}