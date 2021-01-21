package com.beiying.lopnor.demo

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.beiying.lopnor.R

class ActivityC: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_c)
        findViewById<TextView>(R.id.activity_c).setOnClickListener {
            startActivities(
                arrayOf(
                    Intent("com.beiying.lopnor.action.Main").setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    , Intent(this@ActivityC, ActivityA::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)))

        }
    }
}