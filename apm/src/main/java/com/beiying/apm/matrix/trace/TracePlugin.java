package com.beiying.apm.matrix.trace;

import android.app.Application;
import android.os.Build;
import android.os.Looper;

import com.beiying.apm.matrix.MatrixHandlerThread;
import com.beiying.apm.matrix.MatrixLog;
import com.beiying.apm.matrix.plugin.Plugin;
import com.beiying.apm.matrix.plugin.PluginListener;
import com.beiying.apm.matrix.trace.core.AppMethodBeat;
import com.beiying.apm.matrix.trace.core.UIThreadMonitor;
import com.beiying.apm.matrix.trace.tracer.EvilMethodTracer;
import com.beiying.apm.matrix.trace.tracer.FrameTracer;
import com.beiying.apm.matrix.trace.tracer.IdleHandlerLagTracer;
import com.beiying.apm.matrix.trace.tracer.LooperAnrTracer;
import com.beiying.apm.matrix.trace.tracer.SignalAnrTracer;
import com.beiying.apm.matrix.trace.tracer.StartupTracer;
import com.beiying.apm.matrix.trace.tracer.ThreadPriorityTracer;
import com.beiying.apm.matrix.trace.tracer.TouchEventLagTracer;

/**
 * TracePlugin 包含了四个 Tracer，暂且取名为追踪器：
 *
 * EvilMethodTracer：慢函数追踪器，检测影响主线程执行的慢函数；
 *      慢方法监测的原理是在 Looper 分发消息时，计算分发耗时（endMs - beginMs），如果大于阈值（可通过 IDynamicConfig 设置，默认为 700ms），就收集信息并上报。
 * StartupTracer：启动追踪器；
 *      启动监测原理：以第一个执行的方法为起点记录时间戳，接着记录第一个 Activity 或 Service 或 Receiver 启动时的时间戳，作为 Application 启动的结束时间。
 *      最后以主 Activity（闪屏页之后的第一个 Activity）的 onWindowFocusChange 方法作为终点，记录时间戳。Activity 的启动耗时可以通过
 *      onWindowFocusChange 方法回调时的时间戳减去其启动时的时间戳。收集到上述信息之后即可统计启动耗时。
 * FrameTracer：帧率追踪器；
 *      帧率监测的原理是监听 Choreographer，在所有回调都执行完毕后计算当前总共花费的时间，从而计算掉帧数及掉帧程度，当同一个 Activity/Fragment
 *      掉帧程度超过阈值时，就上报问题。但 Matrix 的计算方法存在问题，可能出现频繁上报的情况，需要自行手动过滤。
 *      具体实现：通过添加 input、animation、traversal 等各个类型的回调到 Choreographer 头部实现的，vsync 信号触发后，
 *      Choreographer 中各个类型的回调会被执行，两种类型的回调的开始时间的间隔就相当于第一种类型的事件的耗时
 *      （即 input.cost = animation.begin - input.begiin），最后一种事件（traversal）执行完毕后，Looper 的 diaptchEnd 方法也会被执行，
 *      因此 traversal.cost = Looper.dispatchEnd -traversal.begin。
 * AnrTracer：ANR 追踪器。
 *      ANR检测原理：在 Looper 开始分发消息时，往后台线程插入一个延时（5s 后执行）任务，Looper 消息分发完毕后就删除，如果过了 5s，该任务未被删除，
 *      就认为出现了 ANR，收集信息，报告问题
 *
 * UIThreadMonitor：一个监听 UI 线程的监视器，是实现四个追踪器的基础。
 * */
public class TracePlugin extends Plugin {
    private static final String TAG = "Matrix.TracePlugin";
    private final TraceConfig traceConfig;
    private LooperAnrTracer looperAnrTracer;
    private EvilMethodTracer evilMethodTracer;
    private FrameTracer frameTracer;
    private StartupTracer startupTracer;//启动监控
    private SignalAnrTracer signalAnrTracer;//信号anr的监控
    private IdleHandlerLagTracer idleHandlerLagTracer;//idleHandler监控
    private TouchEventLagTracer touchEventLagTracer;
    private ThreadPriorityTracer threadPriorityTracer;//线程优先级的监控


    private static boolean supportFrameMetrics;

    public TracePlugin(TraceConfig config) {
        this.traceConfig = config;
    }

    @Override
    public void init(Application app, PluginListener listener) {
        super.init(app, listener);
        int sdkInt = Build.VERSION.SDK_INT;
        if (sdkInt < Build.VERSION_CODES.JELLY_BEAN) {
            MatrixLog.e(TAG, "[FrameBeat] API is low Build.VERSION_CODES.JELLY_BEAN(16), TracePlugin is not supported");
            unSupportPlugin();
            return;
        } else if (sdkInt >= Build.VERSION_CODES.O) {
            supportFrameMetrics = true;
        }

        looperAnrTracer = new LooperAnrTracer(traceConfig);
        evilMethodTracer = new EvilMethodTracer(traceConfig);
        frameTracer = new FrameTracer(traceConfig, supportFrameMetrics);
        startupTracer = new StartupTracer(traceConfig);

    }

    @Override
    public void start() {
        super.start();
        if (!isSupported()) {
            MatrixLog.w(TAG, "[start] Plugin is unSupported!");
            return;
        }
        MatrixLog.w(TAG, "start trace!");
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (willUiThreadMonitorRunning(traceConfig)) {
                    if (!UIThreadMonitor.getMonitor().isInit()) {
                        //UI线程监控初始化
                        UIThreadMonitor.getMonitor().init(traceConfig, supportFrameMetrics);
                    }
                }

                // AppMethodBeat  开启慢方法监听
                if(traceConfig.isAppMethodBeatEnable()) {
                    AppMethodBeat.getInstance().onStart();
                } else {
                    AppMethodBeat.getInstance().forceStop();
                }

                UIThreadMonitor.getMonitor().onStart();

                if (traceConfig.isAnrTraceEnable()) {
                    looperAnrTracer.onStartTrace();
                }
                if (traceConfig.isIdleHandlerTraceEnable()) {
                    idleHandlerLagTracer = new IdleHandlerLagTracer(traceConfig);
                    idleHandlerLagTracer.onStartTrace();
                }

                if (traceConfig.isTouchEventTraceEnable()) {
                    touchEventLagTracer = new TouchEventLagTracer(traceConfig);
                    touchEventLagTracer.onStartTrace();
                }

                if (traceConfig.isSignalAnrTraceEnable()) {
                    if (!SignalAnrTracer.hasInstance) {
                        signalAnrTracer = new SignalAnrTracer(traceConfig);
                        signalAnrTracer.onStartTrace();
                    }
                }

                if (traceConfig.isMainThreadPriorityTraceEnable()) {
                    threadPriorityTracer = new ThreadPriorityTracer();
                    threadPriorityTracer.onStartTrace();
                }

                if (traceConfig.isFPSEnable()) {
                    frameTracer.onStartTrace();
                }

                if (traceConfig.isEvilMethodTraceEnable()) {
                    evilMethodTracer.onStartTrace();
                }

                if (traceConfig.isStartupEnable()) {
                    startupTracer.onStartTrace();
                }
            }
        };
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            runnable.run();
        } else {
            MatrixHandlerThread.getDefaultMainHandler().post(runnable);
        }
    }

    private boolean willUiThreadMonitorRunning(TraceConfig traceConfig) {
        return traceConfig.isEvilMethodTraceEnable() || traceConfig.isAnrTraceEnable() || traceConfig.isFPSEnable();
    }
}
