package com.example.coroutine

import kotlinx.coroutines.*
import kotlin.jvm.JvmStatic

/**
 * 启动协程
 */
object Main1 {

    @JvmStatic
    fun main(args: Array<String>) {
        asyncExample()
    }

    private fun launchExample() {
//        没有注释 Thread.sleep(2000L)输出：
//        Coroutine started!
//        Hello World!
//        Process end!

//       注释了 Thread.sleep(2000L)输出： Process end!

        // launch 可以类比为射箭，启动后就改变不了
        // launch 没有运行，类似于守护线程，主线程执行完了守护线程也会同时销毁，所以不会执行
        // GlobalScope 是不建议使用的，因为使用不当会出现内存泄漏

        GlobalScope.launch {
            println("Coroutine started!")
            delay(1000L)
            println("Hello World!")
        }
        Thread.sleep(2000L) // 加上等待 launch 就能执行，因为主线程还没执行完
        println("Process end!")
    }

    private fun runBlockingExample1() {
//        Coroutine started!
//        Hello World!
//        After launch!
//        Process end!

        // runBlocking 会阻塞所在线程
        // runBlocking 最后一个参数是 CoroutineScope，所以就不需要 GlobalScope 提供启动协程的环境
        // runBlocking 一般只会用在测试代码，不要在生产环境使用

        runBlocking {
            println("Coroutine started!")
            delay(1000L)
            println("Hello World!")
        }
        println("After launch!")
//        Thread.sleep(2000L) // 因为 runBlocking 会阻塞，所以不需要主线程等待
        println("Process end!")
    }

    private fun runBlockingExample2() {
//        result is: Coroutine done!

        val result = runBlocking {
            delay(1000L)
            return@runBlocking "Coroutine done!"
        }
        println("result is: $result")
    }

    private fun asyncExample() = runBlocking {
//        in runBlocking: main @coroutine#1
//        after async: main @coroutine#1
//        in async: main @coroutine#2
//        result is: task completed!

        // async 也会启动一个协程，从输出可以看到是有两个协程
        // 并且 async 不会阻塞当前程序的执行
        // async 可以类比为就是调鱼，抛出了钩子，执行完有鱼上钩了就能拿到结果

        println("in runBlocking: ${Thread.currentThread().name}")

        val deferred: Deferred<String> = async {
            println("in async: ${Thread.currentThread().name}")
            delay(1000L) // 模拟耗时操作
            return@async "task completed!"
        }

        println("after async: ${Thread.currentThread().name}")

        val result = deferred.await()
        println("result is: $result")
        delay(2000L)
    }
}