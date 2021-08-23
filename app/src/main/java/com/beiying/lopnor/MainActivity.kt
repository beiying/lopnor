package com.beiying.lopnor

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.beiying.lopnor.demo.ActivityA
import com.beiying.lopnor.demo.ActivityC
import com.beiying.lopnor.demo.ActivityCoroutine
import com.beiying.lopnor.demo.navigation.NavigationActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import org.w3c.dom.Text

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        findViewById<TextView>(R.id.hello_world).setOnClickListener {

            startActivity(Intent(this@MainActivity, ActivityC::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        findViewById<TextView>(R.id.navigation_demo).setOnClickListener {
            startActivity(Intent(this@MainActivity, NavigationActivity::class.java))
        }

        findViewById<TextView>(R.id.coroutine_demo).setOnClickListener {
            startActivity(Intent(this@MainActivity, ActivityCoroutine::class.java))
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
