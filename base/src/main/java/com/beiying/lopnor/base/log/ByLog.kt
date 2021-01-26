package com.beiying.lopnor.base.log

import android.util.Log
import java.lang.StringBuilder

/**
 * 1、打印堆栈信息
 * 2、File输出
 * 3、模拟控制台
 * */
class ByLog {
    companion object {
        fun v(contents: Array<Any>) {
            log(ByLogType.V, contents)
        }
        fun vt(tag: String, contents: Array<Any>) {
            log(ByLogType.V, tag, contents)
        }

        fun d(contents: Array<Any>) {
            log(ByLogType.V, contents)
        }
        fun dt(tag: String, contents: Array<Any>) {
            log(ByLogType.V, tag, contents)
        }

        fun i(contents: Array<Any>) {
            log(ByLogType.V, contents)
        }
        fun it(tag: String, contents: Array<Any>) {
            log(ByLogType.V, tag, contents)
        }

        fun w(contents: Array<Any>) {
            log(ByLogType.V, contents)
        }
        fun wt(tag: String, contents: Array<Any>) {
            log(ByLogType.V, tag, contents)
        }

        fun e(contents: Array<Any>) {
            log(ByLogType.V, contents)
        }
        fun et(tag: String, contents: Array<Any>) {
            log(ByLogType.V, tag, contents)
        }

        fun a(contents: Array<Any>) {
            log(ByLogType.V, contents)
        }
        fun at(tag: String, contents: Array<Any>) {
            log(ByLogType.V, tag, contents)
        }

        fun log(@ByLogType.TYPE type: Int, contents: Array<Any>) {
            log(type, ByLogManager.get().config.getGlobalTag(), contents)
        }

        fun log(@ByLogType.TYPE type: Int, tag: String, contents: Array<Any>) {
            log(ByLogManager.get().config, type, tag, contents)
        }

        fun log(config: ByLogConfig, @ByLogType.TYPE type: Int, tag: String, contents: Array<Any>) {
            if (!config.enable()) return
            var sb: StringBuilder = StringBuilder()
            if (config.includeThread()) {
                var threadInfo: String = ByLogConfig.BY_THREAD_FORMATTER.format(Thread.currentThread())
                sb.append(threadInfo).append("\n")
            }
            if (config.stackTraceDepth() > 0) {
                var stackTrace = ByLogConfig.BY_STACK_TRACE_FORMATTER.format(Throwable().stackTrace)
                sb.append(stackTrace).append("\n")
            }
            var body: String = parseBody(contents, config)
            sb.append(body)

            var printers: ArrayList<ByLogPrinter>? = if (config.printers() == null) {
                ByLogManager.get().getLogPrinters()
            } else {
                arrayListOf(*config.printers()!!)
            }
            printers?.let {logPrinters ->
                for (printer in logPrinters) {
                    printer.print(config, type, tag, sb.toString())
                }
            }
            Log.println(type, tag, body)
        }

        private fun parseBody(contents: Array<Any>, config: ByLogConfig): String {
            config.injectJsonParser()?.let { parser ->
                return parser.toJson(contents)
            }
            var sb: StringBuilder = StringBuilder()
            for(content in contents) {
                sb.append(content.toString()).append(";")
            }
            if (sb.isNotEmpty()) {
                sb.deleteCharAt(sb.length - 1)
            }
            return sb.toString()
        }
    }
}