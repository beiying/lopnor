package com.beiying.lopnor.base.log

import kotlin.math.min

class ByStackTraceUtil {
    companion object {
        fun getCroppedRealStackTrace(callStack: Array<StackTraceElement?>, ignorPkg: String, maxDepth: Int): Array<StackTraceElement?> {
            return cropStackTrace(getRealStackTrace(callStack, ignorPkg), maxDepth)
        }

        fun collectDeviceInfo(throwable: Throwable): String {
            val sb = StringBuffer()
            return sb.toString()
        }

        /**
         * 堆栈信息裁剪
         * （堆栈信息很大一部分是系统调用栈，对我们分析问题并没有实际帮助，为了减少日志数据，可以裁减掉无用的log）
         * */
        private fun cropStackTrace(callStack: Array<StackTraceElement?>, maxDepth: Int): Array<StackTraceElement?> {
            var realDepth: Int = callStack.size
            if (maxDepth > 0) {
                realDepth = min(maxDepth, realDepth)
            }

            var realStack: Array<StackTraceElement?> = arrayOfNulls<StackTraceElement>(realDepth)
            System.arraycopy(callStack, 0, realDepth, 0, realDepth)
            return realStack
        }

        /**
         * 获取指定包名以外的堆栈信息
         * */
        private fun getRealStackTrace(callStack: Array<StackTraceElement?>, ignorPkg: String): Array<StackTraceElement?> {
            var ignorDepth = 0
            var allDepth = callStack.size
            var className: String = ""
            for ((index, item) in callStack.withIndex()) {
                className = if (item != null) item.className else ""
                if (className.startsWith(ignorPkg)) {
                    ignorDepth = index + 1
                    break
                }
            }
            var realDepth = allDepth - ignorDepth
            var realStack: Array<StackTraceElement?> = arrayOfNulls<StackTraceElement>(realDepth)
            System.arraycopy(callStack, 0, realDepth, 0, realDepth)
            return realStack
        }
    }
}