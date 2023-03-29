package com.example.coroutine.source

/**
 * CoroutineScope 管理协程原理
 *
 * 1、每次创建 CoroutineScope 时，内部都会确保 CoroutineContext 存在 Job，因为 CoroutineScope 是通过 Job 管理协程的
 *
 * 2、通过 launch、async 创建协程的时候，会同时创建 AbstractCoroutine 的子类，在它的 initParentJob() 方法当中，会建立协程的父子关系。
 *    每个协程都会对应一个 Job，而每个 Job 都会有一个父 Job，多个子 Job。最终它们会形成一个 N 叉树的结构
 *
 * 3、由于协程是一个 N 叉树的结构，因此协程的取消事件以及异常传播，也会按照这个结构进行传递。
 *    每个 Job 取消的时候，都会通知自己的子 Job 和父 Job，最终以递归的形式传递给每一个协程。
 *    另外，协程在向上取消父 Job 的时候，还利用了责任链模式，确保取消事件可以一步步传播到最顶层的协程。
 *    这里还有一个细节就是，默认情况下，父协程都会忽略子协程的 CancellationException
 *
 * 协程结构化并发取消的规律：
 * 1、对于 CancellationException 引起的取消，它只会向下传播，取消子协程
 * 2、对于其他的异常引起的取消，它既向上传播，也向下传播，最终会导致所有协程都被取消
 */