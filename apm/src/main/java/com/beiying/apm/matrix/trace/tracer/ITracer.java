package com.beiying.apm.matrix.trace.tracer;

import com.beiying.apm.matrix.IAppForeground;

public interface ITracer extends IAppForeground {
    boolean isAlive();

    void onStartTrace();

    void onCloseTrace();
}
