package com.beiying.apm.utils;

import android.app.Activity;

import com.beiying.apm.Env;
import com.beiying.apm.api.ExtraDataType;
import com.beiying.apm.api.IExtraDataCallback;
import com.beiying.apm.core.Manager;

import static com.beiying.apm.Env.TAG;


/**
 * @author ArgusAPM Team
 */
public class ExtraInfoHelper {
    /**
     * 获取主程序版本
     *
     * @param
     * @return
     */
    public static String getMainVersion() {
        IExtraDataCallback callback = Manager.getInstance().getConfig().mExtraDataCallback;
        if (callback != null) {
            try {
                String mainVersion = (String) callback.parse(ExtraDataType.TYPE_GET_MAIN_VERSION);
                if (Env.DEBUG) {
                    LogX.d(TAG, TAG, "getMainVersion version = " + mainVersion);
                }
                return mainVersion;
            } catch (Exception e) {
                if (Env.DEBUG) {
                    LogX.e(TAG, TAG, "getMainVersion ex : " + e.getMessage());
                }
            }
        }
        return "";
    }

    /**
     * 获取插件名称
     *
     * @param activity
     * @return
     */
    public static String getPluginName(Activity activity) {
        IExtraDataCallback callback = Manager.getInstance().getConfig().mExtraDataCallback;
        if (callback != null) {
            try {
                String pluginName = (String) callback.parse(ExtraDataType.TYPE_GET_PLUGIN_NAME, activity);
                if (Env.DEBUG) {
                    LogX.d(TAG, TAG, "getPluginName pluginName = " + pluginName);
                }
                return pluginName;
            } catch (Exception e) {
                if (Env.DEBUG) {
                    LogX.e(TAG, TAG, "getPluginName ex : " + e.getMessage());
                }
            }
        }
        return "";
    }

    /**
     * 获取插件版本号
     *
     * @param name
     * @return
     */
    public static String getPluginVersion(String name) {
        IExtraDataCallback callback = Manager.getInstance().getConfig().mExtraDataCallback;
        if (callback != null) {
            try {
                int pluginVersion = (Integer) callback.parse(ExtraDataType.TYPE_GET_PLUGIN_VERSION, name);
                if (Env.DEBUG) {
                    LogX.d(TAG, TAG, "getPluginVersion pluginVersion = " + pluginVersion);
                }
                return String.valueOf(pluginVersion);
            } catch (Exception e) {
                if (Env.DEBUG) {
                    LogX.e(TAG, TAG, "getPluginVersion ex :" + e.getMessage());
                }
            }
        }
        return "";
    }


    /**
     * 解析V5的ini文件过程中，将解析结果回调给调用方
     *
     * @param task  采样数据
     * @param state 是否采集
     */
    public static void notifyV5Update(String task, boolean state) {
        IExtraDataCallback callback = Manager.getInstance().getConfig().mExtraDataCallback;
        if (callback != null) {

            try {
                callback.parse(ExtraDataType.TYPE_RECV_V5_UPDATE, task, state);
                if (Env.DEBUG) {
                    LogX.d(TAG, TAG, "notifyV5Update task = " + task + " | state = " + state);
                }
            } catch (Exception e) {
                if (Env.DEBUG) {
                    LogX.e(TAG, TAG, "notifyV5Update ex :" + e.getMessage());
                }
            }
        }
    }
}
