package com.example.coroutine.flow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class SharedFlowViewModel : ViewModel() {

    // 因为 StateFlow 和 LiveData 一样是粘性的
    // 如果要做非粘性需求，可以使用 SharedFlow
    private val _loginFlow = MutableSharedFlow<String>()

    val loginFlow = _loginFlow.asSharedFlow()

    fun startLogin() {
        viewModelScope.launch {
            _loginFlow.emit("Login Success")
        }
    }
}