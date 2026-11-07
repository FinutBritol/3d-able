package com.example.dbtest.fragments.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dbtest.R
import com.example.dbtest.data.User

class UserAdapter(
    private val onEditClick: (User) -> Unit,
    private val onDeleteClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private var users: List<User> = emptyList()

    fun submitList(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFullName: TextView = itemView.findViewById(R.id.tvFullName)
        val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.tvFullName.text = user.fullName
        holder.tvUsername.text = user.username
        holder.btnEdit.setOnClickListener { onEditClick(user) }
        holder.btnDelete.setOnClickListener { onDeleteClick(user) }
    }

    override fun getItemCount(): Int = users.size
}