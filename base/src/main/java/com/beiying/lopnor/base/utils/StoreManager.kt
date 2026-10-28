package com.beiying.lopnor.base.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.*
import java.util.*

/**
 * 四类数据和文件存储：
 *      应用专属存储空间：
 *          存储仅供应用使用的文件，可以存储到 "内部存储卷" 中的专属目录或 "外部存储空间" 中的其他专属目录。
 *          使用内部存储空间中的目录保存其他应用不应访问的敏感信息，且这些目录的空间通常比较小，一个目录(getFilesDir方法)保存应用的持久性文件，另一个目录(getCacheDir方法)保存应用的缓存文件
 *          如果用户卸载应用，系统会移除保存在应用专属存储空间中的文件
 *      共享存储：
 *          存储您的应用打算与其他应用共享的文件，包括媒体、文档和其他文件。
 *          访问方法：MediaStore API, Android 10 开始对共享目录进行了权限详细的划分，不再能通过"绝对路径"访问。
 *          如果用户卸载应用，系统不会会移除保存在共享存储中的文件
 *      偏好设置：以键值对形式存储私有原始数据。
 *      数据库：使用 Room 持久性库或其他三方库将结构化数据存储在专用数据库中。
 *
 * 选择何种存储保存数据的依据：
 *      1）您的数据需要占用多少空间：
 *          内部存储空间中用于存储应用专属数据的空间有限。如果您需要保存大量数据，请使用其他类型的存储空间。
 *      2）数据访问需要达到怎样的可靠程度：
 *          如果应用的基本功能需要某些数据（例如应用启动时需要的数据），可以将相应数据存放到内部存储目录或数据库中。
 *          存储在外部存储空间中的应用专属文件并非一直可以访问，因为有些设备允许用户移除提供外部存储空间的实体设备。
 *      3）您需要存储哪类数据：
 *          如果数据仅供您的应用使用，应使用应用专属存储空间。对于可分享的媒体内容，应使用共享的存储空间，以便其他应用可以访问相应内容。
 *          对于结构化数据，应使用偏好设置（适合键值对数据）或数据库（适合包含 2 个以上列的数据）。
 *      4）数据是否应仅供您的应用使用：
 *          在存储敏感数据（不可通过任何其他应用访问的数据）时，应使用内部存储空间、偏好设置或数据库。内部存储空间的一个额外优势是用户无法看到相应数据。
 * */
class StoreManager {
    companion object {
        @JvmStatic
        fun savePictureFile(context: Context, file: File): Boolean {
            var uri: Uri? = insertPictureFileIntoMediaStore(context, file)
            Log.e("liuyu", "savePictureFile uri=${uri.toString()}")
            return saveFile(context, file, uri)
        }

        /**
         * 将bitmap保存到共享目录下
         * */
        @JvmStatic
        fun saveBitmapToPictureFile(context: Context, bitmap: Bitmap, fileName: String, format: Bitmap.CompressFormat, quality: Int): Boolean {
            var uri: Uri? = insertPictureIntoMediaStore(context, fileName)
            Log.e("liuyu", "insertPictureIntoMediaStore uri=${uri.toString()}")
            return saveBitmap(context, bitmap, uri, format, quality)
        }

        /**
         * 将bitmap保存到私有目录下
         * */
        @JvmStatic
        fun saveBitmapToPrivatePictureFile(context: Context, bitmap: Bitmap, fileName: String, format: Bitmap.CompressFormat, quality: Int): String {
            val suffix = "." + format.toString().lowercase(Locale.getDefault())
            var filePath = "${context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath}$fileName$suffix"
            Log.e("liuyu", "saveBitmapToPrivatePictureFile path=$filePath")
            var file: File = File(filePath)
            if (saveBitmapByFile(context, bitmap, file, format, quality)) {
                return file.absolutePath
            }
            return ""
        }

        @JvmStatic
        fun saveFile(context: Context, file: File, uri: Uri?): Boolean {
            if (uri == null) {
                return false
            } else {
                val contentResolver: ContentResolver = context.contentResolver
                var outputStream: OutputStream? = null
                try {
                    outputStream = contentResolver.openOutputStream(uri, "w")
                } catch (exception: FileNotFoundException) {
                    exception.printStackTrace()
                }
                if (outputStream == null) {
                    return false
                } else {
                    var inputStream: FileInputStream? = null
                    return try {
                        inputStream = FileInputStream(file)
                        copy(inputStream, outputStream)
                        true
                    } catch (fileException: FileNotFoundException) {
                        fileException.printStackTrace()
                        try {
                            outputStream.close()
                            inputStream?.close()
                        } catch (ioException: IOException) {
                            ioException.printStackTrace()
                        }
                        false
                    }
                }
            }

        }

        @JvmStatic
        fun saveBitmap(context: Context, bitmap: Bitmap, uri: Uri?, format: Bitmap.CompressFormat, quality: Int): Boolean {
            if (uri == null) {
                return false
            } else {
                val contentResolver: ContentResolver = context.contentResolver
                var outputStream: OutputStream? = null
                try {
                    outputStream = contentResolver.openOutputStream(uri, "w")
                    return if (outputStream == null) {
                        false
                    } else {
                        if (bitmap.isRecycled) {
                            false
                        } else {
                            var ret = bitmap.compress(format, quality, outputStream)
                            bitmap.recycle()
                            ret
                        }
                    }
                } catch (exception: FileNotFoundException) {
                    exception.printStackTrace()
                    return false
                }
            }
        }

        @JvmStatic
        fun saveBitmapByFile(context: Context, bitmap: Bitmap, file: File?, format: Bitmap.CompressFormat, quality: Int): Boolean {
            if (file == null) {
                return false
            } else {
                val contentResolver: ContentResolver = context.contentResolver
                var fos: FileOutputStream? = null
                try {
                    fos = FileOutputStream(file)
                    return if (fos == null) {
                        false
                    } else {
                        if (bitmap.isRecycled) {
                            false
                        } else {
                            var ret = bitmap.compress(format, quality, fos)
                            bitmap.recycle()
                            fos.flush()
                            fos.close()
                            ret
                        }
                    }
                } catch (exception: FileNotFoundException) {
                    exception.printStackTrace()
                    fos?.close()
                    return false
                }
            }
        }

        @JvmStatic
        fun insertPictureFileIntoMediaStore(context: Context, file: File): Uri? {
            val contentValues = ContentValues()
            val pictureCollection =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(
                        MediaStore.VOLUME_EXTERNAL
                    )
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
            contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
            contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            contentValues.put(MediaStore.Images.Media.DATE_TAKEN, file.lastModified())
            var uri: Uri? = null
            try {
                uri = context.contentResolver.insert(pictureCollection, contentValues)
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
            return uri
        }

        @JvmStatic
        fun insertPictureIntoMediaStore(context: Context, fileName: String): Uri? {
            val contentValues = ContentValues()
            val pictureCollection =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(
                        MediaStore.VOLUME_EXTERNAL
                    )
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
            contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            var uri: Uri? = null
            try {
                uri = context.contentResolver.insert(pictureCollection, contentValues)
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
            return uri
        }

        @JvmStatic
        fun copy(input: InputStream, output: OutputStream) {
            var buffer: ByteArray = ByteArray(4096)
            while(true) {
                var count = input.read(buffer)
                if (count == -1) {
                    output.flush()
                    return
                }
                output.write(buffer, 0, count)
            }
        }
    }
}