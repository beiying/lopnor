package com.beiying.apm.core.tasks;

import com.beiying.apm.core.IInfo;

public interface ITask {
    String getTaskName();

    void start();

    boolean isCanWork();

    void setCanWork(boolean value);

    boolean save(IInfo info);

    void stop();
}