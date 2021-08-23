package com.beiying.lopnor.demo

import android.content.res.AssetManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.beiying.lopnor.R
import kotlinx.coroutines.*

class ActivityCoroutine: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coroutine)
        findViewById<TextView>(R.id.startScene1).setOnClickListener {
            startScene1()
        }
        findViewById<TextView>(R.id.startScene2).setOnClickListener {
            startScene2()
        }
    }


    fun startScene1() {
        val job: Job = GlobalScope.launch(Dispatchers.IO) {
            Log.e("liuyu", "coroutines running")
            val result1 = request1()
            val result2 = request2(result1)
            val result3 = request3(result2)

            updateUI(result3)
        }
        Log.e("liuyu","startScene1 coroutines launched")
    }

    fun startScene2() {
        val job: Job = GlobalScope.launch {
            Log.e("liuyu", "corountines is running")
            val result = request1()
            val deffered1 = GlobalScope.async { request2(result) }
            val deffered2 = GlobalScope.async { request3(result) }

            updateUI(deffered1.await(), deffered2.await())
        }
        Log.e("liuyu","startScene2 coroutines launched")
    }

    fun startScene3(assetManager: AssetManager) {
        GlobalScope.launch {
//            val content: String = parseAssetFile(assetManager, "")
        }
    }

    fun startScene4() {
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

    private fun updateUI(result1: String, result2: String) {
        Log.e("liuyu", "update async work on ${Thread.currentThread().name}, result= $result1 $result2")
    }

    suspend fun request1(): String {
        delay(2 * 1000)
        Log.e("liuyu", "request1 work on ${Thread.currentThread().name}")
        return "result from request1"
    }

    suspend fun request2(result: String): String {
        Log.e("liuyu", "request2: $result")
        delay(2 * 1000)
        Log.e("liuyu", "request2 work on ${Thread.currentThread().name}")
        return "hello"
    }

    suspend fun request3(result: String): String {
        Log.e("liuyu", "request3: $result")
        delay(2 * 1000)
        Log.e("liuyu", "request3 work on ${Thread.currentThread().name}")
        return "world"
    }
}