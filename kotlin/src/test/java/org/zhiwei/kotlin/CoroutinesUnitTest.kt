package org.zhiwei.kotlin

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.junit.Test
import java.io.IOException

/**
 * 协程相关的单元测试
 */
class CoroutinesUnitTest {

    /**
     * 测试job的函数使用，协程单元测试，所以用了runBlocking提供协程环境和阻塞的执行域
     */
    @Test
    fun testJob() {
//        jobJoin()
//        jobHandler()
//        cancellable()
//        globalJob()
//        handleException()
//        handleExp()
        supervisorTest()

    }

    private fun jobJoin() = runBlocking {
        val job = launch {
            println("执行launch之前 -------")
            delay(2000)
            println("====== 执行launch的之后")
        }
        job.join()//join的作用会等待job的协程执行完，再让后面执行，如果不join，协程job仍会执行，但是就是异步的，
        println("测testJob的结束执行代码")
    }

    @OptIn(InternalCoroutinesApi::class)
    private fun jobHandler() = runBlocking {
        val handler = CoroutineExceptionHandler { coroutineContext, throwable ->
            println("协程$coroutineContext , 出现异常，被handler捕获了，${throwable.message}")
        }

        val jobHandler = launch(handler) {
            println("handler 执行launch之前 -------")
            delay(2000)
            //cancel和error 同时只用一个测试。
            cancel("抛🏃一个自定义的协程取消cancel信息")//只是抛出取消cancel信息，下面print还是会执行
//            error("咋了，这里error一个throwable抛出去")//正儿八经的error异常，下面println就不会执行了，但是会被invokeOnCompletion捕获到
            println("handler ====== 执行launch的之后")
        }
        jobHandler.invokeOnCompletion(true, true) {
            println("多参数的 jobHandler的invokeOnCompletion 执行到了 ${it?.message}")
        }
        jobHandler.invokeOnCompletion {
            println("jobHandler的invokeOnCompletion 执行到了 ${it?.message}")
        }
        //没有job的join，所以先输出这个，而后才是协程内的执行
        println("测jobHandler 的结束执行代码")
    }

    @OptIn(InternalCoroutinesApi::class)
    private fun cancellable() = runBlocking {
        val job = launch {
            //withContext+NonCancellable，会创建一个不可取消的协程，即使job调用了cancel，它依旧执行；但是其自身内部可以cancel来结束。
            withContext(NonCancellable) {
                repeat(20) {
                    delay(200)
                    println("输出log模拟...")
                    //这cancel会取消withContext启动的子协程
//                    if (it==10)cancel("内部👘cancel可以吗？")
                }
            }
        }
        delay(1000)

//        job.cancel("⚠️手动取消协程任务，withContext的不会被取消执行")
        //这个带参数的，写法，虽然在cancel之后，但是能感知到（配置true的话）
//        job.invokeOnCompletion(true,true) {
//            println("此处立即感知 cancel的信息：${it?.message}")
//        }
        //而这个，必须job执行结束才会收到回调
        job.invokeOnCompletion {
            println("此处job完成结束才感知 cancel的信息：${it?.message}")
        }
        println("--------->>>>>   结束🔚 <<<<<-----   ")
    }

    private fun globalJob() {
        val handler = CoroutineExceptionHandler { coroutineContext, throwable ->
            println("异常捕获的handler来啦🤣 $coroutineContext ,,,  ${throwable.message}")
        }
        val job = GlobalScope.launch(handler) {
            repeat(20) {
                delay(200)
                println("模拟日志log打印...")
                if (it == 4) error("👘个异常👀")
            }
        }
        Thread.sleep(5000)
//        job.cancel("取消，看看Global会结束么")
        println("globalJob的最后一行.")
    }

    private fun handleException() = runBlocking() {

        val handler = CoroutineExceptionHandler { _, throwable ->
            println("异常捕获的handler来啦🤣 ${throwable.message}")
        }
        //因为是单元测试，外面使用了runBlocking，协程内部的异常，一般都会外抛到最外层的协程才能捕获。所以这里使用了supervisor来阻断外传异常，让handler能生效
        supervisorScope {
            try {
                val job = launch(handler) {
                    repeat(20) {
                        delay(200)
                        println("模拟log输出...")
                        if (it == 4) error("📖⚠️来一个异常error呀")
                    }
                }
                job.join()
            } catch (e: Exception) {
                println("try catch不住协程内的异常,cancel的也不能try catch住 ,但是能感知到信息，也就是能走到这里，但JVM依然exception ${e.message}")
            }
        }

        println("handleException 函数最后一行的 ----")
    }

    /**
     * 协程内异常可能会多个，连锁式的，handler中有内置的suppressed的数组，包含次生异常
     */
    private fun handleExp() = runBlocking {
        val handler = CoroutineExceptionHandler { _, exception ->
            // 第三, 这里的异常是第一个被抛出的异常对象
            println("捕捉的异常: $exception 和被嵌套的异常: ${exception.suppressed.contentToString()}")
        }
        val job = GlobalScope.launch(handler) {
            launch {
                try {
                    delay(Long.MAX_VALUE)
                } finally { // 当父协程被取消时其所有子协程都被取消, finally被取消之前或者完成任务之后一定会执行
                    throw ArithmeticException() // 第二, 再次抛出异常, 异常被聚合
                }
            }
            launch {
                delay(100)
                throw IOException() // 第一, 这里抛出异常将导致父协程被取消
            }
            delay(Long.MAX_VALUE)
        }
        job.join() // 避免GlobalScope作用域没有执行完毕JVM虚拟机就退出
    }

    private fun supervisorTest() = runBlocking {
        val handler = CoroutineExceptionHandler { _, throwable ->
            println("输出 捕获了异常 ${throwable.message}")
        }

        supervisorScope {
//            error("这个没用，捕获不住")
            launch(handler) {
//                error("这个好像能捕获")
                repeat(10) {
                    delay(200)
                    println("....打log啊🤔")
                }
            }
        }
        //supervisor自身就是阻塞的，需要执行完内部，外面的才执行
        launch {
            println("这里会在supervisor之后执行")
        }
        println("----- 最后一行 ")

    }


}