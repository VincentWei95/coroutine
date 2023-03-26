package com.example.coroutine.flow

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Timer
import java.util.TimerTask

class StateFlowViewModel : ViewModel() {

    // StateFlow 和 LiveData 的使用基本一致，和 LiveData 一样也是粘性的
    // 从 LiveData 可以零成本切换到 StateFlow
    private val _stateFlow = MutableStateFlow(0)

    val stateFlow = _stateFlow.asStateFlow()

    fun startTimer() {
        val timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                _stateFlow.value += 1
            }
        }, 0, 1000)
    }
}