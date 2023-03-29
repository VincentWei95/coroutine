package com.example.coroutine.source

/**
 * 挂起函数原理：
 *
 * 从使用角度而言，挂起函数的本质是 callback（callback 名为 Continuation，词源 continue）
 * kotlin 编译器会将 suspend 关键字的函数转换成带有 callback 的函数
 * 将 suspend 挂起函数转换为 callback 的过程，叫做 CPS 转换（Continuation-Passing_Style-Transformation）
 *
 * 从协程执行角度而言，kotlin 挂起函数本质上是一个状态机
 *
 * 综合起来，协程的本质可以说是 CPS + 状态机
 *
 * 源码重点分析：
 *
 * 一、kotlin 挂起函数经过编译器 CPS 转换后，函数签名和返回值会有变化：
 * 1、参数变化：多了一个 Continuation 类型的参数
 * suspend fun getUserInfo() => getUserInfo(Continuation continuation)
 *
 * 2、返回值类型变化：挂起函数原本的返回值类型会作为 Continuation 的泛型参数，返回值修改为 Any?
 * suspend fun getUserInfo(): String => getUserInfo(Continuation<String> continuation): Any?
 *
 *
 *
 * 二、为什么返回值是 Any？因为挂起函数有可能被挂起，也有可能不被挂起，所以无法确定只能返回 Any?
 *
 * // 虽然是挂起函数，但是并没有挂起
 * suspend fun getUserInfo(): String {
 *  return "BoyCoder"
 * }
 *
 * // 执行到 withContext{}，挂起函数会被挂起
 * // 状态机识别是函数有挂起会返回 CoroutineSingletons.COROUTINE_SUSPENDED 标记
 * suspend fun getUserInfo(): String {
 *  withContext(Dispatchers.IO) {
 *      delay(1000L)
 *  }
 *  return "BoyCoder"
 * }
 *
 * 三、挂起函数经过反编译以后，会变成由 switch 和 label 组成的状态机结构
 *
 * suspend fun testCoroutine() {
 *  log("start")
 *  val user = getUserInfo()
 *  log(user)
 *  val friendList = getFriendList(user)
 *  log(friendList)
 *  val feedList = getFeedList(friendList)
 *  log(feedList)
 * }
 *
 * 反编译结果（核心逻辑伪代码）：
 *
 * fun testCoroutine(completion: Continuation<Any?>): Any? {
 *  class TestContinuation(completion: Continuation<Any?>?): ContinuationImpl(completion) {
 *      // 表示协程状态机当前状态
 *      // 值改变一次表示挂起函数被调用一次
 *      var label: Int = 0
 *
 *      // 协程返回结果
 *      var result: Any? = null
 *
 *      // 存储挂起函数执行结果
 *      var mUser: Any? = null
 *      var mFriendList: Any? = null
 *
 *      // 状态机的入口，会将执行流程交给 testCoroutine() 进行再次调用
 *      override fun invokeSuspend(_result: Result<Any?>): Any?) {
 *          result = _result
 *          label = label or Int.Companion.MIN_VALUE
 *          return testCoroutine(this)
 *      }
 *  }
 *
 *  // 整个运行期间只会产生一个实例，节省内存开销
 *  val continuation = if (completion is TestContinuation) {
 *      completion // 不是初次运行
 *  } else {
 *      TestContinuation(completion) // 初次运行
 *  }
 *
 *  // 挂起函数执行结果临时存储
 *  lateinit var user: String
 *  lateinit var friendList: String
 *  lateinit var feedList: String
 *
 *  // 接收协程的运行结果
 *  var result = continuation.result
 *
 *  // 接收挂起函数的返回值
 *  var suspendReturn: Any? = null
 *
 *  // COROUTINE_SUSPENDED 代表函数被挂起
 *  val sFlag = CoroutineSingletons.COROUTINE_SUSPENDED
 * }
 *
 * // 状态机，continuation.label 值改变一次表示挂起函数被调用一次
 * when (continuation.label) {
 *  0 -> {
 *      // 检测异常
 *      throwOnFailure(result)
 *
 *      log("start")
 *
 *      // 将 label 置为 1，准备进入下一次状态
 *      continuation.label = 1
 *
 *      // 执行 getUserInfo
 *      suspendReturn = getUserInfo(continuation)
 *
 *      // 判断是否挂起，如果没有挂起，直接下一个状态
 *      if (suspendReturn == sFlag) {
 *          return suspendReturn
 *      } else {
 *          result = suspendReturn
 *          // go to next state
 *      }
 *  }
 *
 *  1 -> {
 *      throwOnFailure(result)
 *
 *      // 获取 user 值
 *      user = result as String
 *      log(user)
 *
 *      // 将协程结果存到 continuation
 *      continuation.mUser = user
 *
 *      // 准备进入下一个状态
 *      continuation.label = 2
 *
 *      // 执行 getFriendList
 *      suspendReturn = getFriendList(user, continuation)
 *
 *      if (suspendReturn == sFlag) {
 *          return suspendReturn
 *      } else {
 *          result = suspendReturn
 *          // go to next state
 *      }
 *  }
 *
 *  2 -> {
 *      throwOnFailure(result)
 *
 *      user = continuation.mUser as String
 *
 *      // 获取 friendList 值
 *      friendList = result as String
 *      log(friendList)
 *
 *      // 将协程结果存到 continuation
 *      continuation.mUser = user
 *      continuation.mFriendList = friendList
 *
 *      // 准备进入下一个状态
 *      continuation.label = 3
 *
 *      // 执行 getFeedList
 *      suspendReturn = getFeedList(user, friendList, continuation)
 *
 *      if (suspendReturn == sFlag) {
 *          return suspendReturn
 *      } else {
 *          result = suspendReturn
 *          // go to next state
 *      }
 *  }
 *
 *  3 -> {
 *      throwOnFailure(result)
 *
 *      user = continuation.mUser as String
 *      friendList = continuation.mFriendList as String
 *      feedList = continuation.result as String
 *      log(feedList)
 *      loop = false
 *  }
 * }
 */