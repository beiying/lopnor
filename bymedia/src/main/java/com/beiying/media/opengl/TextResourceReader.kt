package com.beiying.media.opengl

import android.content.Context
import android.content.res.Resources
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader

class TextResourceReader {
    companion object {
        fun readTextFileFromResource(context: Context, resourceId: Int) : String {
            val body = StringBuilder()
            val inputStream = context.resources.openRawResource(resourceId)
            val inputStreamReader = InputStreamReader(inputStream)
            val bufferedReader = BufferedReader(inputStreamReader)
            try {
                var nextLine: String = bufferedReader.readLine()
                while (nextLine!= null) {
                    body.append(nextLine)
                    body.append("\n")
                    nextLine = bufferedReader.readLine()
                }
            } catch(e: Exception) {
                e.printStackTrace()
            }

            return body.toString()
        }

        fun readTextFileFromAsset(context: Context, fileName: String): String? {
            return readTextFileFromAsset(context.resources, fileName)
        }

        fun readTextFileFromAsset(res: Resources, fileName: String): String? {
            var result: String? = null
            try {
                val inputStream = res.assets.open(fileName)
                var ch: Int = 0
                val baos = ByteArrayOutputStream()
                ch = inputStream.read()
                while(ch != -1) {
                    baos.write(ch)
                    ch = inputStream.read()
                }
                val buff = baos.toByteArray()
                baos.close()
                inputStream.close()
                result = String(buff, Charsets.UTF_8)
                result = result?.replace("\\r\\n", "\n")
            }catch (e:Exception) {
                e.printStackTrace()
            }
            return result
        }
    }
}