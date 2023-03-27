package com.example.coroutine

import kotlinx.coroutines.channels.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Channel
 *
 * 使用 Channel 一般通过 produce{} 创建，使用 consumeEach{} 接收
 * 如果要定制 Channel 的处理逻辑，就只能自己创建 Channel，但需要自己处理关闭逻辑
 */
object Main5 {

    @JvmStatic
    fun main(args: Array<String>) {
        channelConsumeEachExample()
    }

    private fun channelExample() = runBlocking {
//        ========================
//        end
//        Thread:main @coroutine#1
//        ========================
//        receive: 1
//        Thread:main @coroutine#3
//        ========================
//        send: 1
//        Thread:main @coroutine#2
//        ========================
//        send: 2
//        Thread:main @coroutine#2
//        ========================
//        receive: 2
//        Thread:main @coroutine#3
//        ========================
//        receive: 3
//        Thread:main @coroutine#3
//        ========================
//        send: 3
//        Thread:main @coroutine#2
//        程序没有停止运行... // 需要调用 close() 停止 channel

        // channel 就是管道，由发送方到接收方，管道可以传输数据
        // channel 可以在不同的协程通信

        val channel = Channel<Int>()

        launch {
            // 在一个单独的协程中发送管道消息
            (1..3).forEach {
                channel.send(it) // 挂起函数
                logX("send: $it")
            }

            channel.close() // 发送完后调用 close() 停止协程
        }

        launch {
            // 在一个单独的协程当中接受不了管道消息
            for (i in channel) {
                logX("receive: $i")
            }
        }

        logX("end")
    }

    private fun channelParamExample() = runBlocking {
        // val channel = Channel<Int>(capacity = Channel.Factory.UNLIMITED)
        // 因为管道数量无限，发送策略就是先在管道都塞入数据后再开始接收
//        end
//        send: 1
//        send: 2
//        send: 3
//        receive: 1
//        receive: 2
//        receive: 3

        // channel 的几个参数：
        // capacity：
        //  RENDEZVOUS，默认方式，代表 channel 容量为 0
        //  UNLIMITED，无限容量
        //  CONFLATED，容量为 1，新数据会替代旧数据
        //  BUFFERED，具备一定缓存容量，默认 64

        // onBufferOverflow：当超过 capacity 指定的容量时的处理策略
        //  SUSPEND，如果还要继续发送会挂起 send()，等管道空闲了再恢复
        //  DROP_OLDEST，丢弃最旧的那条数据，然后发送新的数据
        //  DROP_LATEST，丢弃最新的那条数据，丢弃那条即将发送的那条数据

        // onUndeliveredElement：异常处理回调

        val channel = Channel<Int>(capacity = Channel.Factory.UNLIMITED)
        launch {
            (1..3).forEach {
                channel.send(it)
                println("send: $it")
            }
            channel.close()
        }
        launch {
            for (i in channel) {
                println("receive: $i")
            }
        }
        println("end")
    }

    private fun channelConflatedExample() = runBlocking {
        // 只拿最新一条数据
//        end
//        send: 1
//        send: 2
//        send: 3
//        receive: 3

        val channel = Channel<Int>(capacity = Channel.Factory.CONFLATED)
        launch {
            (1..3).forEach {
                channel.send(it)
                println("send: $it")
            }
            channel.close()
        }
        launch {
            for (i in channel) {
                println("receive: $i")
            }
        }
        println("end")
    }

    private fun channelBufferOverflowDropOldestExample() = runBlocking {
        // BufferOverflow.DROP_OLDEST 设置为丢弃旧数据
//        end
//        send: 1
//        send: 2
//        send: 3
//        receive: 3

        val channel = Channel<Int>(
            capacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        launch {
            (1..3).forEach {
                channel.send(it)
                println("send: $it")
            }
            channel.close()
        }
        launch {
            for (i in channel) {
                println("receive: $i")
            }
        }
        println("end")
    }

    private fun channelBufferOverflowDropLatestExample() = runBlocking {
        // BufferOverflow.DROP_LATEST 管道满了再发送就丢弃
//        end
//        send: 1
//        send: 2
//        send: 3
//        send: 4
//        send: 5
//        receive: 1
//        receive: 2
//        receive: 3

        val channel = Channel<Int>(
            capacity = 3,
            onBufferOverflow = BufferOverflow.DROP_LATEST
        )
        launch {
            (1..3).forEach {
                channel.send(it)
                println("send: $it")
            }
            channel.send(4) // 被丢弃
            println("send: 4")
            channel.send(5) // 被丢弃
            println("send: 5")

            channel.close()
        }
        launch {
            for (i in channel) {
                println("receive: $i")
            }
        }
        println("end")
    }

    private fun channelUndeliveredElementExample() = runBlocking {
        // onUndeliveredElement 回调异常
//        onUndeliveredElement = 2
//        onUndeliveredElement = 3

        val channel = Channel<Int>(Channel.UNLIMITED) {
            println("onUndeliveredElement = $it")
        }

        // 放入三个数据
        (1..3).forEach {
            channel.send(it)
        }

        // 取出一个，剩下两个
        channel.receive()

        // 取消当前 channel
        channel.cancel()
    }

    private fun channelProduceExample() = runBlocking {
        // produce 会在处理完管道数据后自动关闭，不用手动调用 close()
//        ClosedReceiveChannelException: Channel was closed
        val channel: ReceiveChannel<Int> = produce {
            (1..3).forEach {
                send(it)
            }
        }

        channel.receive()
        channel.receive()
        channel.receive()
        channel.receive() // 取完管道已经关闭，再取会抛出异常

        logX("end")
    }

    private fun channelConsumeEachExample() = runBlocking {
//        send: 1
//        send: 2
//        send: 3
//        send: 4
//        receive 1
//        receive 2
//        receive 3
//        receive 4
//        receive 5
//        send: 5
//        ========================
//        end
//        Thread:main

        val channel = produce(capacity = 3) {
            (1..5).forEach {
                send(it)
                println("send: $it")
            }
        }

        // channel.receive() 是非常危险的操作，如果管道没有 close() 它会一直挂起
        // 应该使用 consumeEach 接收数据，接收结束后会自动关闭
        channel.consumeEach {
            println("receive $it")
        }

        logX("end")
    }
}
