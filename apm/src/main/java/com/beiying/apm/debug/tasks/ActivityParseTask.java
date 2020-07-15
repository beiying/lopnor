package com.beiying.apm.debug.tasks;

import com.beiying.apm.api.ApmTask;
import com.beiying.apm.core.IInfo;
import com.beiying.apm.core.job.activity.ActivityInfo;
import com.beiying.apm.core.tasks.IParser;
import com.beiying.apm.debug.DebugConfig;
import com.beiying.apm.debug.DebugFloatWindowUtls;
import com.beiying.apm.debug.OutputProxy;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Debug模块 Activity分析类
 *
 * @author ArgusAPM Team
 */
public class ActivityParseTask implements IParser {
    /**
     * 生命周期所用时间
     *
     * @param info
     */
    @Override
    public boolean parse(IInfo info) {
        if (info instanceof ActivityInfo) {
            ActivityInfo aInfo = (ActivityInfo) info;
            if (aInfo == null) {
                return false;
            }
            if (aInfo.lifeCycle == ActivityInfo.TYPE_FIRST_FRAME) {
                saveWarningInfo(aInfo, DebugConfig.WARN_ACTIVITY_FRAME_VALUE);
                DebugFloatWindowUtls.sendBroadcast(aInfo);
            } else if (aInfo.lifeCycle == ActivityInfo.TYPE_CREATE) {
                saveWarningInfo(aInfo, DebugConfig.WARN_ACTIVITY_CREATE_VALUE);
                DebugFloatWindowUtls.sendBroadcast(aInfo);
            } else if (aInfo.lifeCycle == ActivityInfo.TYPE_RESUME) {
                saveWarningInfo(aInfo, DebugConfig.WARN_ACTIVITY_CREATE_VALUE);
                DebugFloatWindowUtls.sendBroadcast(aInfo);
            } else {
                saveWarningInfo(aInfo, DebugConfig.WARN_ACTIVITY_CREATE_VALUE);
            }
        }
        return true;
    }

    private void saveWarningInfo(ActivityInfo aInfo, int warningTime) {
        if (aInfo.time < warningTime) {
            return;
        }
        try {
            JSONObject obj = aInfo.toJson();
            obj.put("taskName", ApmTask.TASK_ACTIVITY);
            OutputProxy.output("LifeCycle:" + aInfo.getLifeCycleString() + ",cost time:" + aInfo.time, obj.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
