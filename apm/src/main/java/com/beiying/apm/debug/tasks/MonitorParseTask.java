package com.beiying.apm.debug.tasks;

import com.beiying.apm.core.IInfo;
import com.beiying.apm.core.tasks.IParser;

/**
 * @author ArgusAPM Team
 */
public class MonitorParseTask implements IParser {
    @Override
    public boolean parse(IInfo info) {
//        if (info != null && info instanceof MonitorInfo) {
//            MonitorInfo mInfo = (MonitorInfo) info;
//            try {
//                JSONObject obj = mInfo.toJson();
//                obj.put("taskName", ApmTask.TASK_NET);
//                OutputProxy.output(msg, obj.toString());
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//        }
        return false;
    }
}
