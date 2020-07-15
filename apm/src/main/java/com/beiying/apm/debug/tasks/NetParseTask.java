package com.beiying.apm.debug.tasks;


import com.beiying.apm.api.ApmTask;
import com.beiying.apm.core.IInfo;
import com.beiying.apm.core.job.net.NetInfo;
import com.beiying.apm.core.tasks.IParser;
import com.beiying.apm.debug.DebugFloatWindowUtls;
import com.beiying.apm.debug.OutputProxy;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * 网络分析类
 *
 * @author ArgusAPM Team
 */
public class NetParseTask implements IParser {
    @Override
    public boolean parse(IInfo info) {
        if (info != null && info instanceof NetInfo) {
            NetInfo aInfo = (NetInfo) info;
            if (aInfo.statusCode != 200) {
                String msg = String.format("网络错误，状态码:", aInfo.statusCode);
                DebugFloatWindowUtls.sendBroadcast(aInfo);
                try {
                    JSONObject obj = aInfo.toJson();
                    obj.put("taskName", ApmTask.TASK_NET);
                    OutputProxy.output(msg, obj.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }
}
