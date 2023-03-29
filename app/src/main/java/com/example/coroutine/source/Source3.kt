package com.example.coroutine.source

/**
 * launch 原理
 *
 * kotlin 协程框架在中间层实现了 launch、async 之类的构建器，它们只是对协程底层 API 进行了更好的封装而已
 * 它们除了拥有启动协程的基础能力，还支持传入 CoroutineContext、CoroutineStart 等参数
 * 前者可以帮我们实现结构化并发，后者可以支持更灵活的启动模式
 *
 * createCoroutine{}、startCoroutine{}，它们是 kotlin 提供的两个底层 API
 * 前者用来创建协程，后者用来创建并同时启动协程，launch 和 async 也是使用它们启动的协程。
 *
 *
 * 一、底层 API createCoroutine{} 和 startCoroutine{} 启动协程 demo：
 *
 * private val block = suspend {
 *  println("Hello!")
 *  delay(1000)
 *  println("World!")
 *  "Result"
 * }
 *
 * private fun testStartCoroutine() {
 *  val continuation = object : Continuation<String> {
 *      override val context: CoroutineContext
 *          get() = EmptyCoroutineContext
 *
 *      override fun resumeWith(result: Result<String>) {
 *          println("Result is: ${result.getOrNull()}")
 *      }
 *  }
 *
 *  // 创建并启动协程，扩展函数其实调用了 resume()
 *  block.startCoroutine(continuation)
 * }
 *
 * private fun testCreateCoroutine() {
 *  val continuation = object : Continuation<String> {
 *      override val context: CoroutineContext
 *          get() = EmptyCoroutineContext
 *
 *      override fun resumeWith(result: Result<String>) {
 *          println("Result is: ${result.getOrNull()}")
 *      }
 *  }
 *
 *  // 创建协程
 *  val coroutine = block.createCoroutine(continuation)
 *
 *  // createCoroutine{} 调用 resume() 才启动
 *  coroutine.resume(Unit)
 * }
 *
 * 二、launch 启动协程原理分析
 *
 * demo:
 *
 * fun main() {
 *  testLaunch()
 *  Thread.sleep(2000L)
 * }
 *
 * private fun testLaunch() {
 *  val scope = CoroutineScope(Job())
 *  scope.launch {
 *      println("Hello!")
 *      delay(1000L)
 *      println("World!")
 *  }
 * }
 *
 * launch 启动协程源码分析：
 *
 * public fun CoroutineScope.launch(
 *  context: CoroutineContext = EmptyCoroutineContext,
 *  start: CoroutineStart = CoroutineStart.DEFAULT,
 *  block: suspend CoroutineScope.() -> Unit
 * ): Job {
 *  val newContext = newCoroutineContext(context)
 *  val coroutine = if (start.isLazy) {
 *      LazyStandaloneCoroutine(newContext, block) else
 *      StandaloneCoroutine(newContext, active = true)
 *  }
 *  coroutine.start(start, coroutine, block) // 创建并启动协程
 *  return coroutine
 * }
 *
 * public enum class CoroutineStart {
 *  public operator fun <T> invoke(block: suspend () -> T, completion: Continuation<T>): Unit =
 *      when (this) {
 *          DEFAULT -> block.startCoroutineCancellable(completion) // launch 启动协程走这句代码
 *          ...
 *      }
 * }
 *
 * // createCoroutineUnintercepted().intercepted()：底层 API startCoroutine{} 和 createCoroutine{} 创建协程也是走的这句代码逻辑
 *
 * // 以下代码可以拆分成三部分：
 * // createCoroutineUnintercepted()：创建协程
 * // intercepted()：将程序执行逻辑派发到特定线程上
 * // resumeCancellableWith()：启动协程，执行的 Continuation.resumeWith()
 * public fun <T> (suspend () -> T).startCoroutineCancellable(completion: Continuation<T>): Unit = runSafely(completion) {
 *  createCoroutineUnintercepted(completion).intercepted().resumeCancellableWith(Result.success(Unit))
 * }
 *
 * public actual fun <T> (suspend () -> T).createCoroutineUnintercepted(
 *      completion: Continuation<T>
 * ): Continuation<Unit> {
 *      val probeCompletion = probeCoroutineCreated(completion)
 *
 *      return if (this is BaseContinuationImpl)
 *          // 调用这一句代码创建协程，对应 LaunchDecompile$testLaunch$1 的 create()
 *          create(probeCompletion)
 *      else
 *          createCoroutineFromSuspendFunction(probeCompletion) {
 *              (this as Function1<Continuation<T>, Any>).invoke(it)
 *          }
 * }
 *
 *
 * demo 反编译结果：
 *
 * public final class LaunchDecompile {
 *  public static final void main() throws InterruptedException {
 *      testLaunch();
 *      Thread.sleep(2000L);
 *  }
 *
 *  private static final void testLaunch() {
 *      CoroutineScope scope = CoroutineScopeKt.CoroutineScope(JobKt.default(null, 1, null))
 *      // 创建并启动协程
 *      BuildersKt.launch(scope, null, null, new LaunchDecompile$testLaunch$1(null), 3, null);
 *  }
 *
 *  static final class LaunchDecompile$testLaunch$1 extends SuspendLambda
 *      implements Function2<CoroutineScope, Continuation<? super Unit>, Object> {
 *      int label;
 *
 *      LaunchDecompile$testLaunch$1(Continuation $completion) {
 *          super(2, $completion);
 *      }
 *
 *      // 协程执行时的状态机处理逻辑
 *      @Nullable
 *      public final Object invokeSuspend(@NotNull Object $result) {
 *          ...
 *      }
 *
 *      // 调用该方法时创建协程，随后调用 resume() 启动协程
 *      @NotNull
 *      public final Continuation<Unit> create(Object v, Continuation<*> $completion) {
 *          return new LaunchDecompile$testLaunch$1($completion);
 *      }
 *  }
 * }
 */