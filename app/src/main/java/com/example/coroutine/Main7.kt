package com.example.coroutine

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import java.util.Random

/**
 * select
 *
 * 一般会配合 async、channel 使用，它们提供给 select 的返回结果一般是 onXxx 的函数
 * 例如 async 返回的 Deferred.onAwait，channel 返回 ReceiveChannel.onReceive/onReceiveCatching
 */
object Main7 {
    private val random = Random()

    @JvmStatic
    fun main(args: Array<String>) {
        selectWithDeferredCancelAllExample()
    }

    // select 和 async 配合使用
    private fun selectWithAsyncExample() = runBlocking {
//        xxxId==9.6
//        Time cost: 151
//        xxxId==9.8
//        Time cost: 227

        suspend fun getCacheInfo1(productId: String): Product {
            delay(2000) // 模拟缓存服务出问题，比网络请求慢
            return Product(productId, 9.9)
        }

        suspend fun getCacheInfo2(productId: String): Product {
            delay(random.nextInt(500).toLong())
            return Product(productId, 9.6)
        }

        suspend fun getNetworkInfo(productId: String): Product {
            delay(random.nextInt(500).toLong())
            return Product(productId, 9.8)
        }

        fun updateUI(product: Product) {
            println("${product.productId}==${product.price}")
        }

        val startTime = System.currentTimeMillis()
        val productId = "xxxId"

        // 1. 缓存和网络，并发执行
        val cache1Deferred = async { getCacheInfo1(productId) }
        val cache2Deferred = async { getCacheInfo2(productId) }
        val networkDeferred = async { getNetworkInfo(productId) }

        // select：哪个返回速度快就选谁
        // 2.在缓存和网络中间，选择最快的结果
        val product = select<Product> {
            // onAwait{} 将执行结果传给 select{} 返回执行结果
            cache1Deferred.onAwait { it.copy(isCache = true) }
            cache2Deferred.onAwait { it.copy(isCache = true) }
            networkDeferred.onAwait { it.copy(isCache = false) }
        }

        updateUI(product)
        println("Time cost: ${System.currentTimeMillis() - startTime}")

        // 4.如果当前结果是缓存，再取最新的网络结果
        if (product.isCache) {
            val result = networkDeferred.await() ?: return@runBlocking
            updateUI(result)
            println("Time cost: ${System.currentTimeMillis() - startTime}")
        }
    }

    // select 和 channel 配合使用
    private fun selectWithChannelExample() = runBlocking {
//        1
//        a
//        2
//        b
//        3
//        c
//        Time cost: 545

        val startTime = System.currentTimeMillis()

        val channel1 = produce {
            send("1")
            delay(200)
            send("2")
            delay(200)
            send("3")
            delay(150)
        }
        val channel2 = produce {
            delay(100)
            send("a")
            delay(200)
            send("b")
            delay(200)
            send("c")
        }

        // 重复执行6次，目的是把 channel 管道的所有数据都消费掉
        repeat(6) {
            // 每次只取最快返回的结果打印出来
            val result = selectChannel(channel1, channel2)
            println(result)
        }

        println("Time cost: ${System.currentTimeMillis() - startTime}")
    }

    private fun selectWithChannelExample2() = runBlocking {
//        a
//        b
//        c
//        channel2 is closed!
//        channel2 is closed!
//        channel2 is closed!
//        Time cost: 545

        val startTime = System.currentTimeMillis()

        val channel1 = produce<String> {
            delay(1500) // 模拟管道出问题
        }
        val channel2 = produce {
            delay(100)
            send("a")
            delay(200)
            send("b")
            delay(200)
            send("c")
        }

        // 重复执行多次，目的是把 channel 管道的所有数据都消费掉
        repeat(6) {
            // 每次只取最快返回的结果打印出来
            val result = selectChannel(channel1, channel2)
            println(result)
        }

        // 执行结束后关闭管道
        channel1.cancel()
        channel2.cancel()

        println("Time cost: ${System.currentTimeMillis() - startTime}")
    }

    private fun selectWithDeferredCancelAllExample() = runBlocking {
//        done2
//        result2

        suspend fun <T> fastest(vararg deferreds: Deferred<T>): T = select {
            fun cancelAll() = deferreds.forEach { it.cancel() }

            for (deferred in deferreds) {
                deferred.onAwait {
                    // 取到最快返回的结果后，将其他关闭避免资源浪费
                    cancelAll()
                    it
                }
            }
        }

        val deferred1 = async {
            delay(100)
            println("done1")
            "result1"
        }
        val deferred2 = async {
            delay(50)
            println("done2")
            "result2"
        }
        val deferred3 = async {
            delay(1000)
            println("done3")
            "result3"
        }
        val deferred4 = async {
            delay(2000)
            println("done4")
            "result4"
        }
        val deferred5 = async {
            delay(1400)
            println("done5")
            "result5"
        }

        val result = fastest(deferred1, deferred2, deferred3, deferred4, deferred5)
        println(result)
    }

    private suspend fun selectChannel(
        channel1: ReceiveChannel<String>,
        channel2: ReceiveChannel<String>
    ) = select<String> {
        // onReceiveCatching：管道如果没有数据关闭了，给出提示而不是异常崩溃
        channel1.onReceiveCatching {
            it.getOrNull() ?: "channel1 is closed!"
        }
        channel2.onReceiveCatching {
            it.getOrNull() ?: "channel2 is closed!"
        }
    }
}

data class Product(
    val productId: String,
    val price: Double,
    val isCache: Boolean = false // 是否从缓存获取的
)