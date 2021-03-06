package com.beiying.apm.debug;

import android.text.TextUtils;

import com.beiying.apm.Env;
import com.beiying.apm.api.ApmTask;
import com.beiying.apm.core.Manager;
import com.beiying.apm.core.tasks.IParser;
import com.beiying.apm.debug.tasks.ActivityParseTask;
import com.beiying.apm.debug.tasks.AppStartParseTask;
import com.beiying.apm.debug.tasks.FpsParseTask;
import com.beiying.apm.debug.tasks.MemoryParseTask;
import com.beiying.apm.debug.tasks.NetParseTask;
import com.beiying.apm.debug.view.FloatWindowManager;
import com.beiying.apm.utils.LogX;
import com.beiying.apm.utils.ProcessUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Debug模式，数据分析管理
 *
 * @author ArgusAPM Team
 */
public class AnalyzeManager {
    private static final String SUB_TAG = "AnalyzeManager";
    private static AnalyzeManager instance;
    private boolean mIsDebugMode = false; //debug模式开关
    private boolean mIsShowFloatWin = false; //悬浮窗展示开关
    private boolean isUiProcess = false;

    private Map<String, IParser> mParsers;
    private String floatwinProcessName;

    public static AnalyzeManager getInstance() {
        if (instance == null) {
            synchronized (AnalyzeManager.class) {
                if (instance == null) {
                    instance = new AnalyzeManager();
                }
            }
        }
        return instance;
    }

    private AnalyzeManager() {
        mParsers = new HashMap<String, IParser>(3);
        mParsers.put(ApmTask.TASK_ACTIVITY, new ActivityParseTask());
        mParsers.put(ApmTask.TASK_NET, new NetParseTask());
        mParsers.put(ApmTask.TASK_FPS, new FpsParseTask());
        mParsers.put(ApmTask.TASK_APP_START, new AppStartParseTask());
        mParsers.put(ApmTask.TASK_MEM, new MemoryParseTask());
        this.isUiProcess = Manager.getContext().getPackageName().equals(ProcessUtils.getCurrentProcessName());
    }

    public void setShowFloatWin(boolean open, String floatwinProcessName) {
        this.mIsShowFloatWin = open;
        this.mIsDebugMode = this.mIsShowFloatWin;
        this.floatwinProcessName = floatwinProcessName;
        if (this.mIsShowFloatWin && isShowFloatwin()) {
            FloatWindowManager.getInstance().showBigWindow();
        }
    }

    public void setIsDebugMode(boolean isOpen) {
        this.mIsDebugMode = isOpen;
    }

    public boolean isDebugMode() {
        if (this.mIsShowFloatWin) {
            return true;
        }
        return mIsDebugMode;
    }

    private boolean isShowFloatwin() {
        if (Env.DEBUG) {
            LogX.d(Env.TAG, SUB_TAG, "floatwinProcessName:" + floatwinProcessName + "  isUiProcess: " + isUiProcess);
            LogX.d(Env.TAG, SUB_TAG, "ProcessUtils.getCurrentProcessName() :" + ProcessUtils.getCurrentProcessName());
        }
        if (TextUtils.isEmpty(this.floatwinProcessName) && this.isUiProcess) {
            return true;
        }
        return ProcessUtils.getCurrentProcessName().equals(this.floatwinProcessName);
    }

    public IParser getParseTask(String name) {
        if (TextUtils.isEmpty(name)) {
            return null;
        }
        return mParsers.get(name);
    }

    public IParser getActivityTask() {
        return getParseTask(ApmTask.TASK_ACTIVITY);
    }

    public IParser getNetTask() {
        return getParseTask(ApmTask.TASK_NET);
    }

    public IParser getFpsTask() {
        return getParseTask(ApmTask.TASK_FPS);
    }
}
