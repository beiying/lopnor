package com.beiying.lopnor

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.beiying.lopnor.base.utils.FileUtil
import com.beiying.lopnor.base.utils.StoreManager
import com.beiying.lopnor.demo.ActivityC
import com.beiying.lopnor.demo.ActivityCoroutine
import com.beiying.lopnor.demo.navigation.NavigationActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.snackbar.Snackbar
import com.tbruyelle.rxpermissions3.RxPermissions
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*

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

        Log.e("liuyu", "getFilesDir path=${filesDir.absolutePath}")
        Log.e("liuyu", "getCacheDir path=${cacheDir.absolutePath}")

        Log.e("liuyu", "getFilesDir path=${getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath}")
        Log.e("liuyu", "getFilesDir path=${getExternalFilesDir(Environment.DIRECTORY_ALARMS)?.absolutePath}")
        Log.e("liuyu", "getFilesDir path=${getExternalFilesDir(Environment.DIRECTORY_DCIM)?.absolutePath}")
        Log.e("liuyu", "getFilesDir path=${getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.absolutePath}")
        Log.e("liuyu", "getFilesDir path=${getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath}")
        Log.e("liuyu", "getFilesDir path=${getExternalFilesDir(Environment.DIRECTORY_MOVIES)?.absolutePath}")
        Log.e("liuyu", "getFilesDir path=${getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath}")
        Log.e("liuyu", "getFilesDir path=${getExternalFilesDir(Environment.DIRECTORY_NOTIFICATIONS)?.absolutePath}")
        Log.e("liuyu", "getFilesDir path=${getExternalFilesDir(Environment.DIRECTORY_PODCASTS)?.absolutePath}")
        Log.e("liuyu", "getFilesDir path=${externalMediaDirs[0].absolutePath}")
        Log.e("liuyu", "getCacheDir path=${externalCacheDir?.absolutePath}")
        Log.e("liuyu", "getCanonicalPath path=${externalCacheDir?.canonicalPath}")
        Log.e("liuyu", "getExternalStorageDirectory path=${Environment.getExternalStorageDirectory()?.absolutePath}")
        Log.e("liuyu", "getExternalStoragePublicDirectory path=${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)?.absolutePath}")

        var sdcardPath = Environment.getExternalStoragePublicDirectory("dajia")?.absolutePath
        val file: File = File("$sdcardPath/download")
        Log.e("liuyu", "sdcardPath=$sdcardPath  file exists=${file.exists()}  mkdirs=${file.mkdirs()}")
        if (!file.exists()) file.mkdirs()

        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
        val projection = arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.DURATION,MediaStore.Video.Media.SIZE)
        val selection = "${MediaStore.Video.Media.DURATION} >= ?"
        val selectionArgs = arrayOf("1000")
        val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"

//        FileUtil.getAllVideo(this, collection, projection, selection, selectionArgs, sortOrder)
        Log.e("liuyu", "FileUtil.getExternalCacheDir path=${FileUtil.getExternalCacheDir(this)?.absolutePath}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Log.e("liuyu", "Environment.isExternalStorageLegacy=${Environment.isExternalStorageLegacy()}")
        }

        val picturePath =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath
        var imageFile: File = File("$picturePath/dajiazhongyi/${System.currentTimeMillis()}.jpg")

//        if (!imageFile.exists()) {
//            var rxPermissions = RxPermissions(this)
//            rxPermissions.requestEach(Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                Manifest.permission.READ_EXTERNAL_STORAGE)
//                .subscribe { permission ->
//                    if (permission.name == Manifest.permission.WRITE_EXTERNAL_STORAGE && permission.granted) {
//                        val result: Boolean = imageFile.createNewFile()
//                        Log.e("liuyu", "createNewFile result=$result")
//                    }
//                }
//
//        }

        var rxPermissions = RxPermissions(this)
        rxPermissions.requestEach(Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE)
            .subscribe { permission ->
                if (permission.name == Manifest.permission.WRITE_EXTERNAL_STORAGE && permission.granted) {
//                    StoreManager.savePictureFile(this@MainActivity, File("/storage/emulated/0/Android/data/com.beiying.lopnor/cache/file/kaka.jpg"))
                    Glide.with(this).asBitmap().load("https://nim-nosdn.netease.im/NDA3MTI1Nw==/bmltYV8xMzE1MjY4OTg4OV8xNjcwNDAyOTg1NDMyXzI5YjI5YTliLTcwZTItNDVhMC05OGRiLTk5MWM0NDM3NDBmOQ==?createTime=1670402986685")
                        .into(object : SimpleTarget<Bitmap?>() {
                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: Transition<in Bitmap?>?
                            ) {
                    StoreManager.saveBitmapToPrivatePictureFile(this@MainActivity, resource, "test.jpg", Bitmap.CompressFormat.JPEG, 100)
//                                val picPath: String = "$picturePath/dajiazhongyi/"
//                                val srcFilePath = picPath + System.currentTimeMillis() + ".jpg"
//                                try {
//                                    val srcFile = File(srcFilePath)
//                                    Log.e("liuyu", "srcFilePath=$srcFilePath")
//                                    if (!srcFile.exists()) {
//                                        val result = srcFile.createNewFile()
//                                        Log.e("liuyu", "createNewFile result=$result")
//                                        if (!result) {
//                                            return
//                                        }
//                                    }
//                                    val fout: OutputStream = FileOutputStream(srcFile)
//                                    resource.compress(Bitmap.CompressFormat.JPEG, 90, fout)
//                                    fout.close()
//                                    val values = ContentValues(2)
//                                    values.put(
//                                        MediaStore.Images.Media.MIME_TYPE,
//                                        "image/jpeg"
//                                    )
//                                    values.put(MediaStore.Images.Media.DATA, srcFilePath)
//                                    contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
//                                    this@MainActivity.sendBroadcast(
//                                        Intent(
//                                            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse(
//                                                "file://$srcFilePath"
//                                            )
//                                        )
//                                    )
//                                    Toast.makeText(
//                                        this@MainActivity,
//                                        "保存成功",
//                                        Toast.LENGTH_SHORT
//                                    ).show()
//                                } catch (e: FileNotFoundException) {
//                                    e.printStackTrace()
//                                    Toast.makeText(
//                                        this@MainActivity,
//                                        "保存失败",
//                                        Toast.LENGTH_SHORT
//                                    ).show()
//                                } catch (e: IOException) {
//                                    e.printStackTrace()
//                                    Toast.makeText(
//                                        this@MainActivity,
//                                        "保存失败",
//                                        Toast.LENGTH_SHORT
//                                    ).show()
//                                }
                            }
                        })
                }
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
