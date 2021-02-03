package com.beiying.lopnor.base.log

import android.text.TextUtils
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingDeque

class ByFilePrinter private constructor(var logPath: String, var retentionTime: Long): ByLogPrinter {
    var worker: FilePrintWorker = FilePrintWorker()
    var writer: LogWriter = LogWriter()

    init {
        cleanExpiredLog()
    }

    override fun print(config: ByLogConfig, level: Int, tag: String, printString: String) {
        val timeMillis: Long = System.currentTimeMillis()
        if (!worker.isRunning()) {
            worker.start()
        }
        worker.put(ByLogModel(timeMillis, level, tag, printString))
    }

    companion object {
        private val EXECUTOR: ExecutorService = Executors.newSingleThreadExecutor()
        @Volatile var instance: ByFilePrinter? = null

        fun getInstance(logPath: String, retentionTime: Long): ByFilePrinter {
            return instance?: synchronized(this) {
                instance ?: ByFilePrinter(logPath, retentionTime).apply { instance = this}
            }
        }
    }

    private fun doPrint(log: ByLogModel) {
        var lastFileName: String? = writer.getPreFileName()
        if (TextUtils.isEmpty(lastFileName)) {
            var newFileName: String = genFileName()
            if (writer.isReady()) {
                writer.close()
            }
            if (!writer.ready(newFileName)) {
                return
            }
        }
        writer.append(log.flattendLog())
    }

    private fun genFileName(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(Date(System.currentTimeMillis()))
    }

    /**
     * 清除过期log
     */
    private fun cleanExpiredLog() {
        if (retentionTime <= 0) {
            return
        }
        val currentTimeMillis = System.currentTimeMillis()
        val logDir = File(logPath)
        val files = logDir.listFiles() ?: return
        for (file in files) {
            if (currentTimeMillis - file.lastModified() > retentionTime) {
                file.delete()
            }
        }
    }

    inner class LogWriter {
        private var preFileName: String = ""
        var logFile: File? = null
        var bufferedWriter: BufferedWriter? = null

        fun isReady(): Boolean {
            return bufferedWriter != null
        }

        fun getPreFileName(): String {
            return preFileName
        }

        fun ready(fileName: String): Boolean {
            preFileName = fileName
            logFile = File(logPath, fileName)
            logFile?.let { file ->
                if (!file.exists()) {
                    try {
                        val parent: File = file.parentFile
                        if (!parent.exists()) {
                            parent.mkdirs()
                        }
                        file.createNewFile()
                    } catch (e: IOException) {
                        e.printStackTrace()
                        preFileName = ""
                        logFile = null
                        return false
                    }
                }
            }

            try {
                bufferedWriter = BufferedWriter(FileWriter(logFile, true))
            } catch (e: Exception) {
                e.printStackTrace()
                preFileName = ""
                logFile = null
                return false
            }
            return true
        }

        fun append(logStr: String) {
            try {
                bufferedWriter?.let {writer ->
                    writer.write(logStr)
                    writer.newLine()
                    writer.flush()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        fun close(): Boolean {
            try {
                bufferedWriter?.close()
            } catch (e: IOException) {
                e.printStackTrace()
                preFileName = ""
                logFile = null
                bufferedWriter = null
                return false
            }
            return true
        }
    }

    inner class FilePrintWorker: Runnable {
        var logs: BlockingQueue<ByLogModel> = LinkedBlockingDeque()
        var running: Boolean = false

        fun put(log: ByLogModel) {
            try {
                logs.put(log)
            } catch (e: InterruptedException) {
                e.printStackTrace()

            }
        }

        @Synchronized fun isRunning(): Boolean {
            return running
        }

        @Synchronized fun start() {
            EXECUTOR.execute(this)
            running = true
        }

        override fun run() {
            try {
                while (true) {
                    val log: ByLogModel = logs.take()
                    doPrint(log)
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
                synchronized(this) {
                    running = false
                }
            }
        }

    }
}