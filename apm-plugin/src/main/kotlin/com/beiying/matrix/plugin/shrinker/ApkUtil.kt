package com.beiying.matrix.plugin.shrinker

import com.android.builder.model.SigningConfig
import com.beiying.javalib.common.utils.Util
import org.gradle.api.GradleException
import java.io.*
import java.util.*
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

internal object ApkUtil {
    fun parseResourceId(resId: String): String {
        if (!Util.isNullOrNil(resId) && resId.startsWith("0x")) {
            if (resId.length == 10) {
                return resId
            } else if (resId.length < 10) {
                val strBuilder = StringBuilder(resId)
                for (i in 0 until 10 - resId.length) {
                    strBuilder.append('0')
                }
                return strBuilder.toString()
            }
        }
        return ""
    }

    fun entryToResourceName(entry: String?): String {
        var resourceName = ""
        entry?.let {
            val typeName =
                parseEntryResourceType(
                    entry
                )
            val resName =
                entry.substring(entry.lastIndexOf('/') + 1, entry.indexOf('.'))
            if (!Util.isNullOrNil(typeName) && !Util.isNullOrNil(resName)) {
                resourceName = "R.$typeName.$resName"
            }
        }
        return resourceName
    }

    fun parseEntryResourceType(entry: String): String {
        if (!Util.isNullOrNil(entry) && entry.length > 4) {
            var typeName = entry.substring(4, entry.lastIndexOf('/'))
            if (!Util.isNullOrNil(typeName)) {
                val index = typeName.indexOf('-')
                if (index >= 0) {
                    typeName = typeName.substring(0, index)
                }
                return typeName
            }
        }
        return ""
    }

    fun isSameResourceType(entries: Set<String>): Boolean {
        var resType = ""
        for (entry in entries) {
            if (!Util.isNullOrNil(entry)) {
                if (Util.isNullOrNil(resType)) {
                    resType =
                        parseEntryResourceType(
                            entry
                        )
                    continue
                }
                if (resType != parseEntryResourceType(
                        entry
                    )
                ) {
                    return false
                }
            } else {
                return false
            }
        }
        return !Util.isNullOrNil(resType)
    }

    fun parseResourceType(resource: String): String {
        return resource.substring(resource.indexOf('.') + 1, resource.lastIndexOf('.'))
    }

    fun parseResourceName(resource: String): String {
        return resource.substring(resource.lastIndexOf('.') + 1)
    }

    @Throws(IOException::class)
    fun readFileContent(inputStream: InputStream?): ByteArray {
        val output = ByteArrayOutputStream()
        try {
            val bufferedInput =
                BufferedInputStream(inputStream)
            var len: Int
            val buffer = ByteArray(4096)
            while (bufferedInput.read(buffer).also { len = it } != -1) {
                output.write(buffer, 0, len)
            }
            bufferedInput.close()
        } finally {
            output.close()
        }
        return output.toByteArray()
    }

    @Throws(IOException::class)
    fun unzipEntry(
        zipFile: ZipFile,
        zipEntry: ZipEntry?,
        destFile: String?
    ) {
        val file = File(destFile)
        file.parentFile.mkdirs()
        file.createNewFile()
        val outputStream =
            BufferedOutputStream(FileOutputStream(file))
        val inputStream = zipFile.getInputStream(zipEntry)
        outputStream.write(
            readFileContent(
                inputStream
            )
        )
        outputStream.close()
    }

    @Throws(IOException::class)
    fun addZipEntry(
        zipOutputStream: ZipOutputStream,
        zipEntry: ZipEntry,
        file: File?
    ) {
        val writeEntry = ZipEntry(zipEntry.name)
        val inputStream: InputStream = FileInputStream(file)
        val content = readFileContent(
            inputStream
        )
        if (zipEntry.method == ZipEntry.DEFLATED) {
            writeEntry.method = ZipEntry.DEFLATED
        } else {
            writeEntry.method = ZipEntry.STORED
            val crc32 = CRC32()
            crc32.update(content)
            writeEntry.crc = crc32.value
        }
        writeEntry.size = content.size.toLong()
        zipOutputStream.putNextEntry(writeEntry)
        zipOutputStream.write(content)
        zipOutputStream.flush()
        zipOutputStream.closeEntry()
    }

    @Throws(IOException::class)
    fun addZipEntry(
        zipOutputStream: ZipOutputStream,
        zipEntry: ZipEntry,
        zipFile: ZipFile
    ) {
        val writeEntry = ZipEntry(zipEntry.name)
        val inputStream = zipFile.getInputStream(zipEntry)
        val content = readFileContent(
            inputStream
        )
        if (zipEntry.method == ZipEntry.DEFLATED) {
            writeEntry.method = ZipEntry.DEFLATED
        } else {
            writeEntry.method = ZipEntry.STORED
            writeEntry.crc = zipEntry.crc
            writeEntry.size = zipEntry.size
        }
        zipOutputStream.putNextEntry(writeEntry)
        zipOutputStream.write(content)
        zipOutputStream.flush()
        zipOutputStream.closeEntry()
    }

    @Throws(IOException::class)
    fun addZipEntry(
        zipOutputStream: ZipOutputStream,
        zipEntry: ZipEntry,
        newName: String?,
        zipFile: ZipFile
    ) {
        val writeEntry = ZipEntry(newName)
        val inputStream = zipFile.getInputStream(zipEntry)
        val content = readFileContent(
            inputStream
        )
        if (zipEntry.method == ZipEntry.DEFLATED) {
            writeEntry.method = ZipEntry.DEFLATED
        } else {
            writeEntry.method = ZipEntry.STORED
            writeEntry.crc = zipEntry.crc
            writeEntry.size = zipEntry.size
        }
        zipOutputStream.putNextEntry(writeEntry)
        zipOutputStream.write(content)
        zipOutputStream.flush()
        zipOutputStream.closeEntry()
    }

    @Throws(
        GradleException::class,
        IOException::class,
        InterruptedException::class
    )
    fun sevenZipFile(
        sevenZipPath: String?,
        inputFile: String?,
        outputFile: String?,
        deflated: Boolean
    ) {
        if (!File(sevenZipPath).canExecute()) {
            File(sevenZipPath).setExecutable(true)
        }
        val processBuilder = ProcessBuilder()
        processBuilder.command(
            sevenZipPath,
            "a",
            "-tzip",
            outputFile,
            inputFile,
            if (deflated) "-mx9" else "-mx0"
        )
        //Log.i(TAG, "%s", processBuilder.command())
        val process = processBuilder.start()
        //        process.waitForProcessOutput(System.out, System.err);
//        process.waitFor();
        waitForProcessOutput(process)
        if (process.exitValue() != 0) {
            throw GradleException("7zip apk occur error!")
        }
    }

    @Throws(InterruptedException::class, IOException::class)
    fun waitForProcessOutput(process: Process) {
        process.waitFor()
        val bytes = ByteArray(1024)
        while (process.inputStream.read(bytes) > 0) {
            System.out.write(bytes)
        }
        while (process.errorStream.read(bytes) > 0) {
            System.err.write(bytes)
        }
        return
    }

    @Throws(IOException::class)
    fun readResourceTxtFile(
        resTxtFile: File?,
        resourceMap: MutableMap<String, Int>,
        styleableMap: MutableMap<String, Array<com.beiying.javalib.common.utils.Pair<String, Int>>>
    ) {
        val bufferedReader =
            BufferedReader(FileReader(resTxtFile))
        var line = bufferedReader.readLine()
        var styleable = false
        var styleableName = ""
        val styleableAttrs =
            ArrayList<String>()
        try {
            while (line != null) {
                val columns = line.split(" ".toRegex()).toTypedArray()
                if (columns.size >= 4) {
                    val resourceName = "R." + columns[1] + "." + columns[2]
                    if (!columns[0].endsWith("[]") && columns[3].startsWith("0x")) {
                        if (styleable) {
                            styleable = false
                            styleableName = ""
                        }
                        val resId =
                            parseResourceId(
                                columns[3]
                            )
                        if (!Util.isNullOrNil(resId)) {
                            resourceMap[resourceName] = Integer.decode(resId)
                        }
                    } else if (columns[1] == "styleable") {
                        if (columns[0].endsWith("[]")) {
                            if (columns.size > 5) {
                                styleableAttrs.clear()
                                styleable = true
                                styleableName = "R." + columns[1] + "." + columns[2]
                                for (i in 4 until columns.size - 1) {
                                    if (columns[i].endsWith(",")) {
                                        styleableAttrs.add(
                                            columns[i].substring(
                                                0,
                                                columns[i].length - 1
                                            )
                                        )
                                    } else {
                                        styleableAttrs.add(columns[i])
                                    }
                                }
                                styleableMap[styleableName] = java.lang.reflect.Array.newInstance(
                                    com.beiying.javalib.common.utils.Pair::class.java, styleableAttrs.size
                                ) as Array<com.beiying.javalib.common.utils.Pair<String, Int>>
                            }
                        } else {
                            if (styleable && !Util.isNullOrNil(styleableName)) {
                                val index = columns[3].toInt()
                                val name =
                                    "R." + columns[1] + "." + columns[2]
                                styleableMap[styleableName]!![index] =
                                    com.beiying.javalib.common.utils.Pair(
                                        name,
                                        Integer.decode(
                                            parseResourceId(
                                                styleableAttrs[index]
                                            )
                                        )
                                    )
                            }
                        }
                    } else {
                        if (styleable) {
                            styleable = false
                            styleableName = ""
                        }
                    }
                }
                line = bufferedReader.readLine()
            }
        } finally {
            bufferedReader.close()
        }
    }

    @Throws(IOException::class)
    fun shrinkResourceTxtFile(
        resourceTxt: String?,
        resourceMap: Map<String, Int?>,
        styleableMap: Map<String, Array<com.beiying.javalib.common.utils.Pair<String, Int>>?>
    ) {
        val bufferedWriter =
            BufferedWriter(FileWriter(resourceTxt))
        try {
            for (res in resourceMap.keys) {
                val strBuilder = StringBuilder()
                strBuilder.append("int").append(" ")
                    .append(res.substring(2, res.indexOf('.', 2))).append(" ")
                    .append(res.substring(res.indexOf('.', 2) + 1)).append(" ")
                    .append("0x" + Integer.toHexString(resourceMap[res]!!))
                //Log.d(TAG, "write %s to R.txt", strBuilder.toString())
                bufferedWriter.write(strBuilder.toString())
                bufferedWriter.newLine()
            }
            for (styleable in styleableMap.keys) {
                val strBuilder = StringBuilder()
                val styleableAttrs: Array<com.beiying.javalib.common.utils.Pair<String, Int>>? =
                    styleableMap[styleable]
                strBuilder.append("int[]").append(" ")
                    .append("styleable").append(" ")
                    .append(styleable.substring(styleable.indexOf('.', 2) + 1)).append(" ")
                    .append("{ ")
                for (i in styleableAttrs!!.indices) {
                    if (i != styleableAttrs.size - 1) {
                        strBuilder.append(
                            "0x" + Integer.toHexString(styleableAttrs[i].right)
                        ).append(", ")
                    } else {
                        strBuilder.append("0x" + Integer.toHexString(styleableAttrs[i].right))
                    }
                }
                strBuilder.append(" }")
                //Log.d(TAG, "write %s to R.txt", strBuilder.toString())
                bufferedWriter.write(strBuilder.toString())
                bufferedWriter.newLine()
                for (i in styleableAttrs.indices) {
                    val stringBuilder = StringBuilder()
                    stringBuilder.append("int").append(" ")
                        .append("styleable").append(" ")
                        .append(styleableAttrs[i].left).append(" ")
                        .append(i)
                    //Log.d(TAG, "write %s to R.txt", stringBuilder.toString())
                    bufferedWriter.write(stringBuilder.toString())
                    bufferedWriter.newLine()
                }
            }
        } finally {
            bufferedWriter.close()
        }
    }

    @Throws(
        GradleException::class,
        IOException::class,
        InterruptedException::class
    )
    fun signApk(
        apkFilePath: String,
        apksigner: String,
        signingConfig: SigningConfig?
    ) {
        if (signingConfig == null) return
        val processBuilder = ProcessBuilder()
        val commandList: ArrayList<String?> = ArrayList<String?>(
            Arrays.asList(
                apksigner, "sign", "-v",
                "--ks", signingConfig.storeFile?.absolutePath,
                "--ks-pass", "pass:" + signingConfig.storePassword,
                "--key-pass", "pass:" + signingConfig.keyPassword,
                "--ks-key-alias", signingConfig.keyAlias
            )
        )
        if (signingConfig.isV1SigningEnabled) {
            commandList.add("--v1-signing-enabled")
        }
        if (signingConfig.isV2SigningEnabled) {
            commandList.add("--v2-signing-enabled")
        }
        commandList.add(apkFilePath)
        processBuilder.command(commandList)
        //Log.i(TAG, "%s", processBuilder.command())
        val process = processBuilder.start()
        //        process.waitForProcessOutput(System.out, System.err);
        waitForProcessOutput(process)
        if (process.exitValue() != 0) {
            throw GradleException("sign apk occur error!")
        }
    }

    @Throws(
        GradleException::class,
        IOException::class,
        InterruptedException::class
    )
    fun zipAlignApk(
        inputFile: String?,
        outputFile: String?,
        zipAlignPath: String?
    ) {
        val processBuilder = ProcessBuilder()
        processBuilder.command(zipAlignPath, "-f", "4", inputFile, outputFile)
        val process = processBuilder.start()
        //        process.waitForProcessOutput(System.out, System.err);
        waitForProcessOutput(process)
        if (process.exitValue() != 0) {
            throw GradleException("zipalign apk occur error!")
        }
    }
}