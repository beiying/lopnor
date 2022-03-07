package com.beiying.apm.matrix.trace.tracer;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.beiying.apm.matrix.AppActiveMatrixDelegate;
import com.beiying.apm.matrix.Issue;
import com.beiying.apm.matrix.Matrix;
import com.beiying.apm.matrix.MatrixHandlerThread;
import com.beiying.apm.matrix.MatrixLog;
import com.beiying.apm.matrix.trace.Constants;
import com.beiying.apm.matrix.trace.SharePluginInfo;
import com.beiying.apm.matrix.trace.TraceConfig;
import com.beiying.apm.matrix.trace.TracePlugin;
import com.beiying.apm.matrix.trace.core.UIThreadMonitor;
import com.beiying.apm.matrix.trace.tracer.Tracer;
import com.beiying.apm.matrix.util.DeviceUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Executor;

public class FrameTracer extends Tracer implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "Matrix.FrameTracer";
    private static boolean useFrameMetrics;
    private boolean isFPSEnable;
    private final HashSet<IDoFrameListener> listeners = new HashSet<>();
    private DropFrameListener dropFrameListener;
    private int dropFrameListenerThreshold = 0;
    private long frameIntervalNs;
    private int refreshRate;
    private final TraceConfig config;
    private final long timeSliceMs;
    private long frozenThreshold;
    private long highThreshold;
    private long middleThreshold;
    private long normalThreshold;
    private int droppedSum = 0;
    private long durationSum = 0;
    private Map<String, Long> lastResumeTimeMap = new HashMap<>();
    private Map<Integer, Window.OnFrameMetricsAvailableListener> frameListenerMap = new HashMap<>();


    public FrameTracer(TraceConfig traceConfig, boolean supportFrameMetrics) {
        this.config = traceConfig;
        useFrameMetrics = supportFrameMetrics;
        this.frameIntervalNs = UIThreadMonitor.getMonitor().getFrameIntervalNanos();
        this.timeSliceMs = config.getTimeSliceMs();
        this.isFPSEnable = config.isFPSEnable();
        this.frozenThreshold = config.getFrozenThreshold();
        this.highThreshold = config.getHighThreshold();
        this.normalThreshold = config.getNormalThreshold();
        this.middleThreshold = config.getMiddleThreshold();
        MatrixLog.i(TAG, "[init] frameIntervalMs:%s isFPSEnable:%s", frameIntervalNs, isFPSEnable);
        if (isFPSEnable) {
            addListener(new FPSCollector());
        }
    }

    public void addListener(IDoFrameListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(IDoFrameListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    @Override
    protected void onAlive() {
        super.onAlive();
        if (isFPSEnable) {
            if (!useFrameMetrics) {
                UIThreadMonitor.getMonitor().addObserver(this);
            }
            Matrix.with().getApplication().registerActivityLifecycleCallbacks(this);
        }
    }

    @Override
    protected void onDead() {
        super.onDead();
        removeDropFrameListener();
        if (isFPSEnable) {
            UIThreadMonitor.getMonitor().removeObserver(this);
            Matrix.with().getApplication().unregisterActivityLifecycleCallbacks(this);
        }
    }

    @Override
    public void doFrame(String focusedActivity, long startNs, long endNs, boolean isVsyncFrame, long intendedFrameTimeNs, long inputCostNs, long animationCostNs, long trasversalCostNs) {
        if (isForeground()) {
            notifyListener(focusedActivity, startNs, endNs, isVsyncFrame, intendedFrameTimeNs, inputCostNs, animationCostNs, trasversalCostNs);
        }
    }

    private void notifyListener(final String focusedActivity, final long startNs, final long endNs, final boolean isVsyncFrame,
                                final long intendedFrameTimeNs, final long inputCostNs, final long animationCostNs, final long traversalCostNs) {
        long traceBegin = System.currentTimeMillis();
        try {
            final long jitter = endNs - intendedFrameTimeNs;
            //整个绘制过程的帧数： 实际上一帧的耗时/理论每一帧的时间。因此，就是卡顿导致的掉帧数
            final int dropFrame = (int)(jitter / frameIntervalNs);
            if (dropFrameListener != null) {
                if (dropFrame > dropFrameListenerThreshold) {
                    try {
                        if (AppActiveMatrixDelegate.getTopActivityName() != null) {
                            long lastResumeTime = lastResumeTimeMap.get(AppActiveMatrixDelegate.getTopActivityName());
                            dropFrameListener.dropFrame(dropFrame, jitter, AppActiveMatrixDelegate.getTopActivityName(), lastResumeTime);
                        }
                    } catch (Exception e) {
                        MatrixLog.e(TAG, "dropFrameListener error e:" + e.getMessage());
                    }
                }
            }

            droppedSum += dropFrame;
            durationSum += Math.max(jitter, frameIntervalNs);
            synchronized (listeners) {
                for (final IDoFrameListener listener : listeners) {
                    if (config.isDevEnv()) {
                        listener.time = SystemClock.uptimeMillis();
                    }
                    if (null != listener.getExecutor()) {
                        if (listener.getIntervalFrameReplay() > 0) {
                            listener.collect(focusedActivity, startNs, endNs, dropFrame, isVsyncFrame,
                                    intendedFrameTimeNs, inputCostNs, animationCostNs, traversalCostNs);
                        } else {
                            listener.getExecutor().execute(new Runnable() {
                                @Override
                                public void run() {
                                    listener.doFrameAsync(focusedActivity, startNs, endNs, dropFrame, isVsyncFrame,
                                            intendedFrameTimeNs, inputCostNs, animationCostNs, traversalCostNs);
                                }
                            });
                        }
                    } else {
                        listener.doFrameSync(focusedActivity, startNs, endNs, dropFrame, isVsyncFrame,
                                intendedFrameTimeNs, inputCostNs, animationCostNs, traversalCostNs);
                    }

                    if (config.isDevEnv()) {
                        listener.time = SystemClock.uptimeMillis() - listener.time;
                        MatrixLog.d(TAG, "[notifyListener] cost:%sms listener:%s", listener.time, listener);
                    }
                }
            }
        } finally {
            long cost = System.currentTimeMillis() - traceBegin;
            if (config.isDebug() && cost > frameIntervalNs) {
                MatrixLog.w(TAG, "[notifyListener] warm! maybe do heavy work in doFrameSync! size:%s cost:%sms", listeners.size(), cost);
            }
        }
    }

        public void removeDropFrameListener() {
        this.dropFrameListener = null;
    }

    public interface DropFrameListener {
        void dropFrame(int droppedFrame, long jitter, String scene, long lastResumeTime);
    }


    private class FPSCollector extends IDoFrameListener {
        private Handler frameHandler = new Handler(MatrixHandlerThread.getDefaultHandler().getLooper());
        Executor executor = new Executor() {
            @Override
            public void execute(Runnable command) {
                frameHandler.post(command);
            }
        };
        private HashMap<String, FrameCollectItem> map = new HashMap<>();

    }

    private class FrameCollectItem {
        String visibleScene;
        long sumFrameCost;
        int sumFrame = 0;
        int sumDroppedFrames;

        int[] dropLevel = new int[DropStatus.values().length];
        int[] dropSum = new int[DropStatus.values().length];

        FrameCollectItem(String visibleScene) {
            this.visibleScene = visibleScene;
        }

        void collect(int droppedFrames) {
            float frameIntervalCost = 1f * FrameTracer.this.frameIntervalNs / Constants.TIME_MILLIS_TO_NANO;
            sumFrameCost += (droppedFrames + 1) * frameIntervalCost;
            sumDroppedFrames += droppedFrames;
            sumFrame++;
            if (droppedFrames >= frozenThreshold) {
                dropLevel[DropStatus.DROPPED_FROZEN.index]++;
                dropSum[DropStatus.DROPPED_FROZEN.index] += droppedFrames;
            } else if (droppedFrames >= highThreshold) {
                dropLevel[DropStatus.DROPPED_HIGH.index]++;
                dropSum[DropStatus.DROPPED_HIGH.index] += droppedFrames;
            } else if (droppedFrames >= middleThreshold) {
                dropLevel[DropStatus.DROPPED_MIDDLE.index]++;
                dropSum[DropStatus.DROPPED_MIDDLE.index] += droppedFrames;
            } else if (droppedFrames >= normalThreshold) {
                dropLevel[DropStatus.DROPPED_NORMAL.index]++;
                dropSum[DropStatus.DROPPED_NORMAL.index] += droppedFrames;
            } else {
                dropLevel[DropStatus.DROPPED_BEST.index]++;
                dropSum[DropStatus.DROPPED_BEST.index] += Math.max(droppedFrames, 0);
            }
        }

        void report() {
            float fps = Math.min(refreshRate, 1000.f * sumFrame / sumFrameCost);
            MatrixLog.i(TAG, "[report] FPS:%s %s", fps, toString());
            try {
                TracePlugin plugin = Matrix.with().getPluginByClass(TracePlugin.class);
                if (null == plugin) {
                    return;
                }
                JSONObject dropLevelObject = new JSONObject();
                dropLevelObject.put(DropStatus.DROPPED_FROZEN.name(), dropLevel[DropStatus.DROPPED_FROZEN.index]);
                dropLevelObject.put(DropStatus.DROPPED_HIGH.name(), dropLevel[DropStatus.DROPPED_HIGH.index]);
                dropLevelObject.put(DropStatus.DROPPED_MIDDLE.name(), dropLevel[DropStatus.DROPPED_MIDDLE.index]);
                dropLevelObject.put(DropStatus.DROPPED_NORMAL.name(), dropLevel[DropStatus.DROPPED_NORMAL.index]);
                dropLevelObject.put(DropStatus.DROPPED_BEST.name(), dropLevel[DropStatus.DROPPED_BEST.index]);

                JSONObject dropSumObject = new JSONObject();
                dropSumObject.put(DropStatus.DROPPED_FROZEN.name(), dropSum[DropStatus.DROPPED_FROZEN.index]);
                dropSumObject.put(DropStatus.DROPPED_HIGH.name(), dropSum[DropStatus.DROPPED_HIGH.index]);
                dropSumObject.put(DropStatus.DROPPED_MIDDLE.name(), dropSum[DropStatus.DROPPED_MIDDLE.index]);
                dropSumObject.put(DropStatus.DROPPED_NORMAL.name(), dropSum[DropStatus.DROPPED_NORMAL.index]);
                dropSumObject.put(DropStatus.DROPPED_BEST.name(), dropSum[DropStatus.DROPPED_BEST.index]);

                JSONObject resultObject = new JSONObject();
                resultObject = DeviceUtil.getDeviceInfo(resultObject, plugin.getApplication());

                resultObject.put(SharePluginInfo.ISSUE_SCENE, visibleScene);
                resultObject.put(SharePluginInfo.ISSUE_DROP_LEVEL, dropLevelObject);
                resultObject.put(SharePluginInfo.ISSUE_DROP_SUM, dropSumObject);
                resultObject.put(SharePluginInfo.ISSUE_FPS, fps);

                Issue issue = new Issue();
                issue.setTag(SharePluginInfo.TAG_PLUGIN_FPS);
                issue.setContent(resultObject);
                plugin.onDetectIssue(issue);

            } catch (JSONException e) {
                MatrixLog.e(TAG, "json error", e);
            } finally {
                sumFrame = 0;
                sumDroppedFrames = 0;
                sumFrameCost = 0;
            }
        }

        @Override
        public String toString() {
            return "visibleScene=" + visibleScene
                    + ", sumFrame=" + sumFrame
                    + ", sumDroppedFrames=" + sumDroppedFrames
                    + ", sumFrameCost=" + sumFrameCost
                    + ", dropLevel=" + Arrays.toString(dropLevel);
        }
    }

    public enum DropStatus {
        DROPPED_FROZEN(4), DROPPED_HIGH(3), DROPPED_MIDDLE(2), DROPPED_NORMAL(1), DROPPED_BEST(0);
        public int index;
        DropStatus(int index) {
            this.index = index;
        }
    }

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
}
