package com.beiying.apm.matrix.trace;

public interface IDefaultConfig {
    boolean isAppMethodBeatEnable();

    boolean isFPSEnable();

    boolean isEvilMethodTraceEnable();

    boolean isAnrTraceEnable();

    boolean isIdleHandlerTraceEnable();

    boolean isTouchEventTraceEnable();

    boolean isSignalAnrTraceEnable();

    boolean isMainThreadPriorityTraceEnable();

    boolean isDebug();

    boolean isDevEnv();

    int getLooperPrinterStackStyle();

    String getAnrTraceFilePath();

    String getPrintTraceFilePath();

    boolean isHistoryMsgRecorderEnable();

    boolean isDenseMsgTracerEnable();
}
