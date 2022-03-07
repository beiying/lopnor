package com.beiying.apm.matrix.plugin;

import android.app.Application;

import com.beiying.apm.matrix.AppActiveMatrixDelegate;
import com.beiying.apm.matrix.IAppForeground;
import com.beiying.apm.matrix.Issue;
import com.beiying.apm.matrix.IssuePublisher;
import com.beiying.apm.matrix.MatrixLog;
import com.beiying.apm.matrix.util.MatrixUtil;

import org.json.JSONException;
import org.json.JSONObject;

public class Plugin implements IPlugin, IAppForeground, IssuePublisher.OnIssueDetectListener {
    private static final String TAG = "Matrix.Plugin";

    public static final int PLUGIN_CREATE = 0x00;
    public static final int PLUGIN_INITED = 0x01;
    public static final int PLUGIN_STARTED = 0x02;
    public static final int PLUGIN_STOPPED = 0x04;
    public static final int PLUGIN_DESTROYED = 0x08;

    private PluginListener pluginListener;
    private Application application;

    private boolean isSupported = true;
    private int status = PLUGIN_CREATE;

    //组织 问题的 Json数据 并调用 监听的 onReportIssue 方法
    @Override
    public void onDetectIssue(Issue issue) {
        if (issue.getTag() == null) {
            issue.setTag(getTag());
        }
        issue.setPlugin(this);
        JSONObject content = issue.getContent();
        // add tag and type for default
        try {
            if (issue.getTag() != null) {
                content.put(Issue.ISSUE_REPORT_TAG, issue.getTag());
            }
            if (issue.getType() != 0) {
                content.put(Issue.ISSUE_REPORT_TYPE, issue.getType());
            }
            content.put(Issue.ISSUE_REPORT_PROCESS, MatrixUtil.getProcessName(application));
            content.put(Issue.ISSUE_REPORT_TIME, System.currentTimeMillis());

        } catch (JSONException e) {
            MatrixLog.e(TAG, "json error", e);
        }

        pluginListener.onReportIssue(issue);
    }

    @Override
    public void init(Application app, PluginListener listener) {
        if (application != null || pluginListener != null) {
            throw new RuntimeException("plugin duplicate init, application or plugin listener is not null");
        }
        status = PLUGIN_INITED;
        this.application = app;
        this.pluginListener = listener;
        AppActiveMatrixDelegate.INSTANCE.addListener(this);
    }

    @Override
    public Application getApplication() {
        return application;
    }

    @Override
    public void start() {
        if (isPluginDestroyed()) {
            throw new RuntimeException("plugin start, but plugin has been already destroyed");
        }
        if (isPluginStarted()) {
            throw new RuntimeException("plugin start, but plugin has been already started");
        }
        status = PLUGIN_STARTED;
        if (pluginListener == null) {
            throw new RuntimeException("plugin start, plugin listener is null");
        }
        pluginListener.onStart(this);
    }

    @Override
    public void stop() {
        if (isPluginDestroyed()) {
            throw new RuntimeException("plugin stop, but plugin has been already destroyed");
        }

        if (!isPluginStarted()) {
            throw new RuntimeException("plugin stop, but plugin is never started");
        }

        status = PLUGIN_STOPPED;

        if (pluginListener == null) {
            throw new RuntimeException("plugin stop, plugin listener is null");
        }
        pluginListener.onStop(this);
    }

    @Override
    public void destroy() {
        // stop first
        if (isPluginStarted()) {
            stop();
        }
        if (isPluginDestroyed()) {
            throw new RuntimeException("plugin destroy, but plugin has been already destroyed");
        }
        status = PLUGIN_DESTROYED;

        if (pluginListener == null) {
            throw new RuntimeException("plugin destroy, plugin listener is null");
        }
        pluginListener.onDestroy(this);
    }

    @Override
    public String getTag() {
        return getClass().getName();
    }

    @Override
    public void onForeground(boolean isForeground) {

    }

    public boolean isForeground() {
        return AppActiveMatrixDelegate.INSTANCE.isAppForeground();
    }


    public int getStatus() {
        return status;
    }


    public boolean isPluginStarted() {
        return (status == PLUGIN_STARTED);
    }

    public boolean isPluginStopped() {
        return (status == PLUGIN_STOPPED);
    }

    public boolean isPluginDestroyed() {
        return (status == PLUGIN_DESTROYED);
    }

    public boolean isSupported() {
        return isSupported;
    }

    public void unSupportPlugin() {
        isSupported = false;
    }

    public JSONObject getJsonInfo() {
        return new JSONObject();
    }
}
