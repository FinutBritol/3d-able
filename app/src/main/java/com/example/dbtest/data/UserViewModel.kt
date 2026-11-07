package com.example.dbtest.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UserRepository()

    private val _allUsers = MutableLiveData<List<User>>(emptyList())
    val allUsers: LiveData<List<User>> = _allUsers

    private val listener: ValueEventListener =
        repository.observeAllUsers { users -> _allUsers.postValue(users) }

    fun addUser(user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addUser(user)
        }
    }

    fun updateUser(user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateUser(user)
        }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteUser(user)
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.removeListener(listener)
    }
}
