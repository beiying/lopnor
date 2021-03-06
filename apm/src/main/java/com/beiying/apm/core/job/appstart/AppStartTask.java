package com.beiying.apm.core.job.appstart;

import com.beiying.apm.api.ApmTask;
import com.beiying.apm.core.storage.IStorage;
import com.beiying.apm.core.tasks.BaseTask;

/**
 * 应用启动Task
 *
 * @author ArgusAPM Team
 */
public class AppStartTask extends BaseTask {

    @Override
    protected IStorage getStorage() {
        return new AppStartStorage();
    }

    @Override
    public String getTaskName() {
        return ApmTask.TASK_APP_START;
    }
}