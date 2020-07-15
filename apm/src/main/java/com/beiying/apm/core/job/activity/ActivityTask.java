package com.beiying.apm.core.job.activity;

import com.beiying.apm.api.ApmTask;
import com.beiying.apm.core.Manager;
import com.beiying.apm.core.storage.IStorage;
import com.beiying.apm.core.tasks.BaseTask;
import com.beiying.apm.utils.LogX;

import static com.beiying.apm.Env.DEBUG;
import static com.beiying.apm.Env.TAG;


/**
 * @author ArgusAPM Team
 */
public class ActivityTask extends BaseTask {

    @Override
    protected IStorage getStorage() {
        return new ActivityStorage();
    }

    @Override
    public String getTaskName() {
        return ApmTask.TASK_ACTIVITY;
    }

    @Override
    public void start() {
        super.start();
        if (Manager.getInstance().getConfig().isEnabled(ApmTask.FLAG_COLLECT_ACTIVITY_INSTRUMENTATION) && !InstrumentationHooker.isHookSucceed()) {//hook失败
            if (DEBUG) {
                LogX.d(TAG, "ActivityTask", "canWork hook : hook失败");
            }
            mIsCanWork = false;
        }
    }

    @Override
    public boolean isCanWork() {
        return mIsCanWork;
    }
}