package com.beiying.lopnor.base.utils

import android.graphics.*
import android.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 *
 * */
class ImageUtil {
    companion object {
        //根据图片文件路径将图片加载到Bitmap
        fun getImageBitmap(
            srcPath: String?,
            maxWidth: Float,
            maxHeight: Float
        ): Bitmap? {
            val newOpts = BitmapFactory.Options()
            newOpts.inJustDecodeBounds = true
            var bitmap = BitmapFactory.decodeFile(srcPath, newOpts)
            newOpts.inJustDecodeBounds = false
            val originalWidth = newOpts.outWidth
            val originalHeight = newOpts.outHeight
            var be = 1f
            if (originalWidth > originalHeight && originalWidth > maxWidth) {
                be = originalWidth / maxWidth
            } else if (originalWidth < originalHeight && originalHeight > maxHeight) {
                be = newOpts.outHeight / maxHeight
            }
            if (be <= 0) {
                be = 1f
            }
            newOpts.inSampleSize = be.toInt()
            newOpts.inPreferredConfig = Bitmap.Config.ARGB_8888
            newOpts.inDither = false
            newOpts.inPurgeable = true
            newOpts.inInputShareable = true
            if (bitmap != null && !bitmap.isRecycled) {
                bitmap.recycle()
            }
            try {
                bitmap = BitmapFactory.decodeFile(srcPath, newOpts)
            } catch (e: OutOfMemoryError) {
                if (bitmap != null && !bitmap.isRecycled) {
                    bitmap.recycle()
                }
                Runtime.getRuntime().gc()
            } catch (e: Exception) {
                Runtime.getRuntime().gc()
            }
            if (bitmap != null) {
                bitmap = rotateBitmapByDegree(bitmap, getBitmapDegree(srcPath))
            }
            return bitmap
        }

        fun rotateBitmapByDegree(
            bm: Bitmap,
            degree: Int
        ): Bitmap? {
            var returnBm: Bitmap? = null
            val matrix = Matrix()
            matrix.postRotate(degree.toFloat())
            try {
                returnBm = Bitmap.createBitmap(
                    bm,
                    0,
                    0,
                    bm.width,
                    bm.height,
                    matrix,
                    true
                )
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
            }
            if (returnBm == null) {
                returnBm = bm
            }
            if (bm != returnBm) {
                bm.recycle()
            }
            return returnBm
        }

        fun getBitmapDegree(path: String?): Int {
            var degree = 0
            if (path == null) return degree
            try {
                val exifInterface = ExifInterface(path)
                val orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                degree = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return degree
        }
        fun compressBitmapFileTargetSize(file: File, targetSize: Long) {
            if (file.length() > targetSize) {
                val ratio = 2
                val options: BitmapFactory.Options = BitmapFactory.Options()
                val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
                var targetWidth = options.outWidth / ratio
                var targetHeight = options.outHeight / ratio

                var baos: ByteArrayOutputStream = ByteArrayOutputStream()
                val quality = 100
                var result: Bitmap = generateScaleBitmap(bitmap, targetWidth, targetHeight, baos, quality)
                //计数保护，防止次数太多太耗时
                var count: Int = 0
                while(baos.size() > targetSize && count <=10) {
                    targetWidth /= ratio
                    targetHeight /= ratio
                    count++

                    baos.reset()
                    result = generateScaleBitmap(result, targetWidth, targetHeight, baos, quality)
                }
                try {
                    var fos: FileOutputStream = FileOutputStream(file)
                    fos.write(baos.toByteArray())
                    fos.flush()
                    fos.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun generateScaleBitmap(srcBmp: Bitmap, targetWidth: Int, targetHeight: Int, baos: ByteArrayOutputStream, quality: Int): Bitmap {
            val result: Bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas: Canvas = Canvas(result)
            val rect: Rect = Rect(0, 0, result.width, result.height)
            canvas.drawBitmap(srcBmp, null, rect, null)
            if (!srcBmp.isRecycled) {
                srcBmp.recycle()
            }
            result.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            return result
        }

        fun tranformGray(src: Bitmap): Bitmap {
            val dst: Bitmap = Bitmap.createBitmap(src.width, src.height, src.config)
            val canvas: Canvas = Canvas(dst)
            val paint:Paint = Paint()
            paint.isDither = true
            paint.isAntiAlias = true

            val colorMatrix: ColorMatrix = ColorMatrix()
            colorMatrix.setSaturation(0f)
            paint.colorFilter = ColorMatrixColorFilter(colorMatrix)

            canvas.drawBitmap(src, 0f,0f, paint)
            return dst;
        }
    }
}