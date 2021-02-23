package com.beiying.lopnor.base.concurrent

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.IntRange
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock

/**
 * 支持任务的优先级去执行，支持线程池暂停、恢复（批量文件下载、上传），异步结果主动回调主线程
 * todo 线程池能力监控 耗时任务检测，定时，延迟
 */
object ByExecutor {
    private var isPaused: Boolean = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var executor: ThreadPoolExecutor
    private var lock: ReentrantLock
    private var pauseCondition: Condition
    init {
        val CPU_COUNT:Int = Runtime.getRuntime().availableProcessors()
        val CORE_POOL_SIZE: Int = CPU_COUNT + 1
        val MAX_POOL_SIZE: Int = CPU_COUNT * 2 + 1
        val KEEP_ALIVE_TIME: Long = 60
        val UNIT = TimeUnit.SECONDS
        val blockingQueue: PriorityBlockingQueue<out Runnable> = PriorityBlockingQueue()
        lock = ReentrantLock()
        pauseCondition = lock.newCondition()

        val seq: AtomicLong = AtomicLong()
        val threadFactory: ThreadFactory = ThreadFactory {
            val thread = Thread(it)
            thread.name = "byexecutor-${seq.getAndIncrement()}"
            return@ThreadFactory thread
        }

        executor =object : ThreadPoolExecutor(CORE_POOL_SIZE,
            MAX_POOL_SIZE, KEEP_ALIVE_TIME, UNIT, blockingQueue as BlockingQueue<Runnable>, threadFactory) {
            override fun beforeExecute(t: Thread?, r: Runnable?) {
                super.beforeExecute(t, r)
                if (isPaused) {
                    try {
                        lock.lock()
                        pauseCondition.await()
                    } finally {
                        lock.unlock()
                    }
                }
            }

            override fun afterExecute(r: Runnable?, t: Throwable?) {
                super.afterExecute(r, t)
            }
        }
    }


    /**
     * 不关注执行结果的异步任务
     * */
    fun execute(@IntRange(from = 0, to = 10)priority: Int, runnable: Runnable) {
        executor.execute(PriorityRunnable(priority,  runnable))
    }

    /**
     * 需要处理任务结果的异步任务
     * */
    fun execute(@IntRange(from = 0, to = 10)priority: Int, runnable: Callable<*>) {
        executor.execute(PriorityRunnable(priority, runnable))
    }

    /**
     * 暂停线程池
     * */
    @Synchronized
    fun pause() {
        isPaused = true
        Log.e("liuyu", "线程池暂停了")
    }

    /**
     * 恢复线程池
     * */
    @Synchronized
    fun resume() {
        isPaused = false
        Log.e("liuyu", "线程池恢复了")
        try {
            lock.lock()
            pauseCondition.signalAll()
        } finally {
            lock.unlock()
        }
    }

    class PriorityRunnable(val priority: Int, var runnable: Runnable): Runnable, Comparable<PriorityRunnable> {
        override fun run() {
            runnable.run()
        }

        override fun compareTo(other: PriorityRunnable): Int {
            return if (this.priority < other.priority) 1 else if(this.priority > other.priority) -1 else 0
        }

    }

    abstract class Callable<T>: Runnable {
        override fun run() {
            mainHandler.post { onPrepare() }

            val t = onBackground()

            mainHandler.post { onComplete(t) }
        }

        open fun onPrepare() {

        }

        abstract fun onBackground(): T

        abstract fun onComplete(t: T)
    }

}