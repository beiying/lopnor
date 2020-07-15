package com.beiying.apm.core.job.activity;

import android.app.Activity;

import com.beiying.apm.api.ApmTask;
import com.beiying.apm.cloudconfig.ArgusApmConfigManager;
import com.beiying.apm.core.Manager;
import com.beiying.apm.core.job.appstart.AppStartInfo;
import com.beiying.apm.core.tasks.ITask;
import com.beiying.apm.core.tasks.TaskManager;
import com.beiying.apm.debug.AnalyzeManager;
import com.beiying.apm.utils.ExtraInfoHelper;
import com.beiying.apm.utils.LogX;

import static com.beiying.apm.Env.DEBUG;
import static com.beiying.apm.Env.TAG;


/**
 * @author ArgusAPM Team
 */
public class ActivityCore {
    private static final String SUB_TAG = "traceactivity";

    public static boolean isFirst = true;//是否是第一次启动
    public static long appAttachTime = 0;
    public static int startType;//启动类型

    private static ActivityInfo activityInfo = new ActivityInfo();

    public static void saveActivityInfo(Activity activity, int startType, long time, int lifeCycle) {
        if (activity == null) {
            if (DEBUG) {
                LogX.d(TAG, SUB_TAG, "saveActivityInfo activity == null");
            }
            return;
        }
        if (time < ArgusApmConfigManager.getInstance().getArgusApmConfigData().funcControl.activityLifecycleMinTime) {
            return;
        }
        String pluginName = ExtraInfoHelper.getPluginName(activity);
        String activityName = activity.getClass().getCanonicalName();
        activityInfo.resetData();
        activityInfo.activityName = activityName;
        activityInfo.startType = startType;
        activityInfo.time = time;
        activityInfo.lifeCycle = lifeCycle;
        activityInfo.pluginName = pluginName;
        activityInfo.pluginVer = ExtraInfoHelper.getPluginVersion(pluginName);
        if (DEBUG) {
            LogX.d(TAG, SUB_TAG, "apmins saveActivityInfo activity:" + activity.getClass().getCanonicalName() + " | lifecycle : " + activityInfo.getLifeCycleString() + " | time : " + time);
        }
        ITask task = Manager.getInstance().getTaskManager().getTask(ApmTask.TASK_ACTIVITY);
        boolean result = false;
        if (task != null) {
            result = task.save(activityInfo);
        } else {
            if (DEBUG) {
                LogX.d(TAG, SUB_TAG, "saveActivityInfo task == null");
            }
        }
        if (DEBUG) {
            LogX.d(TAG, SUB_TAG, "activity info:" + activityInfo.toString());
        }
        if (AnalyzeManager.getInstance().isDebugMode()) {
            AnalyzeManager.getInstance().getActivityTask().parse(activityInfo);
        }
        if (DEBUG) {
            LogX.d(TAG, SUB_TAG, "saveActivityInfo result:" + result);
        }
    }

    public static void onCreateInfo(Activity activity, long startTime) {
        startType = isFirst ? ActivityInfo.COLD_START : ActivityInfo.HOT_START;
        activity.getWindow().getDecorView().post(new FirstFrameRunnable(activity, startType, startTime));
        //onCreate 时间
        long curTime = System.currentTimeMillis();
        saveActivityInfo(activity, startType, curTime - startTime, ActivityInfo.TYPE_CREATE);
    }


    private static class FirstFrameRunnable implements Runnable {
        private Activity activity;//Activity
        private int startType;//启动类型
        private long startTime;//开始时间

        public FirstFrameRunnable(Activity activity, int startType, long startTime) {
            this.startTime = startTime;
            this.activity = activity;
            this.startType = startType;
        }

        @Override
        public void run() {
            if (DEBUG) {
                LogX.d(TAG, SUB_TAG, "FirstFrameRunnable time:" + (System.currentTimeMillis() - startTime));
            }
            if ((System.currentTimeMillis() - startTime) >= ArgusApmConfigManager.getInstance().getArgusApmConfigData().funcControl.activityFirstMinTime) {
                saveActivityInfo(activity, startType, System.currentTimeMillis() - startTime, ActivityInfo.TYPE_FIRST_FRAME);
            }
            //保存应用冷启动时间
            if (DEBUG) {
                LogX.d(TAG, SUB_TAG, "FirstFrameRunnable time:" + String.format("[%s, %s]", ActivityCore.isFirst, ActivityCore.appAttachTime));
            }
            if (ActivityCore.isFirst) {
                ActivityCore.isFirst = false;
                if (ActivityCore.appAttachTime <= 0) {
                    return;
                }
                int t = (int) (System.currentTimeMillis() - ActivityCore.appAttachTime);
                AppStartInfo info = new AppStartInfo(t);
                ITask task = Manager.getInstance().getTaskManager().getTask(ApmTask.TASK_APP_START);
                if (task != null) {
                    task.save(info);
                    if (AnalyzeManager.getInstance().isDebugMode()) {
                        AnalyzeManager.getInstance().getParseTask(ApmTask.TASK_APP_START).parse(info);
                    }
                } else {
                    if (DEBUG) {
                        LogX.d(TAG, SUB_TAG, "AppStartInfo task == null");
                    }
                }
            }
        }
    }

    /**
     * acitivty性能手机任务是否正在工作
     *
     * @return
     */
    public static boolean isActivityTaskRunning() {
        TaskManager taskManager = Manager.getInstance().getTaskManager();
        if (taskManager == null) {
            if (DEBUG) {
                LogX.d(TAG, SUB_TAG, "taskManager == null");
            }
            return false;
        }
        ITask task = taskManager.getTask(ApmTask.TASK_ACTIVITY);
        if (DEBUG && null != task) {
            LogX.d(TAG, SUB_TAG, "task.isRunning():" + task.isCanWork());
        }

        return null != task && task.isCanWork();
    }

}
