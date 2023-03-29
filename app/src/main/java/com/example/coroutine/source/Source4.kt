package com.example.coroutine.source

/**
 * Dispatcher 切换线程原理
 *
 * 1、createCoroutineUnintercepted() 创建 Continuation 实例，接着调用 intercepted() 将其封装成 DispatchedContinuation 对象
 *
 * 2、DispatchedContinuation 会持有 CoroutineDispatcher 和步骤1创建的 Continuation 对象。CoroutineDispatcher 就是线程池
 *
 * 3、执行 DispatchedContinuation 的 resumeCancellableWith()，会执行 dispatcher.dispatch()，
 *    将 Continuation 封装成 Task 添加到 Worker 本地任务队列等待执行。
 *    Worker 本质上就是 Java 的 Thread。在这一步，协程就已经完成了线程的切换
 *
 * 4、Worker 的 run() 调用 runWork()，它会执行本地的任务队列当中取出 Task，调用 task.run()。
 *    而它实际上调用的是 DispatchedContinuation 的 run() 方法，在这里会调用 continuation.resume()，它将执行原本 launch 当中生成的 SuspendLambda 子类。
 *    这时候，launch 协程体当中的代码，就在线程上执行了
 *
 *
 * 在讲解 launch 启动协程原理时有提及到，下面这段代码是创建和启动协程的流程：
 *
 * public fun <T> (suspend () -> T).startCoroutineCancellable(completion: Continuation<T>): Unit = runSafely(completion) {
 *  createCoroutineUnintercepted(completion).intercepted().resumeCancellableWith(Result.success(Unit))
 * }
 *
 * 将上面的原理分析梳理为伪代码如下：
 *
 * fun createCoroutineUnintercepted(): Continuation {
 *  return SuspendLambda()
 * }
 *
 * class SuspendLambda : Continuation {
 *  fun intercepted(): DispatchedContinuation {
 *      return DispatchedContinuation(this)
 *  }
 * }
 *
 * class DispatchedContinuation(private val continuation: Continuation) {
 *  private val dispatcher = CoroutineDispatcher() // 可以理解为线程池
 *
 *  fun resumeCancellableWith() {
 *      dispatcher.dispatch(Task(this)) // 将 Continuation 添加到等待队列
 *  }
 *
 *  fun run() {
 *      continuation.resume() // 执行协程，此时已经切换了线程
 *  }
 * }
 *
 * class CoroutineDispatcher {
 *  private val tasks = Queue<Task>()
 *
 *  fun dispatch(task: Task) {
 *      tasks.add(task)
 *  }
 *
 *  // 假设这里是线程池启动，已经切换了线程
 *  fun execute() {
 *      // 从队列取出等待执行的 Task
 *      val task = tasks.poll()
 *      task.run()
 *  }
 * }
 *
 * // 这里为了方便理解不用 Worker 说明，直接在 Task
 * class Task(private val dispatchContinuation: DispatchedContinuation) : Runnable {
 *  fun run() {
 *      dispatchContinuation.run()
 *  }
 * }
 */