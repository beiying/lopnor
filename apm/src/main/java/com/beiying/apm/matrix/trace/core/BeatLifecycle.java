package com.beiying.apm.matrix.trace.core;

public interface BeatLifecycle {
    void onStart();

    void onStop();

    boolean isAlive();
}
