package com.beiying.apm.core.job.net.impl;


import com.beiying.apm.Env;
import com.beiying.apm.utils.LogX;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

import static com.beiying.apm.Env.TAG;

/**
 * @author ArgusAPM Team
 */
public class AopURL {
    public static URLConnection openConnection(URL url) throws IOException {
        if (url == null) {
            return null;
        }
        return getAopConnection(url.openConnection());
    }

    public static URLConnection openConnection(URL url, Proxy proxy) throws IOException {
        if (url == null) {
            return null;
        }
        return getAopConnection(url.openConnection(proxy));
    }

    private static URLConnection getAopConnection(URLConnection con) {
        if (con == null) {
            return null;
        }
        if (Env.DEBUG) {
            LogX.d(TAG, "AopURL", "getAopConnection in AopURL");
        }
        if ((con instanceof HttpsURLConnection)) {
            return new AopHttpsURLConnection((HttpsURLConnection) con);
        }
        if ((con instanceof HttpURLConnection)) {
            return new AopHttpURLConnection((HttpURLConnection) con);
        }
        return con;
    }
}