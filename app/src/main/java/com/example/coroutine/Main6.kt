package com.example.coroutine

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.*
import java.lang.Exception
import java.lang.IllegalStateException
import java.util.concurrent.Executors

/**
 * Flow
 */
object Main6 {

    @JvmStatic
    fun main(args: Array<String>) {
        conflateExample()
    }

    private fun flowExample() = runBlocking {
//        6
//        8

        // flow 会一个个的发
        // flow {} 会创建一个 Flow，emit() 会往下游发送数据
        // filter{}、map{}、take{} 是中间操作符
        // collect{} 是终止操作赋，调用后会终止数据流并接收数据，可以理解为 RxJava 最后的 subscribe() 接收结果

        flow {
            emit(1)
            emit(2)
            emit(3)
            emit(4)
            emit(5)
        }.filter { it > 2 } // 取 3、4、5
            .map { it * 2 } // 转换发送下来的数据
            .take(2) // 取前两个
            .collect {
                println(it)
            }
    }

    private fun flowOfExample() = runBlocking {
//        6
//        8
//        6
//        8

        // flowOf() 也可以创建一个 Flow

        flowOf(1, 2, 3, 4, 5)
            .filter { it > 2 }
            .map { it * 2 }
            .take(2)
            .collect {
                println(it)
            }

        listOf(1, 2, 3, 4, 5)
            .filter { it > 2 }
            .map { it * 2 }
            .take(2)
            .forEach {
                println(it)
            }
    }

    private fun listTransToFlowExample() = runBlocking {
//        6
//        8
//        6
//        8
        // toList() 可以将 Flow 转换为 List
        // asFlow() 可以将 List 转换为 Flow

        flowOf(1, 2, 3, 4, 5)
            .toList()
            .filter { it > 2 }
            .map { it * 2 }
            .take(2)
            .forEach {
                println(it)
            }

        listOf(1, 2, 3, 4, 5)
            .asFlow()
            .filter { it > 2 }
            .map { it * 2 }
            .take(2)
            .collect {
                println(it)
            }
    }

    private fun flowOnStartOnCompletionExample() = runBlocking {
//        onStart
//        filter: 1
//        filter: 2
//        filter: 3
//        map: 3
//        collect: 6
//        filter: 4
//        map: 4
//        collect: 8
//        onCompletion

        // onStart{} 与位置无关，类似 RxJava 的 doOnSubscribe()
        // onCompletion{} 与位置无关，类似 RxJava 的 onComplete()

        flowOf(1, 2, 3, 4, 5)
            .filter {
                println("filter: $it")
                it > 2
            }
            .map {
                println("map: $it")
                it * 2
            }
            .take(2)
            .onStart { println("onStart") }
            .onCompletion { println("onCompletion") }
            .collect {
                println("collect: $it")
            }
    }

    private fun flowCancelCallbackOnCompletionExample() = runBlocking {
//        collect: 1
//        collect: 2
//        cancel
//        onCompletion first: kotlinx.coroutines.JobCancellationException: StandaloneCoroutine was cancelled; job=StandaloneCoroutine{Cancelling}@4524411f
//        collect: 4
//        onCompletion second: java.lang.IllegalStateException

        // onCompletion{} 会在 Flow 正常执行完毕、异常、被取消时都会回调

        launch {
            flow {
                emit(1)
                emit(2)
                emit(3)
            }.onCompletion { println("onCompletion first: $it") }
                .collect {
                    println("collect: $it")
                    if (it == 2) {
                        cancel()
                        println("cancel")
                    }
                }
        }

        delay(100L)

        flowOf(4, 5, 6)
            .onCompletion { println("onCompletion second: $it") }
            .collect {
                println("collect: $it")
                // 仅用于测试，生产环境不应该这么创建异常
                // 已经在下游终止操作符处理时抛出异常是要另外处理的
                throw IllegalStateException()
            }
    }

    private fun flowCatchExceptionExample() = runBlocking {
//        2
//        4
//        catch: java.lang.IllegalStateException

        val flow = flow {
            emit(1)
            emit(2)
            throw IllegalStateException()
            emit(3)
        }

        flow.map { it * 2 }
            .catch { println("catch: $it") } // 捕获异常，和位置强相关，只能捕获 catch{} 的上游抛出的异常
            .collect {
                println(it)
            }
    }

    private fun flowCollectTryCatchExample() = runBlocking {
//        collect: 4
//        catch java.lang.IllegalStateException
//                collect: 5
//        catch java.lang.IllegalStateException
//                collect: 6
//        catch java.lang.IllegalStateException
//                onCompletion send: null

        flowOf(4, 5, 6)
            .onCompletion { println("onCompletion send: $it") }
            .collect {
                try {
                    println("collect: $it")
                    throw IllegalStateException()
                } catch (e: Exception) {
                    println("catch $e")
                }
            }
    }

    private fun flowOnExample() = runBlocking {
//        ========================
//        start
//        Thread:DefaultDispatcher-worker-1 @coroutine#2
//        ========================
//        filter: 1
//        Thread:DefaultDispatcher-worker-1 @coroutine#2
//        ========================
//        emit 1
//        Thread:DefaultDispatcher-worker-1 @coroutine#2
//        ========================
//        filter: 2
//        Thread:DefaultDispatcher-worker-1 @coroutine#2
//        ========================
//        emit 2
//        Thread:DefaultDispatcher-worker-1 @coroutine#2
//        ========================
//        filter: 3
//        Thread:DefaultDispatcher-worker-1 @coroutine#2
//        ========================
//        emit 3
//        Thread:DefaultDispatcher-worker-1 @coroutine#2
//        ========================
//        collect 3
//        Thread:main @coroutine#1

        val flow = flow {
            logX("start")
            emit(1)
            logX("emit 1")
            emit(2)
            logX("emit 2")
            emit(3)
            logX("emit 3")
        }

        flow.filter {
            logX("filter: $it")
            it > 2
        }
            .flowOn(Dispatchers.IO) // 切换线程，与位置强相关，flowOn{} 仅限它的上游
            .collect {
                logX("collect $it")
            }
    }

    private val myDispatcher = Executors.newSingleThreadExecutor {
        Thread(it, "myThread").apply { isDaemon = true }
    }.asCoroutineDispatcher()

    private fun launchInExample() = runBlocking {
//        ========================
//        start
//        Thread:DefaultDispatcher-worker-1 @coroutine#3
//        ========================
//        filter: 1
//        Thread:DefaultDispatcher-worker-1 @coroutine#3
//        ========================
//        emit 1
//        Thread:DefaultDispatcher-worker-1 @coroutine#3
//        ========================
//        filter: 2
//        Thread:DefaultDispatcher-worker-1 @coroutine#3
//        ========================
//        emit 2
//        Thread:DefaultDispatcher-worker-1 @coroutine#3
//        ========================
//        filter: 3
//        Thread:DefaultDispatcher-worker-1 @coroutine#3
//        ========================
//        emit 3
//        Thread:DefaultDispatcher-worker-1 @coroutine#3
//        ========================
//        map: 3
//        Thread:myThread @coroutine#2
//        ========================
//        onEach: 6
//        Thread:myThread @coroutine#2

        val flow = flow {
            logX("start")
            emit(1)
            logX("emit 1")
            emit(2)
            logX("emit 2")
            emit(3)
            logX("emit 3")
        }

        val scope = CoroutineScope(myDispatcher)
        flow
            .filter {
                logX("filter: $it")
                it > 2
            }
            .flowOn(Dispatchers.IO)
            .map {
                logX("map: $it")
                it * 2
            }
            .onEach {
                logX("onEach: $it")
            }
            .launchIn(scope)

        // launchIn() 等价于下面的代码，指定 launchIn() 上游执行的线程
//        scope.launch {
//            flow.collect()
//        }

        delay(100L)
    }

    private fun flowChannelExample() = runBlocking {
//        end
//        before send 1
//        Flow 的代码未执行
        // Channel 是热流，Flow 是冷流
        // Channel "热" 的意思是：不管有没有接收方，发送方都会工作
        // Flow "冷" 的意思是：你要了我才工作
        // Flow 还是 "懒" 的，一次只处理一条数据

        val flow = flow {
            (1..3).forEach {
                println("before send $it")
                emit(it)
                println("send $it")
            }
        }

        val channel = produce(capacity = 0) {
            (1..3).forEach {
                println("before send $it")
                send(it)
                println("send $it")
            }
        }

        println("end")
    }

    private fun launchInScopeExample() = runBlocking {
//        show loading
//        ========================
//        receive: 0
//        Thread:myThread @coroutine#2
//        ========================
//        emit: 0
//        Thread:DefaultDispatcher-worker-1 @coroutine#3
//        ========================
//        emit: 1
//        Thread:DefaultDispatcher-worker-1 @coroutine#3
//        ========================
//        receive: 2
//        Thread:myThread @coroutine#2
//        ========================
//        emit: 2
//        Thread:DefaultDispatcher-worker-1 @coroutine#3
//        ========================
//        receive: 4
//        Thread:myThread @coroutine#2
//        hide loading


        fun loadData() = flow {
            repeat(3) {
                delay(100L)
                emit(it)
                logX("emit: $it")
            }
        }

        fun updateUI(it: Int) {
            logX("receive: $it")
        }

        fun showLoading() {
            println("show loading")
        }

        fun hideLoading() {
            println("hide loading")
        }

        val uiScope = CoroutineScope(myDispatcher)

        // flowOn() 到 launchIn() 之间的代码在 uiScope 运行
        loadData()
            .onStart { showLoading() }
            .map { it * 2 }
            .flowOn(Dispatchers.IO)
            .catch { throwable ->
                println(throwable)
                hideLoading()
                emit(-1) // 发生异常指定默认值指定默认值
            }
            .onEach {
                updateUI(it)
            }
            .onCompletion { hideLoading() }
            .launchIn(uiScope)

        delay(10000L)
    }

//    ============================= 其他操作符 =============================

    private fun debounceExample() = runBlocking {
//        2
//        5

        // debounce：确保 flow 的各项数据之间存在一定的时间间隔，如果是时间点过于临近的数据只会保留最后一条
        // 使用场景：搜索框输入文字，停止一个时间后才将输入结果拿去请求网络

        flow {
            emit(1)
            emit(2) // 2 和 3 之间间隔 600ms 可以发射成功
            delay(600)
            emit(3)
            delay(100)
            emit(4)
            delay(100)
            emit(5) // 3 和 4、4 和 5 之间间隔只有 100ms 不会发送；5 是最后一条数据，可以发射成功
        }.debounce(500).collect {// 设置两条数据之间间隔超过 500ms 才能发送成功
            println(it)
        }
    }

    private fun sampleExample() = runBlocking {
        // sample：和 debounce 类似也是取指定间隔时间的数据
        // 使用场景：数据源数据量很大，但又只需要展示少量数据的时候

        flow {
            while (true) {
                emit("发送消息")
            }
        }.sample(1000) // 每秒只显示一条消息
            .flowOn(Dispatchers.IO)
            .collect {
                println(it)
            }
    }

    private fun reduceExample() = runBlocking {
        // 等差数列，从 1 加到 100，输出结果 5050

        // reduce：acc 是累积值，value 是当前值

        val result = flow {
            for (i in (1..100)) {
                emit(i)
            }
        }.reduce { acc, value -> acc + value }

        println(result)
    }

    private fun foldExample() = runBlocking {
        // 输出结果：Alphabet: ABCDEFGHIJKLMNOPQRSTUVWXYZ

        // fold：和 reduce 一样的功能，不同的是需要传入初始值

        val result = flow {
            for (i in ('A'..'Z')) {
                emit(i.toString())
            }
        }.fold("Alphabet: ") { acc, value -> acc + value }

        println(result)
    }

    private fun flatMapConcatExample() = runBlocking {
//        a1
//        b1
//        a2
//        b2
//        a3
//        b3

        // flatMapConcat：和 RxJava 的 flatMap 操作符功能类似，合并后压平为一个 flow，合并后保证顺序

        flowOf(1, 2, 3).flatMapConcat {
            flowOf("a$it", "b$it")
        }.collect {
            println(it)
        }
    }

    private fun flatMapMergeExample() = runBlocking {
//        a100
//        b100
//        a200
//        b200
//        a300
//        b300

        // flatMapMerge：和 flatMapConcat 处理一样，合并后不保证顺序

        flowOf(300, 200, 100)
            .flatMapMerge {
                flow {
                    delay(it.toLong())
                    emit("a$it")
                    emit("b$it")
                }
            }
            .collect {
                println(it)
            }
    }

    private fun flatMapLatestExample() = runBlocking {
//        1
//        3

        // flatMapLatest：和 flatMapConcat 处理一样，但它只接收处理最新的数据
        // 如果有新数据到来了而前一个数据还没有处理完，则会将前一个数据剩余的处理逻辑全部取消

        flow {
            emit(1)
            delay(150)
            emit(2)
            delay(50)
            emit(3)
        }.flatMapLatest {
            flow {
                // 1 和 2 消息之间相隔 150ms，能处理发射了 1
                // 2 和 3 消息之间相隔 50ms，2 没处理完被丢弃，最后发射 3
                delay(100)
                emit("$it")
            }
        }.collect {
            println(it)
        }
    }

    private fun zipExample() = runBlocking {
//        a1
//        b2
//        c3

        // zip：类似 RxJava 的 zip，只要其中一个 flow 数据全部处理结束就会终止运行
        // flow 之间是并发执行的，zip 最终耗时是由执行时间比较久的 flow 决定

        val flow1 = flowOf("a", "b", "c")
        val flow2 = flowOf(1, 2, 3, 4, 5)
        flow1.zip(flow2) { a, b ->
            a + b
        }
//            .zip() // 可以继续拼接 flow
            .collect {
                println(it)
            }
    }

    private fun bufferExample() = runBlocking {
//        1 is ready
//        2 is ready
//        1 is handled
//        3 is ready
//        2 is handled
//        3 is handled

        // buffer：同样也是解决背压流速不均匀问题，不会丢弃数据。如果流速不均匀问题持续放大，应该用 conflate 适当丢弃一些数据
        // 让 flow 函数和 collect 函数运行在不同的协程当中，这样 flow 的数据发送不会受 collect 函数的影响

        flow {
            emit(1)
            delay(1000)
            emit(2)
            delay(1000)
            emit(3)
        }.onEach { println("$it is ready") }.buffer().collect {
            delay(1000) // 如果没有 buffer，每发送一条消息需要 2s
            println("$it is handled")
        }
    }

    private fun conflateExample() = runBlocking {
//        start handle 1
//        finish handle 1
//        start handle 3
//        finish handle 3
//        start handle 4
//        finish handle 4
//        start handle 6
//        finish handle 6
//        start handle 8
//        finish handle 8
//        start handle 10
//        finish handle 10

        // conflate：解决背压问题，会丢弃数据（没处理完就接收到的数据，之前的消息丢弃直接处理最新的数据）

        flow {
            for (i in (1..10)) {
                emit(i)
                delay(1000)
            }
        }.conflate().collect {
            println("start handle $it")
            delay(2000)
            println("finish handle $it")
        }
    }
}