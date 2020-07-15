package com.beiying.apm.debug.tasks;


import com.beiying.apm.api.ApmTask;
import com.beiying.apm.core.IInfo;
import com.beiying.apm.core.job.appstart.AppStartInfo;
import com.beiying.apm.core.tasks.IParser;
import com.beiying.apm.debug.DebugFloatWindowUtls;
import com.beiying.apm.debug.OutputProxy;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Debug模块 应用启动时间分析类
 *
 * @author ArgusAPM Team
 */
public class AppStartParseTask implements IParser {
    /**
     * app启动
     *
     * @param info
     */
    @Override
    public boolean parse(IInfo info) {
        if (info instanceof AppStartInfo) {
            AppStartInfo aInfo = (AppStartInfo) info;
            if (aInfo == null) {
                return false;
            }
            try {
                JSONObject obj = aInfo.toJson();
                obj.put("taskName", ApmTask.TASK_APP_START);
                OutputProxy.output("启动时间:" + aInfo.getStartTime(), obj.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            DebugFloatWindowUtls.sendBroadcast(aInfo);
        }
        return true;
    }
}
