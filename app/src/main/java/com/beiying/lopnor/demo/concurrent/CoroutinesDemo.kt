package com.beiying.lopnor.demo.concurrent

import android.content.res.AssetManager
import android.util.Log
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.StringBuilder
import kotlin.coroutines.suspendCoroutine

/**
 * 协程demo
 * */
class  CoroutinesDemo {

    fun startScene() {
        val job: Job = GlobalScope.launch(Dispatchers.IO) {
            Log.e("liuyu", "coroutines running")
            val result1 = request1()
            val result2 = request2(result1)
            val result3 = request3(result2)

            updateUI(result3)
        }
        Log.e("liuyu","coroutines launched")
//        val deffered: Deferred = GlobalScope.async(Dispatchers.IO)
    }

    fun startScene1() {
        val job: Job = GlobalScope.launch {
            Log.e("liuyu", "corountines is running")
            val result = request1()
            val deffered1 = GlobalScope.async { request2(result) }
            val deffered2 = GlobalScope.async { request3(result) }

            updateUI(deffered1.await(), deffered2.await())
        }
    }

    fun startScene2(assetManager: AssetManager) {
        GlobalScope.launch {
            val content: String = parseAssetFile(assetManager, "")
        }
    }

    fun startScene3() {
        runBlocking {
            delay(100)
            println("world")
//            launch {
//                delay(100)
//                println("world")
//            }
        }
        println("hello")
    }

    private fun updateUI(result: String) {
        Log.e("liuyu", "update work on ${Thread.currentThread().name}")
    }

    private fun updateUI(result: String, result2: String) {
        Log.e("liuyu", "update async work on ${Thread.currentThread().name}")
    }

    suspend fun request1(): String {
        delay(2 * 1000)

        Log.e("liuyu", "request1 work on ${Thread.currentThread().name}")
        return "result from request1"
    }

    suspend fun request2(result: String): String {
        delay(2 * 1000)

        Log.e("liuyu", "request1 work on ${Thread.currentThread().name}")
        return "result from request2"
    }

    suspend fun request3(result: String): String {
        delay(2 * 1000)

        Log.e("liuyu", "request1 work on ${Thread.currentThread().name}")
        return "result from request3"
    }

    suspend fun parseAssetFile(assetManager: AssetManager, fileName: String): String {
        suspendCoroutine<String> { continuation ->

        }
        return suspendCancellableCoroutine { continuation ->
            Thread(Runnable {
                val inputStream = assetManager.open(fileName)
                val bufferReader = BufferedReader(InputStreamReader(inputStream))
                var line: String
                val stringBuilder: StringBuilder = StringBuilder()
                do {
                    line = bufferReader.readLine()
                    if (line != null) stringBuilder.append(line) else break
                } while (line != null)
                inputStream.close()
                bufferReader.close()

                continuation.resumeWith(Result.success(stringBuilder.toString()))
            }).start()
        }
    }

    fun main() = runBlocking {
        doWorld()
        println("Done")
    }

    // Concurrently executes both sections
    suspend fun doWorld() = coroutineScope { // this: CoroutineScope
        launch {
            delay(2000L)
            println("World 2")
        }
        launch {
            delay(1000L)
            println("World 1")
        }
        println("Hello")
    }


}