package com.beiying.apm.matrix.trace.core;

import android.os.Looper;
import android.os.SystemClock;
import android.view.Choreographer;

import com.beiying.apm.matrix.AppActiveMatrixDelegate;
import com.beiying.apm.matrix.MatrixLog;
import com.beiying.apm.matrix.util.ReflectUtils;
import com.beiying.apm.matrix.trace.Constants;
import com.beiying.apm.matrix.trace.TraceConfig;
import com.beiying.apm.matrix.util.TraceUtils;

import org.aspectj.weaver.Utils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;

/**
 * 两个核心
 * Choreographer 可以简单理解为帧率监听器；
 *      通过反射方式去拿到Choreographer的队列和回调监听，从而实现自己的逻辑
 *
 * LooperMonitor Looper 监听。
 *      监听looper中消息处理前后的逻辑
 *
 * 工作流程如下：
 * 1. 主线程Looper loop 到一个  target是  Choreographer$FrameHandler  massage 是 Choreographer$FrameDisplayEventReceiver 的消息（要能触发 刷新帧）
 * 2. 调用 dispatchBegin() 方法
 * 3. 调用 run方法
 * 4. 回掉 doQueueBegin type=0（input）
 * 5. 回掉 doQueueEnd type=0（input）
 * 6. 回掉 doQueueBegin type=1（animation）
 * 7. 回掉 doQueueEnd type=1（animation）
 * 8. 回掉 doQueueBegin type=2（traversal）
 * 9. message处理完毕
 * 10. 调用 dispatchEnd() 方法
 * 11. 回掉 doQueueEnd type=2（traversal）
 * */
public class UIThreadMonitor implements BeatLifecycle, Runnable {
    private static final String TAG = "Matrix.UIThreadMonitor";
    private static final String ADD_CALLBACK = "addCallbackLocked";
    private final static UIThreadMonitor sInstance = new UIThreadMonitor();
    private volatile boolean isAlive = false;
    /**
     * 0: dispatch 的起始时间
     * 1：dispatch 的结束时间
     * 2: dispatch 的起始时 当前线程时间
     * 3: dispatch 的结束时 当前线程时间
     */
    private long[] dispatchTimeMs = new long[4];
    private final HashSet<LooperObserver> observers = new HashSet<>();
    private volatile long token = 0L;

    //是否属于 刷新帧消息 ， run方法只有主线程中Loop到的 message 对象 是 请求刷新 frame 的 时候 才会回调。
    //应该只是个标志位，对接入方 无感
    private boolean isVsyncFrame = false;
    // The time of the oldest input event
    private static final int OLDEST_INPUT_EVENT = 3;

    // The time of the newest input event
    private static final int NEWEST_INPUT_EVENT = 4;

    /**
     * Callback type: Input callback.  Runs first.
     *
     * @hide
     */
    public static final int CALLBACK_INPUT = 0;

    /**
     * Callback type: Animation callback.  Runs before traversals.
     *
     * @hide
     */
    public static final int CALLBACK_ANIMATION = 1;

    /**
     * Callback type: Commit callback.  Handles post-draw operations for the frame.
     * Runs after traversal completes.
     *
     * @hide
     */
    public static final int CALLBACK_TRAVERSAL = 2;

    /**
     * never do queue end code
     */
    public static final int DO_QUEUE_END_ERROR = -100;

    private static final int CALLBACK_LAST = CALLBACK_TRAVERSAL;
    private TraceConfig config;
    private static boolean useFrameMetrics;
    private Object callbackQueueLock;
    private Object[] callbackQueues;
    private Method addTraversalQueue;
    private Method addInputQueue;
    private Method addAnimationQueue;
    private Choreographer choreographer;
    private Object vsyncReceiver;
    private long frameIntervalNanos = 16666666;
    //这个int[]数组用来保存UI渲染一帧中的三个阶段(input、animation、traversal)的开始和结束状态。
    private int[] queueStatus = new int[CALLBACK_LAST + 1];
    //type对应的 callback 是否还存在，因为callback 执行一次就 被回收了
    private boolean[] callbackExist = new boolean[CALLBACK_LAST + 1]; // ABA
    //这个long[]数组则是保存每一帧的三个阶段每个阶段对应的执行耗时
    private long[] queueCost = new long[CALLBACK_LAST + 1];
    private boolean isInit = false;
    private static final int DO_QUEUE_DEFAULT = 0;
    private static final int DO_QUEUE_BEGIN = 1;
    private static final int DO_QUEUE_END = 2;

    public static UIThreadMonitor getMonitor() {
        return sInstance;
    }

    public boolean isInit() {
        return isInit;
    }

    public void init(TraceConfig config, boolean supportFrameMetrics) {
        this.config = config;
        useFrameMetrics = supportFrameMetrics;
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            throw new AssertionError("must be init in main thread!");
        }

        boolean historyMsgRecorder = config.historyMsgRecorder;
        boolean denseMsgTracer = config.denseMsgTracer;

        //当looper中的消息分发开始和结束的时候会触发回调
        LooperMonitor.register(new LooperMonitor.LooperDispatchListener() {
            @Override
            public boolean isValid() {
                return isAlive;
            }

            @Override
            public void dispatchStart() {
                super.dispatchStart();
                UIThreadMonitor.this.dispatchBegin();
            }

            @Override
            public void dispatchEnd() {
                super.dispatchEnd();
                UIThreadMonitor.this.dispatchEnd();
            }
        });
        this.isInit = true;
        //反射得到mFrameIntervalNanos，帧间时长，一般等于16.66ms。用来计算掉帧率
        frameIntervalNanos = ReflectUtils.reflectObject(choreographer, "mFrameIntervalNanos", Constants.DEFAULT_FRAME_DURATION);
        if (!useFrameMetrics) {
            //反射得到 Choreographer 的mLock锁对象，目的添加自己的callback的时候用来作为锁对象，保持跟系统锁对象的一致。
            callbackQueueLock = ReflectUtils.reflectObject(choreographer, "mLock", new Object());
            //反射得到mCallbackQueues数组对象，用来获取具体类型的CallbackRecord中的callback队列（队列底层是通过链表实现的）
            callbackQueues = ReflectUtils.reflectObject(choreographer, "mCallbackQueues", null);
            if (null != callbackQueues) {
                //反射获得 callbackQueues 中第一个 CallbackQueue对象的 addCallbackLocked 的方法
                // 第一个 CallbackQueue 是处理 input事件的
                addInputQueue = ReflectUtils.reflectMethod(callbackQueues[CALLBACK_INPUT], ADD_CALLBACK, long.class, Object.class, Object.class);

                //反射获得 callbackQueues 中第二个 CallbackQueue对象的 addCallbackLocked 的方法
                // 第二个 CallbackQueue 是处理 动画的
                addAnimationQueue = ReflectUtils.reflectMethod(callbackQueues[CALLBACK_ANIMATION], ADD_CALLBACK, long.class, Object.class, Object.class);

                //反射获得 callbackQueues 中第三个 CallbackQueue对象的 addCallbackLocked 的方法
                // 第三个 CallbackQueue 是绘制完 用于回调的
                addTraversalQueue = ReflectUtils.reflectMethod(callbackQueues[CALLBACK_TRAVERSAL], ADD_CALLBACK, long.class, Object.class, Object.class);
            }

            //反射得到onVsync接收器,用来反射获取onVsync回调时候的mTimestampNanos，目的是跟系统回调同步时间。
            vsyncReceiver = ReflectUtils.reflectObject(choreographer, "mDisplayEventReceiver", null);

            MatrixLog.i(TAG, "[UIThreadMonitor] %s %s %s %s %s %s frameIntervalNanos:%s", callbackQueueLock == null, callbackQueues == null,
                    addInputQueue == null, addTraversalQueue == null, addAnimationQueue == null, vsyncReceiver == null, frameIntervalNanos);

            if (config.isDevEnv()) {
                addObserver(new LooperObserver() {
                    @Override
                    public void doFrame(String focusedActivity, long startNs, long endNs, boolean isVsyncFrame, long intendedFrameTimeNs, long inputCostNs, long animationCostNs, long traversalCostNs) {
                        MatrixLog.i(TAG, "focusedActivity[%s] frame cost:%sms isVsyncFrame=%s intendedFrameTimeNs=%s [%s|%s|%s]ns",
                                focusedActivity, (endNs - startNs) / Constants.TIME_MILLIS_TO_NANO, isVsyncFrame, intendedFrameTimeNs, inputCostNs, animationCostNs, traversalCostNs);
                    }
                });
            }
        }
    }

    public long getFrameIntervalNanos() {
        return frameIntervalNanos;
    }

    public void addObserver(LooperObserver observer) {
        if (!isAlive) {
            onStart();
        }
        synchronized (observers) {
            observers.add(observer);
        }
    }

    public void removeObserver(LooperObserver observer) {
        synchronized (observers) {
            observers.remove(observer);
            if (observers.isEmpty()) {
                onStop();
            }
        }
    }

    @Override
    public synchronized void onStart() {
        if (!isInit) {
            MatrixLog.e(TAG, "[onStart] is never init.");
            return;
        }
        if (!isAlive) {
            this.isAlive = true;
            synchronized (this) {
                MatrixLog.i(TAG, "[onStart] callbackExist:%s %s", Arrays.toString(callbackExist), TraceUtils.getStack());
                callbackExist = new boolean[CALLBACK_LAST + 1];
            }
            if (!useFrameMetrics) {
                queueStatus = new int[CALLBACK_LAST + 1];
                queueCost = new long[CALLBACK_LAST + 1];
                addFrameCallback(CALLBACK_INPUT, this, true);
            }
        }
    }

    //往input类型的链表中加入一个input阶段的callback。
    private void addFrameCallback(int type, Runnable callback, boolean isAddHeader) {
        if (callbackExist[type]) {
            MatrixLog.w(TAG, "[addFrameCallback] this type %s callback has exist! isAddHeader:%s", type, isAddHeader);
            return;
        }
        if (!isAlive && type == CALLBACK_INPUT) {
            MatrixLog.w(TAG, "[addFrameCallback] UIThreadMonitor is not alive!");
            return;
        }
        try {
            synchronized (callbackQueueLock) {
                Method method = null;
                switch (type) {
                    case CALLBACK_INPUT:
                        method = addInputQueue;
                        break;
                    case CALLBACK_ANIMATION:
                        method = addAnimationQueue;
                        break;
                    case CALLBACK_TRAVERSAL:
                        method = addTraversalQueue;
                        break;
                }
                if (null != method) {
                    //最终method就是 CallbackQueue的 addCallbackLocked 方法, 反射调用callback加入type对应的链表结构中
                    method.invoke(callbackQueues[type], isAddHeader ? SystemClock.uptimeMillis() : -1, callback, null);
                    callbackExist[type] = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            MatrixLog.e(TAG, e.toString());
        }
    }

    @Override
    public void run() {
        final long start = System.nanoTime();
        try {
            doFrameBegin(token);
            doQueueBegin(CALLBACK_INPUT);
            addFrameCallback(CALLBACK_ANIMATION, new Runnable() {
                @Override
                public void run() {
                    doQueueEnd(CALLBACK_INPUT);
                    doQueueBegin(CALLBACK_ANIMATION);
                }
            }, true);
            addFrameCallback(CALLBACK_TRAVERSAL, new Runnable() {
                @Override
                public void run() {
                    doQueueEnd(CALLBACK_ANIMATION);
                    doQueueBegin(CALLBACK_TRAVERSAL);
                }
            }, true);
        } finally {
            if (config.isDevEnv()) {
                MatrixLog.d(TAG, "[UIThreadMonitor#run] inner cost:%sns", System.nanoTime() - start);
            }
        }
    }

    @Override
    public void onStop() {

    }

    @Override
    public boolean isAlive() {
        return isAlive;
    }

    public long getQueueCost(int type, long token) {
        if (token != this.token) {
            return -1;
        }
        return queueStatus[type] == DO_QUEUE_END ? queueCost[type] : 0;
    }

    private void doFrameBegin(long token) {
        this.isVsyncFrame = true;
    }

    private void doFrameEnd(long token) {
        
    }

    private void doQueueBegin(int type) {
        queueStatus[type] = DO_QUEUE_BEGIN;
        queueCost[type] = System.nanoTime();
    }

    private void doQueueEnd(int type) {
        queueStatus[type] = DO_QUEUE_END;
        queueCost[type] = System.nanoTime() - queueCost[type];
        synchronized (this) {
            callbackExist[type] = false;
        }
    }

    private void dispatchBegin() {
        token = dispatchTimeMs[0] = System.nanoTime();
        dispatchTimeMs[2] = SystemClock.currentThreadTimeMillis();
        if (config.isAppMethodBeatEnable()) {
            AppMethodBeat.i(AppMethodBeat.METHOD_ID_DISPATCH);
        }

        synchronized (observers) {
            for (LooperObserver observer : observers) {
                if (!observer.isDispatchBegin()) {
                    observer.dispatchBegin(dispatchTimeMs[0], dispatchTimeMs[2], token);
                }
            }
        }
        if (config.isDevEnv()) {
            MatrixLog.d(TAG, "[dispatchBegin#run] inner cost:%sns", System.nanoTime() - token);
        }
    }

    private void dispatchEnd() {
        long traceBegin = 0;
        if (config.isDevEnv()) {
            traceBegin = System.nanoTime();
        }

        if (config.isFPSEnable() && !useFrameMetrics) {
            long startNs = token;
            long intendedFrameTimeNs = startNs;
            if (isVsyncFrame) {
                doFrameEnd(token);
                intendedFrameTimeNs = getIntendedFrameTimeNs(startNs);
            }
            long endNs = System.nanoTime();
            synchronized (observers) {
                for (LooperObserver observer : observers) {
                    observer.doFrame(AppActiveMatrixDelegate.INSTANCE.getVisibleScene(), startNs, endNs, isVsyncFrame,
                            intendedFrameTimeNs, queueCost[CALLBACK_INPUT], queueCost[CALLBACK_ANIMATION], queueCost[CALLBACK_TRAVERSAL]);
                }
            }
        }

        if (config.isEvilMethodTraceEnable() || config.isDevEnv()) {
            dispatchTimeMs[3] = SystemClock.currentThreadTimeMillis();
            dispatchTimeMs[1] = System.nanoTime();
        }

        AppMethodBeat.o(AppMethodBeat.METHOD_ID_DISPATCH);

        synchronized (observers) {
            for (LooperObserver observer : observers) {
                observer.dispatchEnd(dispatchTimeMs[0], dispatchTimeMs[2], dispatchTimeMs[1], dispatchTimeMs[3], token, isVsyncFrame);
            }
        }
    }
    
    private long getIntendedFrameTimeNs(long defaultValue) {
        try {
            return ReflectUtils.reflectObject(vsyncReceiver, "mTimestampNanos", defaultValue);
        } catch (Exception e) {
            e.printStackTrace();
            MatrixLog.e(TAG, e.toString());
        }
        return defaultValue;
    }
}
