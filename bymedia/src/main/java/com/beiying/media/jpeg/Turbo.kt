package com.beiying.media.jpeg

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import com.beiying.media.R
import java.io.FileOutputStream
import java.lang.Exception

class Turbo {
    companion object {
        init {
            System.loadLibrary("jpegcompress")
        }
    }

    external fun turboCompress(bitmap: Any, quality: Int)

    fun compress1(context: Context) {

        val bitmap: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_arrow_back_black)

        compress(bitmap, Bitmap.CompressFormat.JPEG, 50, Environment.getExternalStorageDirectory().absolutePath)

        var scaleBitmap: Bitmap = Bitmap.createScaledBitmap(bitmap, 300, 300,  true)

        compress(scaleBitmap, Bitmap.CompressFormat.JPEG, 100, Environment.getExternalStorageDirectory().absolutePath)
        compress(bitmap, Bitmap.CompressFormat.PNG, 100, Environment.getExternalStorageDirectory().absolutePath)
        compress(bitmap, Bitmap.CompressFormat.WEBP, 100, Environment.getExternalStorageDirectory().absolutePath)
    }

    fun compress(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int, path: String) {
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(path)
            bitmap.compress(format, quality, fos)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            fos?.let {
                try {
                    fos.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}