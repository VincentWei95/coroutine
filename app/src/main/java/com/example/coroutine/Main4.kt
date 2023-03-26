package com.example.coroutine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * CoroutineContext
 */
object Main4 {

    @JvmStatic
    fun main(args: Array<String>) {
        coroutineExceptionHandlerExample()
    }

    private fun coroutineContextExample() = runBlocking {
//        ========================
//        Before IO Context
//        Thread:main
//        ========================
//        In IO Context
//        Thread:DefaultDispatcher-worker-1
//        ========================
//        After IO Context
//        Thread:main
//        ========================
//        BoyCoder
//        Thread:main

        // CoroutineContext 实际开发中最常见的用处就是切换线程池

        val user = getUserInfo()
        logX(user)
    }

    private fun dispatchersExample() = runBlocking(Dispatchers.Default) {
//        ========================
//        Before IO Context
//        Thread:DefaultDispatcher-worker-1
//        ========================
//        In IO Context
//        Thread:DefaultDispatcher-worker-1 // Dispatchers.Default 线程有富余，Dispatchers.IO 会复用
//        ========================
//        After IO Context
//        Thread:DefaultDispatcher-worker-1
//        ========================
//        BoyCoder
//        Thread:DefaultDispatcher-worker-1

        // Dispatchers.Main：只在 UI 编程平台才有意义，UI 绘制线程
        // Dispatchers.Unconfined：代表无所谓，当前协程可能运行在任意线程之上
        // Dispatchers.Default：CPU 密集型任务的线程池，线程池线程个数和CPU核心数一致，最小数量是2
        // Dispatchers.IO：IO 密集型任务的线程池，线程池数量 64 个。可能会复用 Dispatchers.Default 的线程

        val user = getUserInfo()
        logX(user)
    }

    private fun customDispatcherExample() {
//        ========================
//        Before IO Context
//        Thread:MySingleThread
//        ========================
//        In IO Context
//        Thread:DefaultDispatcher-worker-1 // 自定义 Dispatcher 不会复用
//        ========================
//        After IO Context
//        Thread:MySingleThread
//        ========================
//        BoyCoder
//        Thread:MySingleThread

        // 协程运行在线程之上，asCoroutineDispatcher() 创建协程 Dispatcher

        runBlocking(mySingleDispatcher) {
            val user = getUserInfo()
            logX(user)
        }
    }

    private fun coroutineScopeExample() = runBlocking {
//        ========================
//        Third start!
//        Thread:DefaultDispatcher-worker-3
//        ========================
//        First start!
//        Thread:DefaultDispatcher-worker-1
//        ========================
//        Second start!
//        Thread:DefaultDispatcher-worker-2

        // CoroutineScope、Job、Dispatcher、CoroutineExceptionHandler 协程几个重要的概念都和 CoroutineContext 有关系

        // CoroutineScope.launch() 的 CoroutineScope 只是一个简单的接口
        // 接口唯一成员就是 CoroutineContext，CoroutineScope 只是对 CoroutineContext 做了一层封装，核心功能都来自 CoroutineContext
        // CoroutineScope 的最大最用就是用于批量控制协程

        val scope = CoroutineScope(Job())
        scope.launch {
            logX("First start!")
            delay(1000)
            logX("First end!") // 不会执行
        }
        scope.launch {
            logX("Second start!")
            delay(1000)
            logX("Second end!") // 不会执行
        }
        scope.launch {
            logX("Third start!")
            delay(1000)
            logX("Third end!") // 不会执行
        }

        delay(500)
        scope.cancel()
        delay(1000)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun coroutineContextOperatorOverloadExample() = runBlocking {
//        ========================
//        true
//        Thread:MySingleThread

        // CoroutineContext 可以当成 Map 来用

        // Job() + mySingleDispatcher，运算符重载 plus()
        val scope = CoroutineScope(Job() + mySingleDispatcher)
        scope.launch(CoroutineName("MyCoroutine")) {
            logX(coroutineContext[CoroutineDispatcher] == mySingleDispatcher)
            delay(1000)
            logX("First end!")
        }
        delay(500)
        scope.cancel()
        delay(1000)
    }

    private fun coroutineExceptionHandlerExample() = runBlocking {
//        catch exception: java.lang.NullPointerException

        // 协程异常捕获 CoroutineExceptionHandler

        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            println("catch exception: $throwable")
        }
        val scope = CoroutineScope(Job() + mySingleDispatcher)
        val job = scope.launch(exceptionHandler) {
            val s: String? = null
            s!!.length // 模拟异常
        }
        job.join()
    }

    private val mySingleDispatcher = Executors.newSingleThreadExecutor {
        Thread(it, "MySingleThread").apply { isDaemon = true }
    }.asCoroutineDispatcher()

    private suspend fun getUserInfo(): String {
        logX("Before IO Context")
        withContext(Dispatchers.IO) {
            logX("In IO Context")
            delay(1000)
        }
        logX("After IO Context")
        return "BoyCoder"
    }
}