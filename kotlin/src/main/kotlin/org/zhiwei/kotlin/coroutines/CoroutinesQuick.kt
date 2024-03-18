package org.zhiwei.kotlin.coroutines

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield

/**
 * 协程概念学习之 速览
 */
class CoroutinesQuick {

    //region 0. 协程概念

    /*
    1. 进程是系统CPU的任务单位，是资源分配单位；
    2. 线程是为了一个进程内可以更多任务执行，切换创建都比进程更性能好；
    3. 协程，是个编程语言和编译器的特性，通过函数调用栈来实现工作业务的切换调度，其实现可以是线程池也可以是别的方式；
     */

    //endregion

    // region 1. 创建协程
    private fun testCreate() {

        //1.1 GlobalScope创建协程，不阻塞线程；是与App进程生命周期同步的；GlobalScope无job返回，不可取消；
        //GlobalScope本身就是一个作用域, launch属于其子作用域;
        //GlobalScope的launch创建一个协程 ，它是有job返回的，可取消。
        val launch = GlobalScope.launch {
            delay(1000L)
            println("你好啊，协程GlobalScope的launch")
        }

        //1.2 CoroutineScope创建，不阻塞线程，可取消
        CoroutineScope(Dispatchers.IO).launch {
            delay(1000L)
            println("你好啊，协程CoroutineScope的launch")
        }

        //1.3 runBlocking 阻塞线程，一般用于单元测试。业务中很少用。
        runBlocking {
            delay(1000L)
            println("你好啊，runBlocking")
        }
    }

    //endregion

    //region 2.协程作用域

    /*
      1.协程作用域的创建 CoroutineScope的扩展函数创建（只能在其他协程作用域内使用）；suspend修饰的函数内；
      2.协程会等到作用域内的子协程都关闭完成，才会关闭自身。
      3.主协程内创建子协程 串行/并发
       */
    private fun testScope() {
        //此处为的是演示代码，因为协程函数基本都要在suspend函数内或者其他协程函数内才能调用；此处演示使用runBlocking
        runBlocking {
            //以下三个函数，都是suspend的，同步的
            withContext(Dispatchers.Main) {}//调度协程用的，有返回结果
            coroutineScope { }//创建协程作用域，会阻塞当前协程的执行；等待其内部完工后，才继续后面；
            supervisorScope { }//也是创建协程作用域，其特点是，内部出现异常不会影响到外部协程。

            //以下两个函数，不是suspend的，异步的
            launch { }//异步并发，无返回结果
            async { }//异步并发，有返回结果
        }

        //演示代码
        runBlocking {
            //2.1 在此runBlocking是一个协程作用域； 在协程作用域内，启动的异步协程是按顺序的，
            launch { println("异步协程 first") }
            launch { println("异步协程 second") }
            println("先于上面两个输出，因为协程的挂起是耗费时间的；")
            //2.2 async创建并发协程，可得到返回结果
            val numberOne = async {
                println("创建一个异步并发的async，并返回一个int结果，在delay 2秒后 ")
                800//返回的结果
            }
            val numberTwo = async {
                println("创建又☝️异步并发的async，延迟1.5秒后，返回Double ")
                200.99//返回的结果
            }
            //调用await就会等待异步协程的执行结果，两个async就是并发，耗费总时长是其中最长耗时的那个决定的。
            val result = numberOne.await() + numberTwo.await()
            println("并发协程执行结果 $result")
            //2.3 todo 注意：1.不实用await的话，依然会等待所有子协程完成才会结束外部协程；2.若不使用await（包括同类），async的子协程内部异常的话，不会有logcat和try catch，依然会导致外部协程崩溃取消；
            //todo 如果使用了await则会将异常再次抛出。
            listOf(numberOne, numberOne).awaitAll()//如果async的结果放在集合里，会有awaitAll函数可用。
            val deffer =
                async(start = CoroutineStart.LAZY) { println("只有显式的调用该async时候，才会执行") }
            //调用此，才会让上面的执行，因为使用了Lazy
            deffer.await()
            /*
            DEFAULT 立即执行
            LAZY 直到Job执行start或者join才开始执行
            ATOMIC 在作用域开始执行之前无法取消
            UNDISPATCHED 不执行任何调度器, 直接在当前线程中执行, 但是会根据第一个挂起函数的调度器切换
             */
            //Deffered 继承自Job ,可以自定义用于协程结果的返回
            val deffered = CompletableDeferred<Int>()
            launch {
                println("测试全局函数的deffered的自定义结果")
                delay(2000)
                deffered.complete(999)
            }
            println("此时能得到一个launch之后返回的deffered的结果值 ${deffered.await()}")
        }
    }

    //一般子协程发生异常，会引起父协程及其内部的其他协程都一起取消执行；但 supervisorScope创建的则不会影响到父协程。

    //endregion

    //todo 结构化并发：协程创建子协程，会限制子协程的生命周期，子协程会承接父协程的上下文。CoroutineContext

    //region 3. CoroutineContext上下文
    @OptIn(InternalCoroutinesApi::class)
    private fun testCorContext() {
        //演示笔记
        runBlocking {
            //3.1 调度器 Dispatchers，如果不指定，就是当前context运行的调度环境
            launch(Dispatchers.Default) {
                println("launch启动一个协程，这个函数可以使用不同的context，withContext一般用于调度")
                withContext(Dispatchers.IO) {
                    println("现在协程被调度到IO的context环境下执行")
                }
            }
            /*
            Dispatchers.Unconfined 不指定线程, 如果子协程切换线程那么接下来的代码也运行在该线程上
            Dispatchers.IO 适用于IO读写
            Dispatchers.Main 根据平台不同而有所差, Android上为主线程
            Dispatchers.Default 默认调度器, 在线程池中执行协程体, 适用于计算操作
             */
            //每个调度器都会有一个immediate的属性，标记要求立即执行。
            launch(Dispatchers.Main.immediate) { println("执行立即") }
            println("如果上面不是immediate的，此输出就会比launch内的输出快。而加了immediate后，launch就相当于同步执行了。需要等它完成，后面才会开始")
            //3.2 通过CoroutineName可以给启动的协程添加命名
            launch(CoroutineName("命名还是一个CoroutineContext的实现类")) { }
            //3.3 挂起 suspend关键标记挂起函数，要求必须在协程或其他挂起函数内调用。
            launch {
                yield()//yield函数可以让当前协程挂起，让出调度资源给其他协程。如果没有其他协程需要，那么会继续执行自己。
            }
        }

        //3.4 Job属于一个协程任务，继承自CoroutineContext
        val aJob = Job()//job可以是自创建的，也可能是launch，的引用
        runBlocking {
            aJob.join()//让job任务加入到当前协程，并等待完成，是阻塞的。，如果不join，协程还是会执行的，只不过可能是异步的
            aJob.onJoin//选择器使用，
            aJob.cancel()//取消当前job
            aJob.cancelAndJoin()//加入job到当前协程，并完成job，而后取消协程job
            aJob.children//所有的子协程的job
            //这个invokeOnCompletion多参数的函数，还是实验性质的，所以IDE需要添加一个标记提示 @OptIn(InternalCoroutinesApi::class)在函数处
            aJob.invokeOnCompletion(onCancelling = true,//如果是true，本job调用cancel的时候，会立即回调handler
                //todo 参见CoroutinesUnitTest测试类jobHandler的测试
                invokeImmediately = true,//如果true，先调用handler代码块，再执行协程的后续，而后返回DisposableHandle，false的话，返回DisposableHandle
                handler = {
                    //job完成时候的回调，也可能是job的协程内部抛异常了。
                }
            )
            aJob.invokeOnCompletion {
                //job的协程完成后，执行这里，这个handler需要job的父协程指定，job自己指定是无效的。
                //如果手动抛出  CancellationException() 也可以。todo invokeOnCompletion不会捕获cancel之外的异常信息。
            }

            //job有三种状态可用
            aJob.isActive
            aJob.isCompleted
            aJob.isCancelled
            //每个协程作用域都有coroutineContext，每个协程都有job对象
            //todo 协程内如占用一些其他资源，而协程可能被其他原因取消结束，所以此时要注意在适当的回调点处理资源释放的问题。比如invokeOnCompletion内
            withContext(NonCancellable) {
                //如此使用NonCancellable，会创建一个无法取消的协程任务
            }
        }

        //3.5 多个CoroutineContext可以相加 + ，会根据累加顺序覆盖部分重叠的内部信息
        runBlocking {
            launch(Dispatchers.IO + SupervisorJob() + coroutineContext + Job() + CoroutineName("多个累加context的launch")) {
                println("上下文累加")
            }
            ThreadLocal<String>().set("哈哈哈😄")
            //ThreadLocal或获取到线程安全的，线程内部的局部变量，作用域为线程内；如果要作用域为协程内，则可以使用asContextElement
            val tce = ThreadLocal<String>().asContextElement("协程内的作用域，传给哪个协程就是哪个")
            launch(tce) {}
            //3.6 withTimeout可以创建一个有设定超时时间的协程，时间到⌛️会抛出cancel
            withTimeout(100) {}

            //GlobalScope生命周期是跟随JVM的，而且是单例模式，不论在哪里创建，它都不会继承别的协程的上下文，自身周期也不受其他影响。
            GlobalScope//没有job，也无法取消，同JVM生命周期
            val gJob = GlobalScope.launch() { }//GlobalScope启动的launch的协程，可以通过job取消协程
            gJob.cancel()
        }
        //3.7 协程异常
        runBlocking {
            // 协程的异常一般会主层向外层抛出，子协程异常，会影响父协程；除非使用Supervisor做限制。CoroutineExceptionHandler来捕获异常，只是捕获信息，而不能阻止协程的取消结束。
            val handler = CoroutineExceptionHandler { coroutineContext, throwable ->
                throwable.suppressed//协程内部可能多个子协程，异常会触发连锁异常的可能，这个就是次生连锁异常的信息，todo 看单元测试的演示
            }
            //handler会比传递给所有子协程，除非子协程单独指定了别的handler。
        }
        //3.8 监督SupervisorJob
        runBlocking {
            //一般使用SupervisorJob/SupervisorScope为的是处理内部的异常不会影响外部，但是必须配合CoroutineExceptionHandler，否则依然上外抛异常
            //todo 如果单使用SupervisorJob给协程，会导致协程作用域与父协程生命周期不一致，所以需要拼加父协程的context
            launch(coroutineContext + SupervisorJob()) {}
            launch(SupervisorJob(coroutineContext[Job])) { }
            //supervisor是阻塞的，会执行完内部协程任务，才会执行后面的
            supervisorScope {
//                error("supervisor不能直接处理异常，只是能处理内部子协程内的异常，所以这个就直接崩了")
                launch {
                    error("supervisor只是能处理内部子协程内的异常，所以这个会被兜住，但是如果没有handler，它就外抛")
                }
            }

            //3.9 异常捕获，一般协程的子协程可以使用CoroutineExceptionHandler处理捕获，supervisor的可以配合handler。
            //async外的await函数需要try catch ，就是其他非协程内函数的原因，可用try catch
        }

    }

    //endregion

    //4.线程安全问题
    private fun threadSafe() {
        //在协程中有Mutex类似于java的lock，更清凉
        runBlocking {
            val mutex = Mutex()
            mutex.withLock {
                //自动加锁，解锁
            }
            mutex.holdsLock(Any())//判断某对象是否被锁者
            mutex.tryLock()
        }
    }


}