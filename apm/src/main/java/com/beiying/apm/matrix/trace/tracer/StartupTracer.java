package com.beiying.apm.matrix.trace.tracer;

import static android.os.SystemClock.uptimeMillis;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.beiying.apm.matrix.Matrix;
import com.beiying.apm.matrix.MatrixHandlerThread;
import com.beiying.apm.matrix.MatrixLog;
import com.beiying.apm.matrix.trace.ActivityThreadHacker;
import com.beiying.apm.matrix.trace.Constants;
import com.beiying.apm.matrix.trace.MethodItem;
import com.beiying.apm.matrix.trace.TraceConfig;
import com.beiying.apm.matrix.trace.core.AppMethodBeat;
import com.beiying.apm.matrix.trace.listeners.IAppMethodBeatListener;
import com.beiying.apm.matrix.util.TraceDataUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * 启动时间的监控，冷启动、暖启动等时间定义如下：
 * applicationCost（Application的启动时间）：第一次启动Activity或者Service或者广播的时间（这里没有内容提供者是因为内容提供者是在Application初始化完成之前，加载完毕的）
 *          减去 Application开始启动时间
 * firstScreenCost（首屏启动时间）：第一个Activity 可操作的时间（Activity获取焦点） 减去 Application开始启动时间
 * coldCost（冷启动时间）：主Activity可操作的时间（Activity获取焦点） 减去 Application开始启动时间
 * warmCost（暖启动时间）：最近一个Activity开始启动的时间 减去 这个Activity可操作的时间（Activity获取焦点）
 *
 * * firstMethod.i       LAUNCH_ACTIVITY   onWindowFocusChange   LAUNCH_ACTIVITY    onWindowFocusChange
 *  * ^                         ^                   ^                     ^                  ^
 *  * |                         |                   |                     |                  |
 *  * |---------app---------|---|---firstActivity---|---------...---------|---careActivity---|
 *  * |<--applicationCost-->|
 *  * |<--------------firstScreenCost-------------->|
 *  * |<---------------------------------------coldCost------------------------------------->|
 *  * .                         |<-----warmCost---->|
 *
 *  原理：
 *      当 onActivityFocused 被回调时，进行各个时间点的计算，配合 AppMethodBeat 中记录的方法执行时间，通过一定的逻辑 筛选出 导致启动时间长的方法并上报。
 * */
public class StartupTracer extends Tracer implements ActivityThreadHacker.IApplicationCreateListener, IAppMethodBeatListener, Application.ActivityLifecycleCallbacks {
    private static final String TAG = "Matrix.StartupTracer";
    private final TraceConfig config;

    private long firstScreenCost = 0;
    private long coldCost = 0;
    private int activeActivityCount;
    private boolean isWarmStartUp;
    private boolean hasShowSplashActivity;
    private boolean isStartupEnable;
    private Set<String> splashActivities;
    private long coldStartupThresholdMs;
    private long warmStartupThresholdMs;
    private boolean isHasActivity;

    private long lastCreateActivity = 0L;
    private HashMap<String, Long> createdTimeMap = new HashMap<>();
    private boolean isShouldRecordCreateTime = true;

    public StartupTracer(TraceConfig traceConfig) {
        this.config = traceConfig;
        this.isStartupEnable = config.isStartupEnable();
        this.splashActivities = config.getSplashActivities();
        this.coldStartupThresholdMs = config.getColdStartupThresholdMs();
        this.warmStartupThresholdMs = config.getWarmStartupThresholdMs();
        this.isHasActivity = config.isHasActivity();
        ActivityThreadHacker.addListener(this);
    }

    @Override
    public void onApplicationCreateEnd() {
        if (!isHasActivity) {
            long applicationCost = ActivityThreadHacker.getApplicationCost();
            MatrixLog.i(TAG, "onApplicationCreateEnd, applicationCost:%d", applicationCost);
            analyse(applicationCost, 0, applicationCost, false);
        }
    }

    private void analyse(long applicationCost, long firstScreenCost, long allCost, boolean isWarmStartup) {
        MatrixLog.i(TAG, "[report] applicationCost:%s firstScreenCost:%s allCost:%s isWarmStartUp:%s, createScene:%d",
                applicationCost, firstScreenCost, allCost, isWarmStartUp, ActivityThreadHacker.sApplicationCreateScene);
        long[] data = new long[0];
        if (!isWarmStartup && allCost >= coldStartupThresholdMs) {
            data = AppMethodBeat.getInstance().copyData(ActivityThreadHacker.sApplicationCreateBeginMethodIndex);
            ActivityThreadHacker.sApplicationCreateBeginMethodIndex.release();
        } else if (isWarmStartup && allCost >= warmStartupThresholdMs) {
            data = AppMethodBeat.getInstance().copyData(ActivityThreadHacker.sLastLaunchActivityMethodIndex);
            ActivityThreadHacker.sLastLaunchActivityMethodIndex.release();
        }
        MatrixHandlerThread.getDefaultHandler().post(new AnalyseTask(data, applicationCost, firstScreenCost, allCost, isWarmStartUp, ActivityThreadHacker.sApplicationCreateScene));

    }

    @Override
    protected void onAlive() {
        super.onAlive();
        MatrixLog.i(TAG, "[onAlive] isStartupEnable:%s", isStartupEnable);
        if (isStartupEnable) {
            AppMethodBeat.getInstance().addListener(this);
            Matrix.with().getApplication().registerActivityLifecycleCallbacks(this);
        }
    }

    @Override
    protected void onDead() {
        super.onDead();
        if (isStartupEnable) {
            AppMethodBeat.getInstance().removeListener(this);
            Matrix.with().getApplication().unregisterActivityLifecycleCallbacks(this);
        }
    }

    @Override
    public void onActivityFocused(Activity activity) {
        if (ActivityThreadHacker.sApplicationCreateScene == Integer.MIN_VALUE) {
            Log.w(TAG, "start up from unknown scene");
            return;
        }
        String activityName = activity.getClass().getName();
        if (isColdStartup()) {//判断条件是 coldCost == 0 所以只会进来一次
            boolean isCreatedByLaunchActivity = ActivityThreadHacker.isCreatedByLaunchActivity();
            MatrixLog.i(TAG, "#ColdStartup# activity:%s, splashActivities:%s, empty:%b, "
                            + "isCreatedByLaunchActivity:%b, hasShowSplashActivity:%b, "
                            + "firstScreenCost:%d, now:%d, application_create_begin_time:%d, app_cost:%d",
                    activityName, splashActivities, splashActivities.isEmpty(), isCreatedByLaunchActivity,
                    hasShowSplashActivity, firstScreenCost, uptimeMillis(),
                    ActivityThreadHacker.getEggBrokenTime(), ActivityThreadHacker.getApplicationCost());
            String key = activityName + "@" + activity.hashCode();
            Long createdTime = createdTimeMap.get(key);
            if (createdTime == null) {
                createdTime = 0L;
            }
            createdTimeMap.put(key, uptimeMillis() - createdTime);

            if (firstScreenCost == 0) {//首屏启动时间=当前时间点-APP启动时间点
                this.firstScreenCost = uptimeMillis()  - ActivityThreadHacker.getEggBrokenTime();
            }
            if(hasShowSplashActivity) {
                //有splash界面时，冷启动耗时 = （MainActivity启动的时间）当前时间-蛋碎时间
                //类注释上画了，coldCost = 第二个activity onWindowFocusChange时的时间，
                coldCost = uptimeMillis() - ActivityThreadHacker.getEggBrokenTime();
            } else {
                if (splashActivities.contains(activityName)) {
                    hasShowSplashActivity = true;
                } else if (splashActivities.isEmpty()) {
                    if (isCreatedByLaunchActivity) {
                        coldCost = firstScreenCost;
                    } else {
                        firstScreenCost = 0;
                        coldCost = ActivityThreadHacker.getApplicationCost();
                    }
                } else {
                    if (isCreatedByLaunchActivity) {
                        coldCost = firstScreenCost;
                    } else {
                        firstScreenCost = 0;
                        coldCost = ActivityThreadHacker.getApplicationCost();
                    }
                }
            }
            if (coldCost > 0) {
                Long betweenCost = createdTimeMap.get(key);
                if (null != betweenCost && betweenCost >= 30 * 1000) {
                    MatrixLog.e(TAG, "%s cost too much time[%s] between activity create and onActivityFocused, "
                            + "just throw it.(createTime:%s) ", key, uptimeMillis() - createdTime, createdTime);
                    return;
                }
                analyse(ActivityThreadHacker.getApplicationCost(), firstScreenCost, coldCost, isWarmStartUp);
            }
        } else if (isWarmStartUp()){
            isWarmStartUp = false;
            //暖启动时间=当前时间- 最近一个activity被启动的时间
            long warmCost = uptimeMillis() - lastCreateActivity;
            MatrixLog.i(TAG, "#WarmStartup# activity:%s, warmCost:%d, now:%d, lastCreateActivity:%d", activityName, warmCost, uptimeMillis(), lastCreateActivity);

            if (warmCost > 0) {
                analyse(0, 0, warmCost, true);
            }
        }
    }

    private boolean isColdStartup() {
        return coldCost == 0;
    }

    private boolean isWarmStartUp() {
        return isWarmStartUp;
    }

    //在 ActivityLifecycleCallbacks 的相关方法中只做了一件事 就是判断当前是否是暖启动。
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

    private class AnalyseTask implements Runnable {
        long[] data;
        long applicationCost;
        long firstScreenCost;
        long allCost;
        boolean isWarmStartUp;
        int scene;

        AnalyseTask(long[] data, long applicationCost, long firstScreenCost, long allCost, boolean isWarmStartUp, int scene) {
            this.data = data;
            this.scene = scene;
            this.applicationCost = applicationCost;
            this.firstScreenCost = firstScreenCost;
            this.allCost = allCost;
            this.isWarmStartUp = isWarmStartUp;
        }
        /**
         * 上报数据解析
         * application_create：（Application的启动时间）第一次启动Activity或者Service或者广播的时间（这里没有内容提供者是因为内容提供者是在Application初始化完成之前，加载完毕的） 减去 Application开始启动时间
         * application_create_scene：启动场景 Activity（159,100），Service（114），broadcastReceiver（）113
         * first_activity_create：（首屏启动时间）第一个Activity 可操作的时间（Activity获取焦点） 减去 Application开始启动时间
         * startup_duration：启动时间可分为 ：
         *     * （冷启动时间）：主Activity可操作的时间（Activity获取焦点） 减去 Application开始启动时间
         *     * （暖启动时间）：最近一个Activity开始启动的时间 减去 这个Activity可操作的时间（Activity获取焦点）
         * is_warm_start_up：是否是暖启动
         *
         * detail：固定为STARTUP
         * cost：总耗时同 startup_duration
         * stack：方法栈信息， 每个item之间用“\n”隔开，每个item的含义为，调用深度，methodId，调用次数，耗时
         *     * 比如：0,118,1,5 -> 调用深度为0，methodId=118，调用次数=1，耗时5ms
         * stackKey：主要耗时方法 的methodId
         * subType：2：暖启动，1：冷启动
         * */

        @Override
        public void run() {
            LinkedList<MethodItem> stack = new LinkedList<>();
            if (data.length > 0) {
                //根据之前 data 查到的 methodId ，拿到对应插桩函数的执行时间、执行深度，
                // 将每个函数的信息封装成 MethodItem，然后存储到 stack 集合当中
                TraceDataUtils.structuredDataToStack(data, stack, false, -1);

                //根据规则 裁剪 stack 中的数据
                TraceDataUtils.trimStack(stack, Constants.TARGET_EVIL_METHOD_STACK, new TraceDataUtils.IStructuredDataFilter() {
                    @Override
                    public boolean isFilter(long during, int filterCount) {
                        //如果 耗时小于 预设值 则进行裁剪
                        return false;
                    }

                    @Override
                    public int getFilterMaxCount() {
                        //最大方法裁剪数 60
                        return 0;
                    }

                    //降级策略
                    @Override
                    public void fallback(List<MethodItem> stack, int size) {
                        //循环删除 多余的数据
                    }
                });
            }
        }
    }
}
