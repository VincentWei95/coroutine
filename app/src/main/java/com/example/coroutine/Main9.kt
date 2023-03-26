package com.example.coroutine

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.ArithmeticException
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors

/**
 * 协程的异常处理
 *
 * 在 kotlin 协程把异常分为两大类：取消异常（CancellationException）和其他异常
 *
 * 协程异常处理准则：
 * 1、协程的取消需要内部状态的配合
 * 2、不要轻易打破协程的父子结构
 * 3、捕获了 CancellationException 以后，要考虑是否应该重新抛出来
 * 4、不要用 try-catch 直接包裹 launch、async。要捕获协程内的异常，try-catch 要写在协程内
 * 5、灵活使用 SupervisorJob，控制异常传播的范围
 * 6、使用 CoroutineExceptionHandler 处理复杂结构的协程异常，它仅在顶层协程中起作用
 *
 * 因为协程是 "结构化的"，所以异常传播也是 "结构化的"
 */
object Main9 {

    @JvmStatic
    fun main(args: Array<String>) {
        supervisorJobExample()
    }

    private fun coroutineCancelExample1() = runBlocking {
//        i = 1
//        i = 2
//        i = 3
//        i = 4
//        cancel job start
//        cancel job end
//        i = 5
//        End

        // 准则1：协程的取消需要内部状态的配合

        val job = launch(Dispatchers.Default) {
            var i = 0
            // 无限循环 while(true) 调用 cancel() 还是会继续运行
            // 协程的执行要内部状态协助判断
            while (isActive) {
                // 这里没有用 delay() 是因为它内部有检测协程是否被取消会抛出异常停止协程
                // 但这里为了掩饰取消异常的准则，使用 sleep()
                Thread.sleep(500)
                i++
                println("i = $i")
            }
        }

        delay(2000)

        println("cancel job start")
        job.cancel()
        println("cancel job end")
        job.join()

        println("End")
    }

    private fun coroutineCancelExample2() = runBlocking {
//        First i = 1
//        Second i = 1
//        First i = 2
//        Second i = 2
//        First i = 3
//        Second i = 3
//        cancel job start
//        cancel job end
//        First i = 4
//        Second i = 4
//        End

        // 准则2：不要轻易打破协程的父子结构

        val parentJob = launch(fixedDispatcher) {
            // 如果改成 launch(Job())，此时该子协程已经不是 parentJob 的子协程
            // 父协程取消不会影响该子协程运行
            launch {
                var i = 0
                while (isActive) {
                    // 这里没有用 delay() 是因为它内部有检测协程是否被取消会抛出异常停止协程
                    // 但这里为了掩饰取消异常的准则，使用 sleep()
                    Thread.sleep(500)
                    i++
                    println("First i = $i")
                }
            }

            launch {
                var i = 0
                while (isActive) {
                    Thread.sleep(500)
                    i++
                    println("Second i = $i")
                }
            }
        }

        delay(2000)

        println("cancel job start")
        parentJob.cancel()
        println("cancel job end")
        parentJob.join()

        println("End")
    }

    private fun coroutineCancelExample3() = runBlocking {
//        First i = 1
//        Second i = 1
//        First i = 2
//        Second i = 2
//        First i = 3
//        Second i = 3
//        cancel job start
//        cancel job end
//        catch CancellationException
//        End

        // 准则3：捕获了 CancellationException 以后，要考虑是否应该重新抛出来

        val parentJob = launch(Dispatchers.Default) {
            launch {
                var i = 0
                while (true) {
                    try {
                        // 验证 delay() 内部会检测协程取消
                        delay(500)
                    } catch (e: CancellationException) {
                        println("catch CancellationException")
                        throw e // 要将异常抛出去，否则协程是不会停止的
                    }
                    i++
                    println("First i = $i")
                }
            }
            launch {
                var i = 0
                while (true) {
                    delay(500)
                    i++
                    println("Second i = $i")
                }
            }
        }

        delay(2000)

        println("cancel job start")
        parentJob.cancel()
        println("cancel job end")
        parentJob.join()

        println("End")
    }

    private fun tryCatchInvalidExample() = runBlocking {
//        Exception in thread "main" ArithmeticException: / by zero

        // 启动协程时，协程内部的代码已经不在 try-catch 作用域内

        try {
            launch {
                delay(100)
                1 / 0 // 制造异常
            }
        } catch (e: ArithmeticException) {
            println("Catch: $e")
        }

        delay(500)
        println("End")

//        var deferred: Deferred<Unit>? = null
//        try {
//            deferred = async {
//                delay(100)
//                1 / 0
//            }
//        } catch (e: ArithmeticException) {
//            println("catch: $e")
//        }
//
//        deferred?.await()
//
//        delay(500)
//        println("End")
    }

    private fun tryCatchValidExample() = runBlocking {

        // 准则4：不要用 try-catch 直接包裹 launch、async。要捕获协程内的异常，try-catch 要写在协程内

        launch {
            try {
                delay(100)
                1 / 0
            } catch (e: ArithmeticException) {
                println("catch: $e")
            }
        }
        delay(500)
        println("End")
    }

    private fun supervisorJobExample() = runBlocking {
//        catch: java.lang.ArithmeticException: / by zero
//        End

        // 准则5：灵活使用 SupervisorJob，控制异常传播的范围

        // SupervisorJob 只是一个普通的顶层函数，它返回 Job 的子类
        // SupervisorJob 和 Job 的最大区别是，当它的子 Job 发生异常时，其他子 Job 不会收到牵连能继续运行
        // 正常情况下，如果子协程 Job 发生异常，会导致父协程 parentJob 取消，进而父协程其他的子协程 Job 也会取消

        val scope = CoroutineScope(SupervisorJob())
        val deferred = scope.async {
            delay(100)
            1 / 0
        }

        try {
            deferred.await()
        } catch (e: ArithmeticException) {
            println("catch: $e")
        }

        delay(500)
        println("End")
    }

    private fun coroutineExceptionHandlerExample() = runBlocking {

        // 准则6：使用 CoroutineExceptionHandler 处理复杂结构的协程异常，它仅在顶层协程中起作用

        // CoroutineExceptionHandler 捕获协程内的所有异常
        // 但需要注意的是，必须定义在顶层。因为当子协程出现异常时，会统一上报给顶层父协程，然后父协程才会调用 CoroutineExceptionHandler 处理异常

        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            println("catch exception: $throwable")
        }

        // CoroutineExceptionHandler 定义在顶层才会生效捕获到异常
        val scope = CoroutineScope(coroutineContext + Job() + exceptionHandler)
        scope.launch {
            async {
                delay(100)
            }

            // 复杂的协程结构化场景
            launch {
                delay(100)

                // launch(exceptionHandler)，将 CoroutineExceptionHandler 定义在出现异常的子协程是不会生效无法捕获异常
                launch {
                    delay(100)
                    1 / 0 // 制造异常
                }
            }

            delay(100)
        }

        delay(1000)
        println("End")
    }

    private val fixedDispatcher = Executors.newFixedThreadPool(2) {
        Thread(it, "MyFixedThread").apply { isDaemon = true }
    }.asCoroutineDispatcher()
}