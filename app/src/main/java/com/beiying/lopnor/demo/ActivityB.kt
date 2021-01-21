package com.beiying.lopnor.demo

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.beiying.lopnor.R

class ActivityB: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_b)
        findViewById<TextView>(R.id.activity_b).setOnClickListener {
            finish()
        }

    }
}