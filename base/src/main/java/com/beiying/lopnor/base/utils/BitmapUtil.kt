package com.beiying.lopnor.base.utils

import android.graphics.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class BitmapUtil {
    companion object {
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