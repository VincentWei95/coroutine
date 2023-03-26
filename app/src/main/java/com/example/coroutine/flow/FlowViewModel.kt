package com.example.coroutine.flow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

class FlowViewModel : ViewModel() {
    val timeFlow = flow {
        var time = 0
        while (true) {
            emit(time)
            kotlinx.coroutines.delay(1000)
            time++
        }
    }

    // stateIn() 可以将其他的 flow 转换为 StateFlow
    // 通过超时机制判定是否停止 flow 工作
    val stateFlow = timeFlow.stateIn(
        viewModelScope,
        // 设置如果超过 5s，flow 就停止工作，否则可以继续工作
        SharingStarted.WhileSubscribed(5000),
        0 // 初始值
    )
}