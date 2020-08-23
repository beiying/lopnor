package com.beiying.apm.network;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.beiying.apm.Env;
import com.beiying.apm.api.Client;
import com.beiying.apm.cloudconfig.Constant;
import com.beiying.apm.core.Manager;
import com.beiying.apm.utils.FileUtils;
import com.beiying.apm.utils.LogX;
import com.beiying.apm.utils.PreferenceUtils;

import org.json.JSONObject;

import static com.beiying.apm.Env.DEBUG;
import static com.beiying.apm.Env.TAG;
import static com.beiying.apm.Env.TAG_O;

/**
 * 接入云规则平台
 *
 * @author ArgusAPM Team
 */
public class CloudRule {
    public static final String SUB_TAG = "CloudRule";

    private final Context mContext;
    private IRuleRequest mRequest;

    public CloudRule(Context context, IRuleRequest ruleRequest) {
        mContext = context;
        mRequest = ruleRequest;
    }
    
    public boolean request() {
        String result = mRequest.request(Client.getContext(), Manager.getInstance().getConfig().apmId, Env.getVersionName(), Manager.getInstance().getConfig().appName, Manager.getInstance().getConfig().appVersion);
        if (TextUtils.isEmpty(result)) {
            LogX.o(TAG_O, SUB_TAG, "cloudRuleResponse ： is null");
            return false;
        }
        if (DEBUG) {
            LogX.d(TAG, SUB_TAG, "cloudRuleResponse data: " + result);
        }
        try {
            parseResponse(result);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // 云规则平台返回的请求结果
    private void parseResponse(String config) {
        // 解析文件里的时间戳，如果是新文件，则通知更新，否则跳过
        long curTimestamp = 0;
        try {
            JSONObject configJson = new JSONObject(config);
            if (configJson.has("timestamp")) {
                curTimestamp = configJson.getLong("timestamp");
            } else {
                LogX.o(TAG_O, SUB_TAG, "config.is.not.legal");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        long lastTimeStamp = PreferenceUtils.getLong(mContext, PreferenceUtils.SP_KEY_CONFIG_TIMESTAMP, 0);
        if (curTimestamp > lastTimeStamp) {
            LogX.o(TAG_O, SUB_TAG, "config upload success");
            // 写文件，调用reload
            FileUtils.writeFile(FileUtils.getApmConfigFilePath(mContext), config);
            mContext.sendBroadcast(new Intent(Constant.CLOUD_RULE_UPDATE_ACTION));
            PreferenceUtils.setLong(mContext, PreferenceUtils.SP_KEY_CONFIG_TIMESTAMP, curTimestamp);
        } else {
            if (DEBUG) {
                LogX.d(TAG, SUB_TAG, "timestamp 无变化 重复不更新");
            }
        }
    }
}
