package com.beiying.media.image

import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.text.TextUtils
import java.io.*
import java.lang.Exception
import kotlin.math.roundToInt

/**
 * 1、图片加载/读取
 * 2、图片压缩
 *
 * */
class BitmapUtil {
    companion object {
        /**
         * 获取缩放后的本地图片
         *
         * @param filePath 文件路径
         * @param width    宽
         * @param height   高
         * @return
         */
        @JvmStatic
        fun loadBitmapFromFile(filePath: String, width: Int, height: Int): Bitmap? {
            var options: BitmapFactory.Options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(filePath, options)
            var srcWidth: Int = options.outWidth
            var srcHeight: Int = options.outHeight

            var inSampleSize: Int = 1
            if (srcHeight > height || srcWidth > width) {
                inSampleSize = if (srcWidth > srcHeight) {
                    (srcHeight.toFloat() / height.toFloat()).roundToInt()
                } else {
                    (srcWidth.toFloat() / width.toFloat()).roundToInt()
                }
            }
            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            return BitmapFactory.decodeFile(filePath, options)
        }

        /**
         * 获取缩放后的本地图片
         *
         * @param filePath 文件路径
         * @param width    宽
         * @param height   高
         * @return
         */
        @JvmStatic
        fun loadBitmapFromFileDescriptor(filePath: String, width: Int, height: Int): Bitmap? {
            try {
                var fis: FileInputStream = FileInputStream(filePath)
                var options: BitmapFactory.Options = BitmapFactory.Options()
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFileDescriptor(fis.fd, null, options)
                var srcWidth: Float = options.outWidth.toFloat()
                var srcHeight: Float = options.outHeight.toFloat()
                var inSampleSize: Int = 1

                if (srcHeight > height || srcWidth > width) {
                    inSampleSize = if (srcWidth > srcHeight) {
                        Math.round(srcHeight / height)
                    } else {
                        Math.round(srcWidth / width)
                    }
                }

                options.inJustDecodeBounds = false
                options.inSampleSize = inSampleSize

                return BitmapFactory.decodeFileDescriptor(fis.fd, null, options)
            } catch (e: Exception) {
            }
            return null
        }

        /**
         * 从输入流中获取图片
         *
         * @param ins      输入流
         * @param width    宽
         * @param height   高
         * @return
         */
        @JvmStatic
        fun loadBitmapFromInputStream(ins: InputStream, width: Int, height: Int): Bitmap? {
            var options: BitmapFactory.Options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(ins, null, options)
            var srcWidth: Int = options.outWidth
            var srcHeight: Int = options.outHeight

            var inSampleSize: Int = 1
            if (srcHeight > height || srcWidth > width) {
                inSampleSize = if (srcWidth > srcHeight) {
                    (srcHeight.toFloat() / height.toFloat()).roundToInt()
                } else {
                    (srcWidth.toFloat() / width.toFloat()).roundToInt()
                }
            }
            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            return BitmapFactory.decodeStream(ins, null, options)
        }

        /**
         * 从资源文件中获取图片
         * 注意：采用decodeStream代替decodeResource，避免内存消耗
         * @param resources 资源管理器
         * @param resId     资源ID
         * @param width     宽
         * @param height    高
         * @return
         */
        @JvmStatic
        fun loadBitmapFromResource(resources: Resources, resId: Int, width: Int, height: Int): Bitmap? {
            var ins: InputStream = resources.openRawResource(resId)
            var options: BitmapFactory.Options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(ins, null, options)
            var srcWidth: Int = options.outWidth
            var srcHeight: Int = options.outHeight

            var inSampleSize: Int = 1
            if (srcHeight > height || srcWidth > width) {
                inSampleSize = if (srcWidth > srcHeight) {
                    (srcHeight.toFloat() / height.toFloat()).roundToInt()
                } else {
                    (srcWidth.toFloat() / width.toFloat()).roundToInt()
                }
            }
            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            return BitmapFactory.decodeStream(ins, null, options)
        }

        /**
         * 从二进制数据读取图片
         *
         * @param data     二进制数据
         * @param width    宽
         * @param height   高
         * @return
         */
        @JvmStatic
        fun loadBitmapFromByteArray(data: ByteArray, width: Int, height: Int): Bitmap? {
            var options: BitmapFactory.Options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeByteArray(data, 0, data.size, options)
            var srcWidth: Int = options.outWidth
            var srcHeight: Int = options.outHeight

            var inSampleSize: Int = 1
            if (srcHeight > height || srcWidth > width) {
                inSampleSize = if (srcWidth > srcHeight) {
                    (srcHeight.toFloat() / height.toFloat()).roundToInt()
                } else {
                    (srcWidth.toFloat() / width.toFloat()).roundToInt()
                }
            }
            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            return BitmapFactory.decodeByteArray(data, 0, data.size, options)
        }

        /**
         * 从输入流中获取图片
         * @param context       上下文
         * @param filePath      输入流
         * @return
         */
        @JvmStatic
        fun loadBitmapFromAssetsFile(context: Context, filePath: String): Bitmap? {
            var am: AssetManager = context.resources.assets
            var bitmap: Bitmap? = null
            try {
                var ins: InputStream = am.open(filePath)
                bitmap = BitmapFactory.decodeStream(ins);
                ins.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return bitmap
        }

        /**
         * 将图片保存到文件中
         * */
        @JvmStatic
        fun writeBitmapToFile(bitmap: Bitmap, filePath: String, quality: Int) {
            try {
                var desFile = File(filePath)
                var fos: FileOutputStream = desFile.outputStream()
                var bos: BufferedOutputStream = BufferedOutputStream(fos)
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos)
                bos.flush()
                bos.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        /**
         * 质量压缩
         * */
        @JvmStatic
        fun qualityCompressBitmap(bitmap: Bitmap, quality: Int) {
            var outputStream: ByteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        }

        /**
         * 邻近采样压缩
         * */
        @JvmStatic
        fun sampleCompressBitmapWithNNR() {
            var options: BitmapFactory.Options = BitmapFactory.Options()
            options.inSampleSize = 2
            var originBitmap: Bitmap = BitmapFactory.decodeFile("xxx.png")
            var compressBitmap: Bitmap = BitmapFactory.decodeFile("xxx.png", options)
        }

        /**
         * 双线性采样压缩
         * */
        @JvmStatic
        fun sampleCompressBitmapWithBR() {
            var originBitmap: Bitmap = BitmapFactory.decodeFile("xxx.png")
            var compressBitmap: Bitmap = Bitmap.createScaledBitmap(originBitmap, originBitmap.width / 2, originBitmap.height / 2, true)

            var matrix: Matrix = Matrix()
            matrix.setScale(0.5f, 0.5f)
            compressBitmap = Bitmap.createBitmap(originBitmap, 0, 0, originBitmap.width, originBitmap.height, matrix, true)
        }

        /**
         * 读取照片exif信息中的旋转角度
         *
         * @param filePath 照片路径
         * @return角度
         */
        @JvmStatic
        fun getPictureDegree(filePath: String): Int {
            if (TextUtils.isEmpty(filePath)) return 0
            var degree: Int = 0
            try {
                var exifInterface: ExifInterface = ExifInterface(filePath)
                var orientation: Int = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> degree = 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> degree = 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> degree = 270
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
            return degree
        }
    }
}