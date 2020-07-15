package com.beiying.apm.core.job.processinfo;


import com.beiying.apm.api.ApmTask;
import com.beiying.apm.core.Manager;
import com.beiying.apm.core.storage.IStorage;
import com.beiying.apm.core.tasks.BaseTask;
import com.beiying.apm.core.tasks.ITask;
import com.beiying.apm.utils.AsyncThreadTask;
import com.beiying.apm.utils.LogX;

import static com.beiying.apm.Env.DEBUG;
import static com.beiying.apm.Env.TAG;

/**
 * 进程信息任务类
 *
 * @author ArgusAPM Team
 */
public class ProcessInfoTask extends BaseTask {

    @Override
    public void start() {
        super.start();
        saveProcessInfo();
    }

    /**
     * 保存进程相关信息
     */
    private void saveProcessInfo() {
        AsyncThreadTask.executeDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isCanWork()) {
                    return;
                }
                ProcessInfo info = new ProcessInfo();
                ITask task = Manager.getInstance().getTaskManager().getTask(ApmTask.TASK_PROCESS_INFO);
                if (task != null) {
                    task.save(info);
                } else {
                    if (DEBUG) {
                        LogX.d(TAG, "Client", "ProcessInfo task == null");
                    }
                }
            }
        }, 2000 + (int) (Math.round(Math.random() * 1000)));
    }

    @Override
    protected IStorage getStorage() {
        return new ProcessInfoStorage();
    }

    @Override
    public String getTaskName() {
        return ApmTask.TASK_PROCESS_INFO;
    }
}
