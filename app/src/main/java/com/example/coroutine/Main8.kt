package com.example.coroutine

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 协程的并发
 *
 * 协程并发有4种手段：
 * 1、单线程并发：kotlin 多个协程是可以运行在单个线程
 * 2、Mutex：kotlin 官方提供的协程同步锁方案，挂起和恢复
 * 3、Actor：基于 Channel 的简单封装，并发同步也源自 Channel
 * 4、函数式思维：多个协程之间不共享可变状态变量，就不需要处理同步问题
 */
object Main8 {

    @JvmStatic
    fun main(args: Array<String>) {
        asyncConcurrentExample2()
    }

    private fun synchronizedExample() = runBlocking {
        val lock = Any()
        suspend fun coroutineMethod() {
        }

        // 使用 Java 同步的方案内部调用协程挂起函数编译器会报错
        // 原因是挂起函数本质上是 callback，在同步代码是失效的，synchronized 不支持挂起函数
//        synchronized(lock) {
//            coroutineMethod()
//        }
//        等同于
//        synchronized(lock) {
//            coroutineMethod() {
//                fun resumeWith(Continuation continuation) {
//                    // 回调结果不会收同步代码块影响
//                }
//            }
//        }
    }

    private fun mutexExample() = runBlocking {
//        i = 10000

        // 在协程使用 Java 的锁方案会影响协程的非阻塞式特性，因为 Java 的锁方案是阻塞式的
        // 不推荐在协程直接使用传统的同步锁方案，应该使用 kotlin 提供的非阻塞式锁 Mutex
        // Mutex 是支持挂起和恢复的，因为 lock 是 suspend 挂起函数，这也是实现非阻塞式同步锁的根本原因

        val mutex = Mutex()

        var i = 0
        val jobs = mutableListOf<Job>()

        repeat(10) {
            val job = launch(Dispatchers.Default) {
                repeat(1000) {
                    // 这样会有问题：mutex.unlock() 之前如果出现异常，unlock() 就不会被调用
                    // 要使用 mutex.withLock{}
//                    mutex.lock()
//                    i++
//                    mutex.unlock()

                    mutex.withLock {
                        i++
                    }

                    // mutex.withLock{} 本质上等同于:
                    // mutex.lock()
                    // try {
                    //   i++
                    // } finally {
                    //  mutex.unlock()
                    // }
                }
            }
            jobs.add(job)
        }

        // 等待所有 job 执行完成
        jobs.joinAll()

        println("i = $i")
    }

    private fun actorExample() = runBlocking {
//        i = 10000

        // actor 本质上是基于 channel 管道消息实现的，它是 Channel 的简单封装
        // actor 的多线程同步能力都源自于 Channel

        suspend fun addActor() = actor<Msg> {
            var counter = 0
            for (msg in channel) {
                when (msg) {
                    is AddMsg -> counter++ // 如果是 AddMsg 类型做累加操作
                    is ResultMsg -> msg.result.complete(counter) // 如果是 ResultMsg 类型输出最终累加结果
                }
            }
        }

        val actor = addActor()
        val jobs = mutableListOf<Job>()

        repeat(10) {
            val job = launch {
                repeat(1000) {
                    actor.send(AddMsg)
                }
            }
            jobs.add(job)
        }

        jobs.joinAll()

        val deferred = CompletableDeferred<Int>()
        actor.send(ResultMsg(deferred))

        val result = deferred.await()
        actor.close()

        println("i = $result")
    }

    private fun asyncConcurrentExample() = runBlocking {
//        i = 10000

        // 多线程并发往往会有共享的可变状态，所以才需要考虑同步问题
        // 那如果不用共享可变状态，其实就不需要考虑同步的问题了

        val deferreds = mutableListOf<Deferred<Int>>()

        repeat(10) {
            val deferred = async {
                var i = 0 // 不再共享可变状态，每个协程自己一个局部变量各自计算
                repeat(1000) {
                    i++
                }
                return@async i
            }
            deferreds.add(deferred)
        }

        var result = 0
        deferreds.forEach {
            result += it.await()
        }
        println("i = $result")
    }

    private fun asyncConcurrentExample2() = runBlocking {
        // 函数式编程实现 asyncConcurrentExample()，函数式编程不变性、无副作用

        val result = (1..10).map {
            async {
                var i = 0
                repeat(1000) {
                    i++
                }
                return@async i
            }
        }.awaitAll().sum()

        println("i = $result")
    }
}

sealed class Msg
object AddMsg : Msg()

class ResultMsg(val result: CompletableDeferred<Int>) : Msg()