package com.example.coroutine.flow

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.coroutine.R
import kotlinx.coroutines.launch

class SharedFlowActivity : AppCompatActivity() {
    private val viewModel by viewModels<SharedFlowViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener {
            viewModel.startLogin()
        }
        // 如果使用 StateFlow，点击按钮收到提示后，屏幕旋转 Activity 重新创建，因为 StateFlow 是粘性的，所以会再次收到消息
        // 要 Flow 是非粘性的，可以使用 SharedFlow
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginFlow.collect {
                    if (it.isNotBlank()) {
                        Toast.makeText(this@SharedFlowActivity, it, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}