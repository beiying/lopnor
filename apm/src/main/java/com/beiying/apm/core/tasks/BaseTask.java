package com.beiying.apm.core.tasks;

import com.beiying.apm.core.IInfo;
import com.beiying.apm.core.storage.IStorage;
import com.beiying.apm.utils.LogX;

import static com.beiying.apm.Env.DEBUG;
import static com.beiying.apm.Env.TAG;


/**
 * ArgusAPM任务基类
 *
 * @author ArgusAPM Team
 */
public abstract class BaseTask implements ITask {

    public static final String SUB_TAG = "BaseTask";

    protected IStorage mStorage;

    protected boolean mIsCanWork = false;

    public BaseTask() {
        mStorage = getStorage();
    }

    protected abstract IStorage getStorage();

    @Override
    public void start() {
        if (DEBUG) {
            LogX.d(TAG, SUB_TAG, "start task :" + getTaskName());
        }
    }

    @Override
    public boolean save(IInfo info) {
        if (DEBUG) {
            LogX.d(TAG, SUB_TAG, "save task :" + getTaskName());
        }
        return info != null && mStorage != null && mStorage.save(info);
    }

    @Override
    public boolean isCanWork() {
        return mIsCanWork;
    }

    public void setCanWork(boolean canWork) {
        if (DEBUG) {
            LogX.d(TAG, SUB_TAG, "setCanWork task :" + getTaskName() + " :" + canWork);
        }
        mIsCanWork = canWork;
    }

    @Override
    public void stop() {

    }
}
