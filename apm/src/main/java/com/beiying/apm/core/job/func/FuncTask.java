package com.beiying.apm.core.job.func;


import com.beiying.apm.api.ApmTask;
import com.beiying.apm.core.storage.IStorage;
import com.beiying.apm.core.tasks.BaseTask;

/**
 * @author ArgusAPM Team
 */
public class FuncTask extends BaseTask {
    @Override
    protected IStorage getStorage() {
        return new FuncStorage();
    }

    @Override
    public String getTaskName() {
        return ApmTask.TASK_FUNC;
    }
}
