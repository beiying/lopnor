package com.beiying.lopnor.base.log

class ByThreadFormatter: ByLogFormatter<Thread> {
    override fun format(data: Thread): String {
        return "Thread ${data.name}"
    }
}