package com.beiying.lopnor.demo

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.beiying.common.flutter.ByFlutterFragment
import com.beiying.lopnor.R
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class ActivityA: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_a)
//        supportFragmentManager.beginTransaction().add(R.id.fragment_container,
//            ByFlutterFragment()
//        ).commit()
        findViewById<TextView>(R.id.activity_a).setOnClickListener {
            startActivity(Intent(this@ActivityA, ActivityB::class.java))
            finish()
        }
        val client: OkHttpClient = OkHttpClient()

        var request: Request = Request.Builder()
            .url("")
            .build();

    }
}