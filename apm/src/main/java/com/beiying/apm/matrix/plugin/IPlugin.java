package com.beiying.apm.matrix.plugin;

import android.app.Application;

interface IPlugin {
    Application getApplication();

    void init(Application application, PluginListener listener);

    void start();

    void stop();

    void destroy();

    String getTag();

    void onForeground(boolean isForground);
}
