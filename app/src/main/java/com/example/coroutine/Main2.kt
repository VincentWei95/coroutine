package com.example.coroutine

import kotlinx.coroutines.*

/**
 * 挂起函数
 */
object Main2 {

    @JvmStatic
    fun main(args: Array<String>) {
        suspendPrinciple()
    }

    private fun suspendPrinciple() = runBlocking {
//        userInfo = BoyCoder
//        friendList = Tom, Jack
//        feedList = {FeedList...}

        // 协程比起线程的优势：
        // 1、它有"挂起恢复"的能力
        // 2、结构化并发（带有结构和层级的并发，比如父协程取消子协程也会取消，子协程都执行完父协程才算执行完）
        // 3、以同步的代码写法完成子线程和主线程的操作

        // suspend 声明的函数是挂起函数，既然是挂起，自然就能被恢复
        // = 左边的代码运行在主线程，= 右边的代码运行在 IO 线程，一句代码做了线程切换
        // 每次从主线程到 IO 线程，都是一次协程挂起
        // 每次从 IO 线程到主线程，都是一次协程恢复

        // 挂起函数的本质就是 callback（Continuation，词源 continue），只是 kotlin 编译器会将 suspend 关键字的函数转换成带有 callback 的函数
        // 将 suspend 挂起函数转换为 callback 的过程，叫做 CPS 转换（Continuation-Passing_Style-Transformation）
        // 挂起函数反编译为 Java：
        // public static final Object getUserInfo(Continuation $completion) {}

        // 协程和挂起函数的关系（为什么挂起函数要在协程才能调用）：
        // 挂起和恢复，是协程的一种底层能力
        // 挂起函数，是这种底层能力的一种表现形式。通过暴露 suspend 关键字，开发者可以在上层非常方便的使用这种底层能力
        // 所以挂起函数才有挂起和恢复的能力

        // 总结：
        // 协程之所以非阻塞，是因为它支持挂起和恢复
        // 而挂起和恢复的能力，源自于挂起函数
        // 挂起函数是由 CPS 实现的，其中的 Continuation 本质上就是 callback

        val userInfo = getUserInfo()
        println("userInfo = $userInfo")
        val friendList = getFriendList(userInfo)
        println("friendList = $friendList")
        val feedList = getFeedList(friendList)
        println("feedList = $feedList")
    }

    private suspend fun getUserInfo(): String {
        withContext(Dispatchers.IO) {
            delay(1000L)
        }
        return "BoyCoder"
    }

    private suspend fun getFriendList(user: String): String {
        withContext(Dispatchers.IO) {
            delay(1000L)
        }
        return "Tom, Jack"
    }

    private suspend fun getFeedList(list: String): String {
        withContext(Dispatchers.IO) {
            delay(1000L)
        }
        return "{FeedList...}"
    }
}