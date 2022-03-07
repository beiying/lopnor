package com.beiying.apm.matrix.trace.core;

import android.app.Activity;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;

import com.beiying.apm.matrix.AppActiveMatrixDelegate;
import com.beiying.apm.matrix.MatrixHandlerThread;
import com.beiying.apm.matrix.MatrixLog;
import com.beiying.apm.matrix.trace.ActivityThreadHacker;
import com.beiying.apm.matrix.trace.Constants;
import com.beiying.apm.matrix.trace.listeners.IAppMethodBeatListener;
import com.beiying.apm.matrix.util.MatrixUtil;
import com.beiying.apm.matrix.util.TraceUtils;

import java.util.HashSet;
import java.util.Set;

public class AppMethodBeat implements BeatLifecycle{
    private static final String TAG = "Matrix.AppMethodBeat";
    public static boolean isDev = false;
    private static AppMethodBeat sInstance = new AppMethodBeat();
    //默认状态
    private static final int STATUS_DEFAULT = Integer.MAX_VALUE;
    //调用onStart()后的状态
    private static final int STATUS_STARTED = 2; //启动
    //第一次 执行 i 方法后的状态
    private static final int STATUS_READY = 1; // 准备好
    //调用onStop()后的状态
    private static final int STATUS_STOPPED = -1; //停止
    //启动已过期 当 在 realExecute 方法之后后 如果 15ms 内 AppMethodBeat 还没有被启动（onStart）就会被置为这种状态
    private static final int STATUS_EXPIRED_START = -2;
    //在 AppMethodBeat 类加载 15s 后，还没有使用（status的状态还是STATUS_DEFAULT），就会被置为这种状态
    private static final int STATUS_OUT_RELEASE = -3;//已释放


    private static volatile int status = STATUS_DEFAULT;
    private static final int METHOD_ID_MAX = 0xFFFFF;
    public static final int METHOD_ID_DISPATCH = METHOD_ID_MAX - 1;

    private final static Object statusLock = new Object();
    public static MethodEnterListener sMethodEnterListener;

    //long占用8byte 所以 sBuffer 占用内存大小为 8* BUFFER_SIZE(100 * 10000) =7.6
    private static long[] sBuffer = new long[Constants.BUFFER_SIZE];
    //将要向sBuffer 中插入元素 的下标
    private static int sIndex = 0;
    //已经向sBuffer 中插入元素 的下标
    private static int sLastIndex = -1;
    private static boolean assertIn = false;
    private volatile static long sCurrentDiffTime = SystemClock.uptimeMillis();
    private volatile static long sDiffTime = sCurrentDiffTime;
    private static long sMainThreadId = Looper.getMainLooper().getThread().getId();
    private static HandlerThread sTimerUpdateThread = MatrixHandlerThread.getNewHandlerThread("matrix_time_update_thread", Thread.MIN_PRIORITY + 2);
    private static Handler sHandler = new Handler(sTimerUpdateThread.getLooper());
    private static Set<String> sFocusActivitySet = new HashSet<>();
    private static final HashSet<IAppMethodBeatListener> listeners = new HashSet<>();
    private static final Object updateTimeLock = new Object();
    private static volatile boolean isPauseUpdateTime = false;
    private static Runnable checkStartExpiredRunnable = null;

    //AppMethodBeat注册looperMonitorListener监听到 LooperMonitor主要是:
    //1、为了在主线程空闲的情况下停止更新sCurrentDiffTime
    //2、在主线程开始工作时再开始更新sCurrentDiffTime以减少资源的占用。
    private static LooperMonitor.LooperDispatchListener looperMonitorListener = new LooperMonitor.LooperDispatchListener() {
        @Override
        public boolean isValid() {
            return status >= STATUS_READY;
        }

        @Override
        public void dispatchStart() {
            super.dispatchStart();
            AppMethodBeat.dispatchBegin();
        }

        @Override
        public void dispatchEnd() {
            super.dispatchEnd();
            AppMethodBeat.dispatchEnd();
        }
    };

    private static Runnable realReleaseRunnable = new Runnable() {
        @Override
        public void run() {
            realRelease();
        }
    };

    static {
        MatrixHandlerThread.getDefaultHandler().postDelayed(realReleaseRunnable, Constants.DEFAULT_RELEASE_BUFFER_DELAY);
    }

    /**
     * update time runnable
     */
    private static Runnable sUpdateDiffTimeRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                while (true) {
                    //无限循环  当isPauseUpdateTime=false（dispatchBegin方法完成）,然后更新 sCurrentDiffTime
                    //考虑到每个方法执行前后都获取系统时间（System.nanoTime）会对性能影响比较大，而实际上，单个函数执行耗时小于 5ms 的情况，
                    //对卡顿来说不是主要原因，可以忽略不计，如果是多次调用的情况，则在它的父级方法中可以反映出来，所以为了减少对性能的影响，
                    //通过另一条更新时间的线程每 5ms 去更新一个时间变量，而每个方法执行前后只读取该变量来减少性能损耗。
                    while (!isPauseUpdateTime && status > STATUS_STOPPED) {
                        sCurrentDiffTime = SystemClock.uptimeMillis() - sDiffTime;
                        SystemClock.sleep(Constants.TIME_UPDATE_CYCLE_MS);
                    }
                    synchronized (updateTimeLock) {
                        updateTimeLock.wait();
                    }
                }
            } catch (Exception e) {
                MatrixLog.e(TAG, "" + e.toString());
            }
        }
    };

    public static AppMethodBeat getInstance() {
        return sInstance;
    }

    public static long getDiffTime() {
        return sDiffTime;
    }

    public interface MethodEnterListener {
        void enter(int method, long threadId);
    }
    @Override
    public void onStart() {
        synchronized (statusLock) {
            if (status < STATUS_STARTED && status >= STATUS_EXPIRED_START) {
                sHandler.removeCallbacks(checkStartExpiredRunnable);
                MatrixHandlerThread.getDefaultHandler().removeCallbacks(realReleaseRunnable);
                if (sBuffer == null) {
                    throw new RuntimeException(TAG + " sBuffer == null");
                }
                MatrixLog.i(TAG, "[onStart] preStatus:%s", status, TraceUtils.getStack());
                status = STATUS_STARTED;
            } else {
                MatrixLog.w(TAG, "[onStart] current status:%s", status);
            }
        }
    }

    @Override
    public void onStop() {
        synchronized (statusLock) {
            if (status == STATUS_STARTED) {
                MatrixLog.i(TAG, "[onStop] %s", TraceUtils.getStack());
                status = STATUS_STOPPED;
            } else {
                MatrixLog.w(TAG, "[onStop] current status:%s", status);
            }
        }
    }

    public void forceStop() {
        synchronized (statusLock) {
            status = STATUS_STOPPED;
        }
    }

    @Override
    public boolean isAlive() {
        return status >= STATUS_STARTED;
    }

    public static boolean isRealTrace() {
        return status >= STATUS_READY;
    }

    public static void i(int methodId) {
        if (status <= STATUS_STOPPED) {
            return;
        }
        if (methodId >= METHOD_ID_MAX) {
            return;
        }
        //第一次执行该方法时 调用 realExecute 方法进行一些初始化工作，并将状态切换为 STATUS_READY
        if (status == STATUS_DEFAULT) {// 如果是默认状态，则说明是第一个方法
            synchronized (statusLock) {
                if (status == STATUS_DEFAULT) {
                    realExecute();
                    status = STATUS_READY;
                }
            }
        }

        long threadId = Thread.currentThread().getId();
        if (sMethodEnterListener != null) {
            sMethodEnterListener.enter(methodId, threadId);
        }

        if (threadId == sMainThreadId) {
            //i方法被重复执行的 提醒
            if (assertIn) {
                MatrixLog.e(TAG, "ERROR!!! AppMethodBeat.i Recursive calls!!!");
                return;
            }
            assertIn = true;
            if (sIndex < Constants.BUFFER_SIZE) {
                //保存调用时的methodId和时间到sBuffer中
                mergeData(methodId, sIndex, true);
            } else {
                sIndex = 0;
                mergeData(methodId, sIndex, true);
            }
            ++sIndex;
            assertIn = false;
        }
    }

    public static void o(int methodId) {
        if (status <= STATUS_STOPPED) {
            return;
        }
        if (methodId >= METHOD_ID_MAX) {
            return;
        }
        if (Thread.currentThread().getId() == sMainThreadId) {
            if (sIndex < Constants.BUFFER_SIZE) {
                mergeData(methodId, sIndex, false);
            } else {
                sIndex = 0;
                mergeData(methodId, sIndex, false);
            }
            ++sIndex;
        }
    }
    /**
     * when the special method calls,it's will be called.
     *
     * called after {@link #i(int)}
     * 感知到某个activity是否获取到焦点。这是因为at()方法会在编译器被插入到 Activity的onWindowFocusChanged()方法中
     * 将获得的焦点信息分发给每一个IAppMethodBeatListener
     *
     * @param activity now at which activity
     * @param isFocus  this window if has focus
     */
    public static void at(Activity activity, boolean isFocus) {
        String activityName = activity.getClass().getName();
        if (isFocus) {
            if (sFocusActivitySet.add(activityName)) {
                synchronized (listeners) {
                    for (IAppMethodBeatListener listener : listeners) {
                        listener.onActivityFocused(activity);
                    }
                }
                MatrixLog.i(TAG, "[at] visibleScene[%s] has %s focus!", getVisibleScene(), "attach");
            }
        } else {
            if (sFocusActivitySet.remove(activityName)) {
                MatrixLog.i(TAG, "[at] visibleScene[%s] has %s focus!", getVisibleScene(), "detach");
            }
        }
    }

    public static String getVisibleScene() {
        return AppActiveMatrixDelegate.INSTANCE.getVisibleScene();
    }


    /**
     * merge trace info as a long data
     * */
    private static void mergeData(int methodId, int index, boolean isIn) {
        if (methodId == AppMethodBeat.METHOD_ID_DISPATCH) {
            sCurrentDiffTime = SystemClock.uptimeMillis() - sDiffTime;
        }
        try {
            long trueId = 0L;
            if (isIn) {
                trueId |= 1L << 63;
            }
            trueId |= (long) methodId << 43;
            trueId |= sCurrentDiffTime & 0x7FFFFFFFFFFL;
            sBuffer[index] = trueId;
            checkPileup(index);
            sLastIndex = index;
        } catch (Throwable t) {
            MatrixLog.e(TAG, t.getMessage());
        }
    }

    private static void checkPileup(int index) {
        IndexRecord indexRecord = sIndexRecordHead;
        while(indexRecord != null) {
            if (indexRecord.index == index || (indexRecord.index == -1 && sLastIndex == Constants.BUFFER_SIZE -1)) {
                indexRecord.isValid = false;
                MatrixLog.w(TAG, "[checkPileup] %s", indexRecord.toString());
                sIndexRecordHead = indexRecord = indexRecord.next;
            } else {
                break;
            }
        }
    }

    /**
     * 这个方法主要是在 AppMethodBeat 类加载 15s 后，还没有使用的情况下释放各种资源，尤其是sBuffer因为它就占用了7.6M的内存。
     * */
    private static void realRelease() {
        synchronized (statusLock) {
            if (status == STATUS_DEFAULT || status <= STATUS_READY) {
                sHandler.removeCallbacksAndMessages(null);
                LooperMonitor.unregister(looperMonitorListener);
                sTimerUpdateThread.quit();
                sBuffer = null;
                status = STATUS_OUT_RELEASE;
            }
        }
    }

    //这个方法只有 在第一次执行 i方法时才会被调用
    private static void realExecute() {
        MatrixLog.i(TAG, "[realExecute] timestamp:%s", System.currentTimeMillis());
        //当前时间减去上一个 记录的时间 (更新 sCurrentDiffTime)
        sCurrentDiffTime = SystemClock.uptimeMillis() - sDiffTime;

        sHandler.removeCallbacksAndMessages(null);
        //延迟5 ms 后执行 sUpdateDiffTimeRunnable ,开始刷新  sCurrentDiffTime
        sHandler.postDelayed(sUpdateDiffTimeRunnable, Constants.TIME_UPDATE_CYCLE_MS);
        //延迟15 ms 后执行 checkStartExpiredRunnable (检查 AppMethodBeat 当前状态的 runnable )
        //也就是 在 realExecute 方法之后后 如果 15ms 内 AppMethodBeat 还没有被启动（onStart）
        // 就将 AppMethodBeat的状态置为 STATUS_EXPIRED_START（启动过期）
        // 启动过期 只是一种状态，并不会影响 AppMethodBeat 的运行
        sHandler.postDelayed(checkStartExpiredRunnable = new Runnable() {
            @Override
            public void run() {
                synchronized (statusLock) {
                    MatrixLog.i(TAG, "[startExpired] timestamp:%s status:%s", System.currentTimeMillis(), status);
                    if (status == STATUS_DEFAULT || status == STATUS_READY) {
                        status = STATUS_EXPIRED_START;
                    }
                }
            }
        }, Constants.DEFAULT_RELEASE_BUFFER_DELAY);

        //hook 主线程的 HandlerCallback
        ActivityThreadHacker.hackSysHandlerCallback();
        LooperMonitor.register(looperMonitorListener);
    }

    private static void dispatchBegin() {
        sCurrentDiffTime = SystemClock.uptimeMillis() - sDiffTime;
        isPauseUpdateTime = false;

        synchronized (updateTimeLock) {
            updateTimeLock.notify();
        }
    }

    private static void dispatchEnd() {
        isPauseUpdateTime = true;
    }


    /**
     * 创建一个链表的头，或者新建一个节点插入到链表的尾部
     * */
    public IndexRecord maskIndex(String source) {
        //创建一个链表的头，或者新建一个节点插入到链表的尾部
        if (sIndexRecordHead == null) {
            sIndexRecordHead = new IndexRecord(sIndex - 1);
            sIndexRecordHead.source = source;
            return sIndexRecordHead;
        } else {
            //创建一个 IndexRecord 并插入链表的尾部
            IndexRecord indexRecord = new IndexRecord(sIndex - 1);
            indexRecord.source = source;
            IndexRecord record = sIndexRecordHead;
            IndexRecord last = null;
            while(record != null) {
                if (indexRecord.index <= record.index) {
                    if (null == last) {
                        IndexRecord tmp = sIndexRecordHead;
                        sIndexRecordHead = indexRecord;
                        indexRecord.next = tmp;
                    } else {
                        IndexRecord tmp = last.next;
                        last.next = indexRecord;
                        indexRecord.next = tmp;
                    }
                    return indexRecord;
                }
                last = record;
                record = record.next;
            }
            last.next = indexRecord;
            return indexRecord;
        }
    }

    /**
     * 获取从 startRecord 到结束的 所有 IndexRecord
     * */
    public long[] copyData(IndexRecord startRecord) {
        return copyData(startRecord, new IndexRecord(sIndex -1));
    }

    private long[] copyData(IndexRecord startRecord, IndexRecord endRecord) {
        long current = System.currentTimeMillis();
        long[] data = new long[0];
        try {
            if (startRecord.isValid && endRecord.isValid) {
                int length;
                int start = Math.max(0, startRecord.index);
                int end = Math.max(0, endRecord.index);

                //计算出copy区域的长度和copy
                if (end > start) {//正常情况下 一次copy
                    length = end - start + 1;
                    data = new long[length];
                    System.arraycopy(sBuffer, start, data, 0, length);
                } else if (end < start) {// 两次copy(后半截+前半截)
                    length = 1 + end + (sBuffer.length - start);
                    data = new long[length];
                    System.arraycopy(sBuffer, start, data, 0, sBuffer.length - start);
                    System.arraycopy(sBuffer, 0, data, sBuffer.length - start, end + 1);
                }
                return data;
            }
            return data;
        } catch (Throwable t) {//这里还捕获 OutOfMemoryError ，大厂程序员真的是细啊
            MatrixLog.e(TAG, t.toString());
            return data;
        } finally {
            MatrixLog.i(TAG, "[copyData] [%s:%s] length:%s cost:%sms", Math.max(0, startRecord.index), endRecord.index, data.length, System.currentTimeMillis() - current);
        }
    }

    private static IndexRecord sIndexRecordHead = null;

    public static final class IndexRecord {
        public IndexRecord(int index) {
            this.index = index;
        }

        public IndexRecord() {
            this.isValid = false;
        }

        public int index;
        private IndexRecord next;
        public boolean isValid = true;
        public String source;

        public void release() {
            isValid = false;
            IndexRecord record = sIndexRecordHead;
            IndexRecord last = null;
            while (null != record) {
                if (record == this) {
                    if (null != last) {
                        last.next = record.next;
                    } else {
                        sIndexRecordHead = record.next;
                    }
                    record.next = null;
                    break;
                }
                last = record;
                record = record.next;
            }
        }

        @Override
        public String toString() {
            return "index:" + index + ",\tisValid:" + isValid + " source:" + source;
        }
    }

    public void printIndexRecord() {
        StringBuilder ss = new StringBuilder(" \n");
        IndexRecord record = sIndexRecordHead;
        while (null != record) {
            ss.append(record).append("\n");
            record = record.next;
        }
        MatrixLog.i(TAG, "[printIndexRecord] %s", ss.toString());
    }
}
