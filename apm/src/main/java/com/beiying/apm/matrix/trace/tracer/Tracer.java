package com.beiying.apm.matrix.trace.tracer;

import androidx.annotation.CallSuper;

import com.beiying.apm.matrix.AppActiveMatrixDelegate;
import com.beiying.apm.matrix.MatrixLog;
import com.beiying.apm.matrix.trace.core.LooperObserver;
import com.beiying.apm.matrix.trace.tracer.ITracer;

public class Tracer extends LooperObserver implements ITracer {
    private volatile boolean isAlive = false;
    private static final String TAG = "Matrix.Tracer";

    @CallSuper
    protected void onAlive() {
        MatrixLog.i(TAG, "[onAlive] %s", this.getClass().getName());
    }

    @CallSuper
    protected void onDead() {
        MatrixLog.i(TAG, "[onDead] %s", this.getClass().getName());
    }

    @Override
    final synchronized public void onStartTrace() {
        if (!isAlive) {
            this.isAlive = true;
            onAlive();
        }
    }

    @Override
    final synchronized public void onCloseTrace() {
        if (isAlive) {
            this.isAlive = false;
            onDead();
        }
    }

    @Override
    public void onForeground(boolean isForeground) {

    }

    @Override
    public boolean isAlive() {
        return isAlive;
    }

    public boolean isForeground() {
        return AppActiveMatrixDelegate.INSTANCE.isAppForeground();
    }
}
