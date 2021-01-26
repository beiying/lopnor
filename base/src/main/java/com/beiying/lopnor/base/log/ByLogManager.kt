package com.beiying.lopnor.base.log

class ByLogManager private constructor(var config: ByLogConfig, printers: Array<ByLogPrinter>) {
    private val printers: ArrayList<ByLogPrinter> = ArrayList()

    init {
        this.printers.addAll(printers.toList())
    }

    fun getLogPrinters(): ArrayList<ByLogPrinter> {
        return this.printers
    }

    fun addPrinter(printer: ByLogPrinter) {
        this.printers.add(printer)
    }

    fun removePrinter(printer: ByLogPrinter) {
        this.printers.remove(printer)
    }

    companion object {
        private lateinit var instance: ByLogManager
        fun init(config: ByLogConfig, printer: Array<ByLogPrinter>) {
            instance = ByLogManager(config, printer)
        }
        fun get(): ByLogManager {
            return instance
        }
    }
}