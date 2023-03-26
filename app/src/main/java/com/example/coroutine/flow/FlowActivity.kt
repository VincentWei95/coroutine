package com.example.coroutine.flow

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.coroutine.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FlowActivity : AppCompatActivity() {
    private val viewModel by viewModels<FlowViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textView = findViewById<TextView>(R.id.text_view)
        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener {
            lifecycleScope.launch {
                // collectLatest 解决背压问题（背压：发送消息快，接收消息慢，导致管道阻塞），只接收最新的数据
                // 如果有新数据到来而前一个数据还没有处理完，会将前一个数据剩余的处理逻辑全部取消
                viewModel.timeFlow.collectLatest { time ->
                    textView.text = time.toString()
                    delay(3000)
                }
            }
        }

        button.setOnClickListener {
            // 调用了 collect 相当于进入一个死循环，collect 后续的代码是不会执行的
            // TODO 问题：没有和 Activity 生命周期同步，app 退到后台还是会继续接收到 flow 发送的消息
            lifecycleScope.launch {
                viewModel.timeFlow.collect { time ->
                    textView.text = time.toString()
                    Log.v("@@@", "update time $time in UI")
                }
            }
        }

        button.setOnClickListener {
            // launchWhenStarted 解决了在 Activity onStart() 时才继续更新 UI 的问题
            // TODO 问题：flow 的数据并没有取消中止，还是保留了过期数据浪费内存，app 从后台切回前台，还是会接着之前的数据更新 UI
            lifecycleScope.launchWhenStarted {
                viewModel.timeFlow.collect { time ->
                    textView.text = time.toString()
                    Log.v("@@@", "update time $time in UI")
                }
            }
        }

        button.setOnClickListener {
            // repeatOnLifecycle(Lifecycle.State.STARTED) 只接收在 Activity onStart() 时协程代码才会执行
            // 并且在进入后台之后就完全停止，不会保留任何数据；app 回到前台，flow 又重新启动开始工作
            // TODO 问题：屏幕旋转时 Activity 重建，flow 会重新 collect 重新开始
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.timeFlow.collect { time ->
                        textView.text = time.toString()
                        Log.v("@@@", "update time $time in UI")
                    }
                }
            }
        }

        button.setOnClickListener {
            // 通过 flow.stateIn() 转换为 StateFlow
            // 通过超时机制判断，如果屏幕旋转重新创建没有超过设定的时间，flow 可以继续工作
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.timeFlow.collect { time ->
                        textView.text = time.toString()
                        Log.v("@@@", "update time $time in UI")
                    }
                }
            }
        }
    }
}