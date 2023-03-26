package com.example.coroutine

import kotlinx.coroutines.*
import kotlin.random.Random
import kotlin.system.measureTimeMillis

/**
 * Job
 */
object Main3 {

    @JvmStatic
    fun main(args: Array<String>) {
        concurrencyWithAsyncExample()
    }

    private fun jobExample() = runBlocking {
//        ========================
//        isActive = true
//        isCancelled = false
//        isCompleted = false
//        Thread:main @coroutine#1
//        ========================
//        isActive = false // 调用了 job.cancel() 后为 false
//        isCancelled = true // 调用了 job.cancel() 后为 true
//        isCompleted = false
//        Thread:main @coroutine#1

        // launch {} 和 async{} 都会返回一个 job
        // job 可以用来监控协程的运行状态和操控协程

        // isActive：协程是否处于活跃状态，启动或懒加载时调用 job.start() 时为 true
        // isCancelled：协程任务是否被取消，调用 job.cancel() 时为 true
        // isCompleted：协程是否执行完成，调用 job.cancel() 或正常执行完成时为 true

        val job = launch {
            delay(1000L)
        }
        job.log()
        job.cancel() // 取消协程
        job.log()
        delay(1500L)
    }

    private fun jobLazyExample() = runBlocking {
//        ========================
//        isActive = false // CoroutineStart.LAZY，所以 job 没有启动
//        isCancelled = false
//        isCompleted = false
//        Thread:main @coroutine#1
//        ========================
//        isActive = true // 调用了 job.start()，协程才启动
//        isCancelled = false
//        isCompleted = false
//        Thread:main @coroutine#1
//        ========================
//        Coroutine start!
//        Thread:main @coroutine#2
//        ========================
//        isActive = false // 调用了 job.cancel()
//        isCancelled = true
//        isCompleted = true // 某种原因取消的协程也算结束
//        Thread:main @coroutine#1
//        ========================
//        Process end!
//        Thread:main @coroutine#1

        val job = launch(start = CoroutineStart.LAZY) {
            logX("Coroutine start!")
            delay(1000L)
        }
        delay(500L)
        job.log()
        job.start() // 调用时才启动协程
        job.log()
        delay(500L)
        job.cancel()
        delay(500L)
        job.log()
        delay(2000L)
        logX("Process end!")
    }

    private fun jobInvokeOnCompletionExample() = runBlocking {
//        ========================
//        isActive = false
//        isCancelled = false
//        isCompleted = false
//        Thread:main @coroutine#1
//        ========================
//        isActive = true
//        isCancelled = false
//        isCompleted = false
//        Thread:main @coroutine#1
//        ========================
//        Coroutine start!
//        Thread:main @coroutine#2
//        ========================
//        Delay time: 578
//        Thread:main @coroutine#2
//        ========================
//        Coroutine end!
//        Thread:main @coroutine#2
//        ========================
//        isActive = false
//        isCancelled = false
//        isCompleted = true
//        Thread:main @coroutine#2
//        ========================
//        Process end!
//        Thread:main @coroutine#1

        suspend fun download() {
            val time = (Random.nextDouble() * 1000).toLong()
            logX("Delay time: $time")
            delay(time)
        }
        val job = launch(start = CoroutineStart.LAZY) {
            logX("Coroutine start!")
            download()
            logX("Coroutine end!")
        }
        delay(500L)
        job.log()
        job.start()
        job.log()
        job.invokeOnCompletion {
            job.log() // 协程结束后调用，job.cancel() 协程被取消也会回调
        }
        job.join() // 挂起函数，挂起等待协程执行完毕
        logX("Process end!")
    }

    private fun deferredExample() = runBlocking {
//        ========================
//        Coroutine start!
//        Thread:main @coroutine#2
//        ========================
//        Coroutine end!
//        Thread:main @coroutine#2
//        result: Coroutine result!
//        ========================
//        Process end!
//        Thread:main @coroutine#1

        // Deferred 继承自 Job

        val deferred = async {
            logX("Coroutine start!")
            delay(1000L)
            logX("Coroutine end!")
            "Coroutine result!"
        }
        val result = deferred.await() // 只是执行流程被挂起等待了，看起来像阻塞实际并不是
        println("result: $result")
        logX("Process end!")
    }

    private fun concurrencyExample1() = runBlocking {
//        job1 === job is true
//        job2 === job is true
//        job3 === job is true
//        ========================
//        Process end!
//        Thread:main @coroutine#1

        // 结构化并发
        // 协程是存在父子关系的
        // 子协程都执行完成，父协程才算执行完成

        val parentJob: Job
        var job1: Job? = null
        var job2: Job? = null
        var job3: Job? = null

        parentJob = launch {
            job1 = launch {
                delay(1000L)
            }
            job2 = launch {
                delay(3000L)
            }
            job3 = launch {
                delay(5000L)
            }
        }
        delay(500L)

        parentJob.children.forEachIndexed { index, job ->
            when (index) {
                0 -> println("job1 === job is ${job1 === job}")
                1 -> println("job2 === job is ${job2 === job}")
                2 -> println("job3 === job is ${job3 === job}")
            }
        }

        parentJob.join() // 会等待所有子 job 执行完后才往下执行
        logX("Process end!")
    }

    private fun concurrencyExample2() = runBlocking {
//        ========================
//        Job1 start!
//        Thread:main @coroutine#3
//        ========================
//        Job2 start!
//        Thread:main @coroutine#4
//        ========================
//        Job3 start!
//        Thread:main @coroutine#5
//        job1 === job is true
//        job2 === job is true
//        job3 === job is true
//        ========================
//        Process end!
//        Thread:main @coroutine#1

        val parentJob: Job
        var job1: Job? = null
        var job2: Job? = null
        var job3: Job? = null

        parentJob = launch {
            job1 = launch {
                logX("Job1 start!")
                delay(1000L)
                logX("Job1 done!") // 不会执行
            }
            job2 = launch {
                logX("Job2 start!")
                delay(3000L)
                logX("Job2 done!") // 不会执行
            }
            job3 = launch {
                logX("Job3 start!")
                delay(5000L)
                logX("Job3 end!") // 不会执行
            }
        }
        delay(500L)

        parentJob.children.forEachIndexed { index, job ->
            when (index) {
                0 -> println("job1 === job is ${job1 === job}")
                1 -> println("job2 === job is ${job2 === job}")
                2 -> println("job3 === job is ${job3 === job}")
            }
        }

        parentJob.cancel() // parentJob 取消了，子 Job 后续的操作都不会执行
        logX("Process end!")
    }

    private fun concurrencyWithAsyncExample() = runBlocking {
//        Time: 3014
//        [Result1, Result2, Result3]

        suspend fun getResult1(): String {
            delay(1000) // 模拟耗时操作
            return "Result1"
        }
        suspend fun getResult2(): String {
            delay(1000) // 模拟耗时操作
            return "Result2"
        }
        suspend fun getResult3(): String {
            delay(1000) // 模拟耗时操作
            return "Result3"
        }

//        val results = mutableListOf<String>()
//        val time = measureTimeMillis {
//            results.add(getResult1())
//            results.add(getResult2())
//            results.add(getResult3())
//        }

//        Time: 1093
//        [Result1, Result2, Result3]

        // async 一般用于与挂起函数结合优化并发
        val results: List<String>
        val time = measureTimeMillis {
            val deferred1 = async { getResult1() }
            val deferred2 = async { getResult2() }
            val deferred3 = async { getResult3() }

            results = listOf(deferred1.await(), deferred2.await(), deferred3.await())
        }

        println("Time: $time")
        println(results)
    }
}

fun Job.log() {
    logX("""
        isActive = $isActive
        isCancelled = $isCancelled
        isCompleted = $isCompleted
        """.trimIndent())
}