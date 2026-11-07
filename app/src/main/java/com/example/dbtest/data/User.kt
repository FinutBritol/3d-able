package com.example.dbtest.data

// No Room annotations. "id" is the Firebase Realtime Database key (a push ID),
// not an auto-increment Long anymore. The no-arg-friendly defaults are required
// so Firebase's automatic getValue(User::class.java) deserialization works.
data class User(
    var id: String = "",
    val fullName: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    // Named "admin", not "isAdmin": Firebase's serializer strips the "is" prefix from
    // boolean getters when writing JSON, so a field called isAdmin gets saved as "admin".
    // Naming it "admin" here keeps write and read consistent with what's actually in the DB.
    val admin: Boolean = false
)