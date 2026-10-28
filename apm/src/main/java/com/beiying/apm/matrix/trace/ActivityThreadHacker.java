package com.beiying.apm.matrix.trace;

import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.beiying.apm.matrix.MatrixLog;
import com.beiying.apm.matrix.trace.core.AppMethodBeat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 拦截应用启动过程中Application、Activity相关的重要启动时间点
 * */
public class ActivityThreadHacker {
    private static final String TAG = "Matrix.ActivityThreadHacker";
    //这个时间被认为是APP启动时间
    private static long sApplicationCreateBeginTime = 0L;
    private static long sApplicationCreateEndTime = 0L;
    public static AppMethodBeat.IndexRecord sLastLaunchActivityMethodIndex = new AppMethodBeat.IndexRecord();
    public static AppMethodBeat.IndexRecord sApplicationCreateBeginMethodIndex = new AppMethodBeat.IndexRecord();
    public static int sApplicationCreateScene = Integer.MIN_VALUE;
    private static final HashSet<IApplicationCreateListener> listeners = new HashSet<>();
    private static boolean sIsCreatedByLaunchActivity = false;

    public static void addListener(IApplicationCreateListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public static void removeListener(IApplicationCreateListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public static long getApplicationCost() {
        return ActivityThreadHacker.sApplicationCreateEndTime
                - ActivityThreadHacker.sApplicationCreateBeginTime;
    }

    public static long getEggBrokenTime() {
        return ActivityThreadHacker.sApplicationCreateBeginTime;
    }

    public static boolean isCreatedByLaunchActivity() {
        return sIsCreatedByLaunchActivity;
    }


    //通过反射的方式，接管 ActivityThread 的 Handler 的 Callback
    public static void hackSysHandlerCallback() {
        try {
            //当前方法加载的时间 被认为是 APP启动时间，因为在插桩的过程中忽略了 android包下的所有类，
            // 所以运行期第一个执行 AppMethodBeat.i方法的应该是 Application的attachBaseContext方法（如果有），
            // 下来就是onCreate()方法。所以这个时间被认为是APP启动时间是没有毛病的。
            sApplicationCreateBeginTime = SystemClock.uptimeMillis();
            //记录这第一个方法
            sApplicationCreateBeginMethodIndex = AppMethodBeat.getInstance().maskIndex("ApplicationCreateBeginMethodIndex");

            //替换 ActivityThread 中handler的 callBack 方法
            Class<?> forName = Class.forName("android.app.ActivityThread");
            Field field = forName.getDeclaredField("sCurrentActivityThread");
            field.setAccessible(true);
            Object activityThreadValue = field.get(forName);
            Field mH = forName.getDeclaredField("mH");
            mH.setAccessible(true);
            Object handler = mH.get(activityThreadValue);
            Class<?> handlerClass = handler.getClass().getSuperclass();
            if (null != handlerClass) {
                Field callbackField = handlerClass.getDeclaredField("mCallback");
                callbackField.setAccessible(true);
                Handler.Callback originalCallback = (Handler.Callback) callbackField.get(handler);
                HackCallback callback = new HackCallback(originalCallback);
                callbackField.set(handler, callback);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface IApplicationCreateListener {
        void onApplicationCreateEnd();
    }

    //自定义的HackCallback对象主要用来记录最近一个Activity被打开的时间
    private final static class HackCallback implements Handler.Callback {
        private static final int LAUNCH_ACTIVITY = 100;
        private static final int CREATE_SERVICE = 114;
        private static final int RELAUNCH_ACTIVITY = 126;
        private static final int RECEIVER = 113;
        private static final int EXECUTE_TRANSACTION = 159; // for Android 9.0
        private static boolean isCreated = false;
        private static int hasPrint = Integer.MAX_VALUE;

        private final Handler.Callback mOriginalCallback;

        private static final int SERIVCE_ARGS = 115;
        private static final int STOP_SERVICE = 116;
        private static final int STOP_ACTIVITY_SHOW = 103;
        private static final int STOP_ACTIVITY_HIDE = 104;
        private static final int SLEEPING = 137;

        HackCallback(Handler.Callback callback) {
            this.mOriginalCallback = callback;
        }

        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (IssueFixConfig.getInstance().isEnableFixSpApply()) {
                if (Build.VERSION.SDK_INT >= 21 && Build.VERSION.SDK_INT <= 25) {
                    if (msg.what == SERIVCE_ARGS || msg.what == STOP_SERVICE
                            || msg.what == STOP_ACTIVITY_SHOW || msg.what == STOP_ACTIVITY_HIDE
                            || msg.what == SLEEPING) {
                        MatrixLog.i(TAG, "Fix SP ANR is enabled");
                        fix();
                    }
                }
            }
            if (!AppMethodBeat.isRealTrace()) {
                //将 handleMessage 的控制权交还给原始的 mOriginalCallback,再一次感叹真细啊，哈哈
                return null != mOriginalCallback && mOriginalCallback.handleMessage(msg);
            }
            //是否是打开activity的handler信息
            boolean isLaunchActivity = isLaunchActivity(msg);
            if (hasPrint > 0) {
                MatrixLog.i(TAG, "[handleMessage] msg.what:%s begin:%s isLaunchActivity:%s SDK_INT=%s", msg.what, SystemClock.uptimeMillis(), isLaunchActivity, Build.VERSION.SDK_INT);
                hasPrint--;
            }
            if (!isCreated) {
                if (isLaunchActivity || msg.what == CREATE_SERVICE
                        || msg.what == RECEIVER) {
                    //发送启动Activity等消息，认为是Application 启动的结束时间
                    ActivityThreadHacker.sApplicationCreateEndTime = SystemClock.uptimeMillis();
                    ActivityThreadHacker.sApplicationCreateScene = msg.what;
                    isCreated = true;
                    sIsCreatedByLaunchActivity = isLaunchActivity;
                    MatrixLog.i(TAG, "application create end, sApplicationCreateScene:%d, isLaunchActivity:%s", msg.what, isLaunchActivity);
                    synchronized (listeners) {
                        for (IApplicationCreateListener listener : listeners) {
                            listener.onApplicationCreateEnd();
                        }
                    }
                }
            }
            return null != mOriginalCallback && mOriginalCallback.handleMessage(msg);
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        private void fix() {
            try {
                Class cls = Class.forName("android.app.QueuedWork");
                Field field = cls.getDeclaredField("sPendingWorkFinishers");
                if (field != null) {
                    field.setAccessible(true);
                    ConcurrentLinkedDeque<Runnable> runnables = (ConcurrentLinkedDeque<Runnable>) field.get(null);
                    runnables.clear();
                    MatrixLog.i(TAG, "Fix SP ANR sPendingWorkFinishers.clear successful");
                }
            } catch (ClassNotFoundException e) {
                MatrixLog.e(TAG,
                        "Fix SP ANR ClassNotFoundException = " + e.getMessage());
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                MatrixLog.e(TAG, "Fix SP ANR IllegalAccessException =" + e.getMessage());
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                MatrixLog.e(TAG, "Fix SP ANR NoSuchFieldException = " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                MatrixLog.e(TAG, "Fix SP ANR Exception = " + e.getMessage());
                e.printStackTrace();
            }
        }

        private Method method = null;
        private boolean isLaunchActivity(Message msg) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
                if (msg.what == EXECUTE_TRANSACTION && msg.obj != null) {
                    try {
                        if (null == method) {
                            Class clazz = Class.forName("android.app.servertransaction.ClientTransaction");
                            method = clazz.getDeclaredMethod("getCallbacks");
                            method.setAccessible(true);
                        }
                        List list = (List) method.invoke(msg.obj);
                        if (!list.isEmpty()) {
                            return list.get(0).getClass().getName().endsWith(".LaunchActivityItem");
                        }
                    } catch (Exception e) {
                        MatrixLog.e(TAG, "[isLaunchActivity] %s", e);
                    }
                }
                return msg.what == LAUNCH_ACTIVITY;
            } else {
                return msg.what == LAUNCH_ACTIVITY || msg.what == RELAUNCH_ACTIVITY;
            }
        }
    }
}
