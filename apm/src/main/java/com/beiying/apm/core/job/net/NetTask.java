package com.beiying.apm.core.job.net;

import com.beiying.apm.api.ApmTask;
import com.beiying.apm.core.storage.IStorage;
import com.beiying.apm.core.tasks.BaseTask;

/**
 * @author ArgusAPM Team
 */
public class NetTask extends BaseTask {

    @Override
    protected IStorage getStorage() {
        return new NetStorage();
    }

    @Override
    public String getTaskName() {
        return ApmTask.TASK_NET;
    }
}
