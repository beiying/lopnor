package com.beiying.apm.debug.tasks;


import com.beiying.apm.api.ApmTask;
import com.beiying.apm.core.IInfo;
import com.beiying.apm.core.job.memory.MemoryInfo;
import com.beiying.apm.core.tasks.IParser;
import com.beiying.apm.debug.DebugFloatWindowUtls;
import com.beiying.apm.debug.OutputProxy;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * debug模式内存task
 *
 * @author ArgusAPM Team
 */
public class MemoryParseTask implements IParser {
    @Override
    public boolean parse(IInfo info) {
        if (info instanceof MemoryInfo) {
            MemoryInfo aInfo = (MemoryInfo) info;
            if (aInfo == null) {
                return false;
            }
            try {
                JSONObject obj = aInfo.toJson();
                obj.put("taskName", ApmTask.TASK_MEM);
                OutputProxy.output("", obj.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            DebugFloatWindowUtls.sendBroadcast(aInfo);
        }
        return true;
    }
}
