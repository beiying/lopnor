package com.beiying.lopnor.demo.concurrent

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.beiying.lopnor.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.lang.IllegalArgumentException
import java.util.concurrent.TimeUnit

class CoroutinesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coroutines)
        //协程结合lifecycle使用
        lifecycleScope.launch {
        }
    }

    fun createFlow(): Flow<Int> = flow {
        for (i in 1..10)
            emit(i)
    }

    /**
     * flow优化输入框的搜索
     * */
    fun searchFilter() {
        //一个状态容器式可观察数据流,可以向其收集器发出当前状态更新和新状态更新
        val stateFlow = MutableStateFlow("")
        stateFlow.debounce(400)
            .filter {
                it.isNotEmpty()
            }.flatMapLatest {
                getFlowList(it)
            }.catch {
//                Log.e("liuyu", it.message)
            }.onEach {
                Log.e("liuyu", "输出 $it")
            }.flowOn(Dispatchers.Main)
            .launchIn(lifecycleScope)
    }

    //模拟请求数据
    suspend fun getFlowList(str: String): Flow<List<String>> {
        return flow{
            emit(listOf<String>("$str 01", "$str 02", "$str 03"))
        }
    }

    /**
     * 使用flow倒计时
     * */
    fun countDownCoroutines(
        total: Int, onTick: (Int) -> Unit,
        onFinish: () -> Unit, scope: CoroutineScope
    ): Job {
        return flow {
            for (i in total downTo 0) {
                emit(i)
                delay(1000)
            }
        }.flowOn(Dispatchers.Default)
            .onCompletion { onFinish() }
            .flowOn(Dispatchers.Main)
            .launchIn(scope)

    }

    /**
     * 通过扩展函数防抖动处理
     * */
    inline fun View.setThrottleListener(
        delayMillis: Long = 1000L,
        crossinline onClick: () -> Unit
    ) {
        this.setOnClickListener {
            this.isClickable = false
            onClick()
            this.postDelayed({
                this.isClickable = true
            }, delayMillis)

        }
    }

    /**
     * 通过扩展函数简化EditText的输入
     * */
    inline fun TextView.doBeforeTextChanged(crossinline action: (text: CharSequence?, start: Int, before: Int, count: Int) -> Unit) =
        addTextChangedListener(beforeTextChanged = action)

    inline fun TextView.doOnTextChanged(
        crossinline action: (
            text: CharSequence?,
            start: Int,
            before: Int,
            count: Int
        ) -> Unit
    ) = addTextChangedListener(onTextChanged = action)

    inline fun TextView.doAfterTextChanged(
        crossinline action: (text: Editable?) -> Unit
    ) = addTextChangedListener(afterTextChanged = action)


    inline fun TextView.addTextChangedListener(crossinline beforeTextChanged: (text: CharSequence?, start: Int, before: Int, after: Int) -> Unit,
    crossinline onTextChanged: (text: CharSequence?, start: Int, before: Int, count: Int) -> Unit, crossinline afterTextChanged: (text: Editable?) -> Unit) {
        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                afterTextChanged.invoke(s)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                beforeTextChanged.invoke(s, start, count, after)
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                onTextChanged.invoke(text, start, before, count)
            }
        }
        this.addTextChangedListener(textWatcher)
    }

    /**
     * Retrofit集成协程的用法
     * */

    inner class CoroutinesVm: ViewModel() {
        fun requestData() {
            //协程结合ViewModel组件使用，协程会在ViewModel被onCleared的时候自动取消
            viewModelScope.launch {
                request {
                    Api.getListProject()
                }.next {
                    Log.e("data===>", "data=====>${this.data}")
                }
            }
        }

        suspend fun <T> request(showLoading: Boolean = false, request: suspend ApiInterface.() -> BaseResponse<T>?): BaseResponse<T> {
            return try {
                requestSimple(request)
            } catch(e: java.lang.Exception) {
                BaseResponse<T>().apply {
                    exception = e
                }
            } finally {

            }
        }

        //协程切换到IO线程执行网络请求
        suspend fun <T> requestSimple(realRequest: suspend ApiInterface.() -> BaseResponse<T>?): BaseResponse<T> {
            val response = withContext(Dispatchers.IO) {
                realRequest(Api)
            } ?: throw IllegalArgumentException("数据非法")
            return response
        }
    }

    inner class BaseResponse<T> {
        val data: T? = null
        val errorCode: Int? = null
        val errorMsg: String? = null
        var exception: java.lang.Exception? = null


    }

    inline fun <T> BaseResponse<T>.next(block: BaseResponse<T>.() -> Unit): BaseResponse<T> {
        return if (exception == null) {
            block()
            this
        } else {
            this
        }
    }

    interface ApiInterface {
        @GET("/article/listproject/0/json")
        suspend fun getListProject(): BaseResponse<Int>
    }

    val Api: ApiInterface by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.wanandroid.com")
            .addConverterFactory(GsonConverterFactory.create())
            .client(getOkHttpClient())
            .build().create(ApiInterface::class.java)
    }

    private fun getOkHttpClient(): OkHttpClient {
        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS) //设置读取超时时间
            .writeTimeout(30, TimeUnit.SECONDS) //设置写的超时时间
            .connectTimeout(30, TimeUnit.SECONDS)
        val httpLoggingInterceptor = HttpLoggingInterceptor()
        builder.addInterceptor(httpLoggingInterceptor.apply {
            httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        })
        return builder.build()
    }

}