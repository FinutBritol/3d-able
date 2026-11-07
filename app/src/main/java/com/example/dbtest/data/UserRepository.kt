package com.example.dbtest.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await

/**
 * Replaces UserDao + UserDatabase. Data lives under /users/{pushKey} in
 * Firebase Realtime Database instead of a local SQLite table, so every
 * device/user sees the same data.
 */
class UserRepository {

    private val usersRef = FirebaseDatabase.getInstance().getReference("users")

    companion object {
        /**
         * Firebase Realtime Database keys can't contain '.', '#', '$', '[' or ']'.
         * Emails are the one field guaranteed unique per user, so we turn each email
         * into a safe key and use THAT as the /users/{key} node itself - the database's
         * own uniqueness (you can't have two nodes with the same key) enforces "one
         * account per email" for free, no separate index or query needed.
         */
        fun emailToKey(email: String): String =
            email.trim().lowercase().replace(".", ",")
    }

    /** Direct key lookup - O(1), no query needed, since the key IS the email. */
    suspend fun getUserByEmail(email: String): User? {
        val key = emailToKey(email)
        val snapshot = usersRef.child(key).get().await()
        return snapshot.getValue(User::class.java)?.copy(id = snapshot.key ?: "")
    }

    suspend fun getUserByUsername(username: String): User? {
        val snapshot = usersRef.orderByChild("username").equalTo(username).get().await()
        val child = snapshot.children.firstOrNull() ?: return null
        return child.getValue(User::class.java)?.copy(id = child.key ?: "")
    }

    /** Insert or overwrite. If user.id is blank, a new push key is generated. */
    suspend fun addUser(user: User): String {
        val ref = if (user.id.isNotBlank()) usersRef.child(user.id) else usersRef.push()
        ref.setValue(user.copy(id = "")).await()
        return ref.key ?: user.id
    }

    suspend fun updateUser(user: User) {
        require(user.id.isNotBlank()) { "Cannot update a user with no id" }
        usersRef.child(user.id).setValue(user.copy(id = "")).await()
    }

    suspend fun deleteUser(user: User) {
        require(user.id.isNotBlank()) { "Cannot delete a user with no id" }
        usersRef.child(user.id).removeValue().await()
    }

    /**
     * Realtime replacement for the old LiveData query. Attaches a listener that
     * fires immediately with the current list and again on every change.
     * Call removeListener() (e.g. in onCleared()/onDestroy()) to stop listening.
     */
    fun observeAllUsers(onChange: (List<User>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = snapshot.children.mapNotNull { child ->
                    child.getValue(User::class.java)?.copy(id = child.key ?: "")
                }.sortedBy { it.id }
                onChange(users)
            }

            override fun onCancelled(error: DatabaseError) {
                // Surface however you prefer - Log.w, a callback, etc.
            }
        }
        usersRef.addValueEventListener(listener)
        return listener
    }

    fun removeListener(listener: ValueEventListener) {
        usersRef.removeEventListener(listener)
    }
}