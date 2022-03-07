package com.beiying.apm.matrix;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.ArrayMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public enum AppActiveMatrixDelegate {
    INSTANCE;
    private static final String TAG = "Matrix.AppActiveDelegate";
    private final Set<IAppForeground> listeners = new HashSet();
    private boolean isAppForeground = false;
    private String visibleScene = "default";
    private Controller controller = new Controller();
    private boolean isInit = false;
    private String currentFragmentName;
    private Handler handler;

    public void init(Application app) {
        if (isInit) {
            return;
        }
        this.isInit = true;
        if (null != MatrixHandlerThread.getDefaultHandlerThread()) {
            this.handler = new Handler(MatrixHandlerThread.getDefaultHandlerThread().getLooper());
        }
        app.registerComponentCallbacks(controller);
        app.registerActivityLifecycleCallbacks(controller);
    }

    public String getCurrentFragmentName() {
        return currentFragmentName;
    }

    /**
     * must set after {@link Activity#onStart()}
     *
     * @param fragmentName
     */
    public void setCurrentFragmentName(String fragmentName) {
//        MatrixLog.i(TAG, "[setCurrentFragmentName] fragmentName:%s", fragmentName);
        this.currentFragmentName = fragmentName;
        updateScene(fragmentName);
    }

    public String getVisibleScene() {
        return visibleScene;
    }

    public void addListener(IAppForeground listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public boolean isAppForeground() {
        return isAppForeground;
    }

    private final class Controller implements Application.ActivityLifecycleCallbacks, ComponentCallbacks2 {

        @Override
        public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {

        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {

        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {

        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {

        }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {

        }

        @Override
        public void onTrimMemory(int level) {

        }

        @Override
        public void onConfigurationChanged(@NonNull Configuration newConfig) {

        }

        @Override
        public void onLowMemory() {

        }
    }

    private void updateScene(Activity activity) {
        visibleScene = activity.getClass().getName();
    }

    private void updateScene(String currentFragmentName) {
        StringBuilder ss = new StringBuilder();
        ss.append(TextUtils.isEmpty(currentFragmentName) ? "?" : currentFragmentName);
        visibleScene = ss.toString();
    }

    public static String getTopActivityName() {
        long start = System.currentTimeMillis();
        try {
            Class activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
            activitiesField.setAccessible(true);
            Map<Object, Object> activities;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                activities = (HashMap<Object, Object>)activitiesField.get(activityThread);
            } else {
                activities = (ArrayMap<Object, Object>)activitiesField.get(activityThread);
            }
            if (activities.size() < 1) {
                return null;
            }
            for (Object activityRecord : activities.values()) {
                Class activityRecordClass = activityRecord.getClass();
                Field pauseField = activityRecordClass.getDeclaredField("paused");
                pauseField.setAccessible(true);
                if (!pauseField.getBoolean(activityRecord)) {
                    Field activityField = activityRecordClass.getDeclaredField("activity");
                    activityField.setAccessible(true);
                    Activity activity = (Activity) activityField.get(activityRecord);
                    return activity.getClass().getName();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            long cost = System.currentTimeMillis() - start;
            MatrixLog.d(TAG, "[getTopActivityName] Cost:%s", cost);
        }
        return null;
    }
}
