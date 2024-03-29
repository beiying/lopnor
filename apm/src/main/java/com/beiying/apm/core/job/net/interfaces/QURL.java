package com.beiying.apm.core.job.net.interfaces;


import com.beiying.apm.api.ApmTask;
import com.beiying.apm.core.Manager;
import com.beiying.apm.core.job.net.impl.AopURL;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

/**
 * aop 代码织入时会调用
 *
 * @author ArgusAPM Team
 */
public class QURL {

    public static URLConnection openConnection(URL url) throws IOException {
        return isNetTaskRunning() ? AopURL.openConnection(url) : url.openConnection();
    }

    public static URLConnection openConnection(URL url, Proxy proxy) throws IOException {
        return isNetTaskRunning() ? AopURL.openConnection(url, proxy) : url.openConnection(proxy);
    }

    private static boolean isNetTaskRunning() {
        return Manager.getInstance().getTaskManager().taskIsCanWork(ApmTask.TASK_NET);
    }
}