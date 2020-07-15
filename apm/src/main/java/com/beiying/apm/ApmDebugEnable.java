package com.beiying.apm;

import android.text.TextUtils;


import com.beiying.apm.api.ApmTask;
import com.beiying.apm.core.Manager;
import com.beiying.apm.utils.FileUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/**
 * debug包APM本地开关控制类
 *
 * @author ArgusAPM Team
 */
public class ApmDebugEnable {
    public static final String SUB_TAG = "debug_enable";

    private static JSONObject sDebugSwitchControl;
    private static String sDebugConfig;

    public static String load() {
        return load(ApmTask.APM_CONFIG_FILE);
    }

    private static String load(String fileName) {
        String name = Manager.getInstance().getBasePath() + File.separator + fileName;
        if (TextUtils.isEmpty(sDebugConfig)) {
            sDebugConfig = FileUtils.readFile(name);
        }
        if (!TextUtils.isEmpty(sDebugConfig)) {
            try {
                if (sDebugSwitchControl == null) {
                    sDebugSwitchControl = new JSONObject(sDebugConfig);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return sDebugConfig;
    }


}
