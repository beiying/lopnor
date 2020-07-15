package com.beiying.apm.core.job.webview;


import com.beiying.apm.api.ApmTask;
import com.beiying.apm.core.storage.IStorage;
import com.beiying.apm.core.tasks.BaseTask;

/**
 * @author ArgusAPM Team
 */
public class WebTask extends BaseTask {
    @Override
    protected IStorage getStorage() {
        return new WebStorage();
    }

    @Override
    public String getTaskName() {
        return ApmTask.TASK_WEBVIEW;
    }
}
