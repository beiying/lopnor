package com.beiying.apm.core.job.activity;

import android.app.Instrumentation;


import com.beiying.apm.utils.LogX;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.beiying.apm.Env.DEBUG;
import static com.beiying.apm.Env.TAG;

/**
 * Instrumentation Hook类
 *
 * @author ArgusAPM Team
 */
public class InstrumentationHooker {
    private static boolean isHookSucceed = false;//是否已经hook成功

    public static void doHook() {
        try {
            hookInstrumentation();
            isHookSucceed = true;
        } catch (Exception e) {
            if (DEBUG) {
                LogX.e(TAG, "InstrumentationHooker", e.toString());
            }
        }
    }

    static boolean isHookSucceed() {
        return isHookSucceed;
    }

    /**
     * 通过反射的方式获取当前ActivityThread的mInstrumentation属性的实例，返回给currentInstrumentation，
     * 并且用currentInstrumentation初始化一个ApmInstrumentation的实例ins，最后用ins的替换到当前ActivityThread中mInstrumentation的值，实现hook。
     * Instrumentation方式是一种在运行期进行hook的方式。
     * Activity的另一种采集方式是AOP，在编译过程织入代码进行hook。
     * */
    private static void hookInstrumentation() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        Class<?> c = Class.forName("android.app.ActivityThread");
        Method currentActivityThread = c.getDeclaredMethod("currentActivityThread");
        boolean acc = currentActivityThread.isAccessible();
        if (!acc) {
            currentActivityThread.setAccessible(true);
        }
        Object o = currentActivityThread.invoke(null);
        if (!acc) {
            currentActivityThread.setAccessible(acc);
        }
        Field f = c.getDeclaredField("mInstrumentation");
        acc = f.isAccessible();
        if (!acc) {
            f.setAccessible(true);
        }
        Instrumentation currentInstrumentation = (Instrumentation) f.get(o);
        Instrumentation ins = new ApmInstrumentation(currentInstrumentation);
        f.set(o, ins);
        if (!acc) {
            f.setAccessible(acc);
        }
    }
}