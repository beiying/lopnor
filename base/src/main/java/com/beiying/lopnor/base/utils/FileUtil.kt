package com.beiying.lopnor.base.utils

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.*

class FileUtil {
    data class Video(val uri: Uri, val name: String, val duration: Int, val  size: Int)


    companion object {
        /**
         * val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
        val projection = arrayOf(MediaStore.Video.Media._ID,MediaStore.Video.Media.DISPLAY_NAME,MediaStore.Video.Media.DURATION,MediaStore.Video.Media.SIZE)
        val selection = "${MediaStore.Video.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES).toString())
        val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"
         * */

        @JvmStatic
        fun getAllVideo(context: Context, collection: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): List<Video> {
            val videoList = mutableListOf<Video>()
            val query = context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )
            query?.use { cursor ->
                // Cache column indices.
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val nameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)

                while (cursor.moveToNext()) {
                    // Get values of columns for a given video.
                    val id = cursor.getLong(idColumn)
                    val data = cursor.getString(dataColumn)
                    val name = cursor.getString(nameColumn)
                    val duration = cursor.getInt(durationColumn)
                    val size = cursor.getInt(sizeColumn)

                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    Log.e("liuyu", "video id=$id, data=$data, uri=${contentUri.toString()}")
                    var file: File = File(data)
                    Log.e("liuyu", "file exists=${file.exists()}")
                    videoList += Video(contentUri, name, duration, size)
                }
            }
            return videoList
        }

        fun createTempFile(context: Context, fileName: String, suffix: String?): File {
            return File.createTempFile(fileName, suffix, context.cacheDir)
        }
        /**
         * 图片保存：
         *      一般应用内需要保存的图片都是为了能够在其他应用中使用，因此一般图片保存在系统共享存储目录下
         * */
        fun savePicture(context: Context, bm: Bitmap?, fileName: String?, needToast: Boolean): String? {
            if (null == bm) {
                return ""
            }
            //建立指定文件夹
            val foder = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "dajia")
            if (!foder.exists()) {
                foder.mkdirs()
            }
            val myCaptureFile = File(foder, fileName)
            try {
                if (!myCaptureFile.exists()) {
                    myCaptureFile.createNewFile()
                }
                val bos = BufferedOutputStream(FileOutputStream(myCaptureFile))
                //压缩保存到本地
                bm.compress(Bitmap.CompressFormat.PNG, 90, bos)
                bos.flush()
                bos.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            // 把文件插入到系统图库
            try {
                MediaStore.Images.Media.insertImage(
                    context.contentResolver,
                    myCaptureFile.absolutePath, fileName, null
                )
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
            // 最后通知图库更新
            context.sendBroadcast(
                Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.parse("file://" + myCaptureFile.path)
                )
            )
            return myCaptureFile.absolutePath
        }

        fun getExternalCacheDir(context: Context): File? {
            val dataDir = File(File(Environment.getExternalStorageDirectory(), "DICM"), "data")
            val appCacheDir = File(File(dataDir, context.packageName), "cachess")
            if (!dataDir.exists()) {
                if (!dataDir.mkdirs()) {
                    Log.e("liuyu", "Unable to create external cache directory")
                    return null
                }
            }
            return dataDir
        }
    }


}