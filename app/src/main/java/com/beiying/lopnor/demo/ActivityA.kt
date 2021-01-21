package com.beiying.lopnor.demo

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.beiying.lopnor.R

class ActivityA: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_a)
        findViewById<TextView>(R.id.activity_a).setOnClickListener {
            startActivity(Intent(this@ActivityA, ActivityB::class.java))
            finish()
        }
    }
}