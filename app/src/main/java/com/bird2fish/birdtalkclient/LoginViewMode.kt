package com.bird2fish.birdtalkclient

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LoginViewMode : ViewModel() {
    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    fun sendMessage(message: String) {
        _message.value = message
    }
}