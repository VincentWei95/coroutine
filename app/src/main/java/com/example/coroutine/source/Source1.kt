package com.example.coroutine.source

/**
 * 协程源码目录说明
 *
 * ---------------------------------------------
 * JVM JS Native                                平台层
 * ---------------------------------------------
 * Job Deferred Select Channel Flow             中间层
 * ---------------------------------------------
 * ContinuationInterceptor RestrictsSuspension
 * CancellationException CoroutineContext       基础层
 * SafeContinuation Continuation Intrinsics
 *
 * 自底向上分别是：
 * 基础层：
 * kotlin 库当中定义的协程基础元素。
 * kotlinx.coroutines 协程框架就是基于该层的协程基础元素构造出来的
 *
 * 中间层：
 * 协程框架通用逻辑 kotlinx.coroutines-common。
 * kotlinx.coroutines 源码中的 common 子模块，里面包含了 kotlin 协程框架的通用逻辑
 * 比如 launch、async、CoroutineScope、Job、Deferred、Channel、Select、Flow 等
 * 在这一层只有纯粹的协程框架逻辑，不会包含任何特定的平台特性
 *
 * 平台层：
 * 这个是指协程在特定平台的实现，比如 JVM、JS、Native。
 * kotlin 协程最终都会运行在协程之上，协程会基于不同特性平台做支持，比如 JVM 则运行在线程池、JS 则是 JS 线程
 */
