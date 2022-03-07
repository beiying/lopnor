package com.beiying.apm.matrix.trace.core;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.MessageQueue;
import android.os.SystemClock;
import android.util.Log;
import android.util.Printer;

import androidx.annotation.NonNull;

import com.beiying.apm.matrix.MatrixHandlerThread;
import com.beiying.apm.matrix.MatrixLog;
import com.beiying.apm.matrix.util.ReflectUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 感知到主线程Looper开始分发和结束分发Message的时间。
 * Looper 的监控是由类 LooperMonitor 实现的，原理很简单，利用Looper的next()会在消息处理的前后打印消息，我们只要能够监听到这两条消息之间即可计算出这条消息的耗时！
 * 为主线程 Looper 设置一个 Printer 即可，但值得一提的是，LooperMonitor 不会直接设置 Printer，而是先获取旧对象，并创建代理对象，
 * 避免影响到其它用户设置的 Printer
 *
 * */
public class LooperMonitor implements MessageQueue.IdleHandler {
    private static final String TAG = "Matrix.LooperMonitor";
    private static final int HISTORY_QUEUE_MAX_SIZE = 200;
    private static final int RECENT_QUEUE_MAX_SIZE = 5000;

    //LooperMonitor不仅仅可以监听主线程还可以监听其他线程的消息卡顿时间。只要传入不同线程的looper即可。
    private static final Map<Looper, LooperMonitor> sLooperMonitorMap = new ConcurrentHashMap<>();
    private static final LooperMonitor sMainMonitor = LooperMonitor.of(Looper.getMainLooper());
    private final HashSet<LooperDispatchListener> listeners = new HashSet<>();
    private static final HandlerThread historyMsgHandlerThread = MatrixHandlerThread.getNewHandlerThread("historyMsgHandlerThread", HandlerThread.NORM_PRIORITY);
    private static final Handler historyMsgHandler = new Handler(historyMsgHandlerThread.getLooper());
    private static final Queue<M> anrHistoryMQ = new ConcurrentLinkedQueue<>();
    private static final Queue<M> recentMsgQ = new ConcurrentLinkedQueue<>();
    private LooperPrinter printer;
    private Looper looper;
    private static final long CHECK_TIME = 60 * 1000L;
    private long lastCheckPrinterTime = 0;
    private static long messageStartTime = 0;

    private static String latestMsgLog = "";
    private static long recentMCount = 0;
    private static long recentMDuration = 0;

    public abstract static class LooperDispatchListener {
        boolean isHasDispatchStart = false;
        boolean historyMsgRecorder = false;
        boolean denseMsgTracer = false;

        public LooperDispatchListener(boolean historyMsgRecorder, boolean denseMsgTracer) {
            this.historyMsgRecorder = historyMsgRecorder;
            this.denseMsgTracer = denseMsgTracer;
        }

        public LooperDispatchListener() {

        }

        public boolean isValid() {
            return false;
        }


        public void dispatchStart() {

        }

        public void dispatchEnd() {

        }

        public void onDispatchStart(String x) {
            this.isHasDispatchStart = true;
            dispatchStart();
        }

        public void onDispatchEnd(String x) {
            this.isHasDispatchStart = false;
            dispatchEnd();
        }
    }

    private LooperMonitor(Looper looper) {
        Objects.requireNonNull(looper);
        this.looper = looper;
        //代替looper中的mLogging对象
        resetPrinter();
        //往IdleHandler添加一个 外部handler，用来在空闲的时候做逻辑
        addIdleHandler(looper);
    }

    public Looper getLooper() {
        return looper;
    }

    public static LooperMonitor of(@NonNull Looper looper) {
        LooperMonitor looperMonitor = sLooperMonitorMap.get(looper);
        if (looperMonitor == null) {
            looperMonitor = new LooperMonitor(looper);
            sLooperMonitorMap.put(looper, looperMonitor);
        }
        return looperMonitor;
    }

    static void register(LooperDispatchListener listener) {
        sMainMonitor.addListener(listener);
    }

    static void unregister(LooperDispatchListener listener) {
        sMainMonitor.removeListener(listener);
    }

    public void addListener(LooperDispatchListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(LooperDispatchListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
    private static boolean isReflectLoggingError = false;
    /**
     * 替换Printer
     * */
    private synchronized void resetPrinter() {
        Printer originPrinter = null;
        try {
            if (!isReflectLoggingError) {
                originPrinter = ReflectUtils.get(looper.getClass(), "mLogging", looper);
                if (originPrinter == printer && null != printer) {
                    return;
                }
                // Fix issues that printer loaded by different classloader
                //与之前反射过的printer进行对比，一致则返回。一般情况下都是一致的，除非有不同classloader加载过。
                if (originPrinter != null && printer != null) {
                    if (originPrinter.getClass().getName().equals(printer.getClass().getName())) {
                        MatrixLog.w(TAG, "LooperPrinter might be loaded by different classloader"
                                + ", my = " + printer.getClass().getClassLoader()
                                + ", other = " + originPrinter.getClass().getClassLoader());
                        return;
                    }
                }
            }
        } catch (Exception e) {
            isReflectLoggingError = true;
            e.printStackTrace();
            MatrixLog.e(TAG, "[resetPrinter] %s", e);
        }
        if (null != printer) {
            MatrixLog.w(TAG, "maybe thread:%s printer[%s] was replace other[%s]!",
                    looper.getThread().getName(), printer, originPrinter);
        }
        looper.setMessageLogging(printer = new LooperPrinter(originPrinter));
        if (null != originPrinter) {
            MatrixLog.i(TAG, "reset printer, originPrinter[%s] in %s", originPrinter, looper.getThread().getName());
        }
    }

    private synchronized void addIdleHandler(Looper looper) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            looper.getQueue().addIdleHandler(this);
        } else {
            try {
                MessageQueue queue = ReflectUtils.get(looper.getClass(), "mQueue", looper);
                queue.addIdleHandler(this);
            } catch (Exception e) {
                Log.e(TAG, "[removeIdleHandler] %s", e);
            }
        }
    }
    //每次消息处理后的ideleHandler回调中每60s再去调用resetPrinter（）方法，确保是我们自己的Printer对象。
    @Override
    public boolean queueIdle() {
        if (SystemClock.uptimeMillis() - lastCheckPrinterTime >= CHECK_TIME) {
            resetPrinter();
            lastCheckPrinterTime = SystemClock.uptimeMillis();
        }
        return true;
    }

    private void dispatch(boolean isBegin, String log) {
        synchronized (listeners) {
            for (LooperDispatchListener listener : listeners) {
                if (listener.isValid()) {
                    if (isBegin) {
                        if (!listener.isHasDispatchStart) {
                            if (listener.historyMsgRecorder) {
                                messageStartTime = System.currentTimeMillis();
                                latestMsgLog = log;
                                recentMCount++;
                            }
                            listener.onDispatchStart(log);
                        }
                    } else {
                        if (listener.isHasDispatchStart) {
                            if (listener.historyMsgRecorder) {
                                recordMsg(log, System.currentTimeMillis() - messageStartTime, listener.denseMsgTracer);
                            }
                            listener.onDispatchEnd(log);
                        }
                    }
                } else if(!isBegin && listener.isHasDispatchStart) {
                    listener.dispatchEnd();
                }
            }
        }
    }

    private static void recordMsg(final String log, final long duration, boolean denseMsgTracer) {
        historyMsgHandler.post(new Runnable() {
            @Override
            public void run() {
                enqueueHistoryMQ(new M(log, duration));
            }
        });
        if (denseMsgTracer) {
            historyMsgHandler.post(new Runnable() {
                @Override
                public void run() {
                    enqueueRecentMQ(new M(log, duration));
                }
            });
        }
    }

    private static void enqueueRecentMQ(M m) {
        if (recentMsgQ.size() == RECENT_QUEUE_MAX_SIZE) {
            recentMsgQ.poll();
        }
        recentMsgQ.offer(m);
        recentMDuration += m.d;
    }

    private static void enqueueHistoryMQ(M m) {
        if (anrHistoryMQ.size() == HISTORY_QUEUE_MAX_SIZE) {
            anrHistoryMQ.poll();
        }
        anrHistoryMQ.offer(m);
    }

    public static Queue<M> getHistoryMQ() {
        enqueueHistoryMQ(new M(latestMsgLog, System.currentTimeMillis() - messageStartTime));
        return anrHistoryMQ;
    }

    public static Queue<M> getRecentMsgQ() {
        return recentMsgQ;
    }

    public static void cleanRecentMQ() {
        recentMsgQ.clear();
        recentMCount = 0;
        recentMDuration = 0;
    }

    public static long getRecentMCount() {
        return recentMCount;
    }

    public static long getRecentMDuration() {
        return recentMDuration;
    }

    class LooperPrinter implements Printer {
        public Printer origin;
        boolean isHasChecked = false;
        boolean isValid = false;

        LooperPrinter(Printer printer) {
            this.origin = printer;
        }

        //通过LooperPrinter中的println方法回调，来判断消息处理的开始和结束。从而分发给对应的监听者，实现逻辑处理
        @Override
        public void println(String x) {
            if (null != origin) {
                origin.println(x);
                if (origin == this) {
                    throw new RuntimeException(TAG + " origin == this");
                }
            }

            if (!isHasChecked) {
                isValid = x.charAt(0) == '>' || x.charAt(0) == '<';
                isHasChecked = true;
                if (!isValid) {
                    MatrixLog.e(TAG, "[println] Printer is inValid! x:%s", x);
                }
            }
            if (isValid) {
                //x.charAt(0) == '>'： 表示消息开始
                //harAt(0) == '<' 表示消息结束
                dispatch(x.charAt(0) == '>', x);
            }
        }
    }

    public static class M {
        public String l;
        public long d;
        M(String l, long d) {
            this.l = l;
            this.d = d;
        }

        @Override
        public String toString() {
            return "{" + l + " -> " + d + '}';
        }
    }

}
