package com.beiying.apm.matrix.trace.core;

import androidx.annotation.CallSuper;

/**
 * LooperObserver 是一个抽象类，顾名思义，它是 Looper 的观察者，在 Looper 分发消息、刷新 UI 时回调，这几个回调方法也是 ANR、慢方法等模块的判断依据
 * */
public abstract class LooperObserver {
    private boolean isDispatchBegin = false;

    // 分发消息前
    /**
     * beginNs: 消息开始分发时候的时间点，单位为纳秒。通过 System.nanoTime() 获得。
     * cpuBeginNs:  消息开始时主线程的线程时间，单位：毫秒通过SystemClock.currentThreadTimeMillis()获得。
     * token: 跟 beginNs 一样。
     * */
    @CallSuper
    public void dispatchBegin(long beginNs, long cpuBeginNs, long token) {
        isDispatchBegin = true;
    }

    // UI 刷新
    /**
     * focusedActivity： 当前顶层的activity名字
     * startNs：消息开始分发的时间点 纳秒
     * endNs：消息分发结束的时间点 纳秒
     * isVsyncFrame： 是否是UI渲染帧。只要触发了input callback回调，那肯定是渲染消息。
     * intendedFrameTimeNs：当前时间，纳秒。如果反射获取失败，则把startNs作为默认值。 通过反射Choreographer内部类 FrameDisplayEventReceiver类中的变量：mTimestampNanos。
     * inputCostNs：input耗时。现在这三个比较简单了吧
     * animationCostNs：animation耗时。
     * traversalCostNs：traversal耗时。
     * */
    public void doFrame(String focusedActivity, long startNs, long endNs, boolean isVsyncFrame,
                        long intendedFrameTimeNs, long inputCostNs, long animationCostNs, long trasversalCostNs) {

    }

    // 分发消息后
    /**
     * beginNs:  消息开始分发时候的时间点，跟 dispatchBegin 中的 beginNs 是同一个值。
     * cpuBeginMs:  跟 dispatchBegin 中的 cpuBeginMs 是同一个值。
     * endNs:   消息结束分发时的时间点，单位：纳秒。
     * cpuEndMs:  消息结束分发时主线程时间点，单位：毫秒。
     * token:   跟 dispatchBegin 中的 token 是同一个值。
     * isVsyncFrame:  用来判断 looper 中消息是否是UI渲染帧。 默认为false。matrix 通过 UIThreadMonitor 类中
     *              addFrameCallback(CALLBACK_INPUT, this, true); 开始监听input事件，在runnable 开始执行时候，说明此时looper中的消息是处理ui 绘制的。
     * */
    @CallSuper
    public void dispatchEnd(long beginNs, long cpuBeginMs, long endNs, long cpuEndMs, long token, boolean isVsyncFrame) {
        isDispatchBegin = false;
    }

    public boolean isDispatchBegin() {
        return isDispatchBegin;
    }
}
