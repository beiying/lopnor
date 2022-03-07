package com.beiying.apm.matrix.trace.tracer;

import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;

import com.beiying.apm.matrix.AppActiveMatrixDelegate;
import com.beiying.apm.matrix.Issue;
import com.beiying.apm.matrix.Matrix;
import com.beiying.apm.matrix.MatrixHandlerThread;
import com.beiying.apm.matrix.MatrixLog;
import com.beiying.apm.matrix.trace.Constants;
import com.beiying.apm.matrix.trace.MethodItem;
import com.beiying.apm.matrix.trace.SharePluginInfo;
import com.beiying.apm.matrix.trace.TraceConfig;
import com.beiying.apm.matrix.trace.TracePlugin;
import com.beiying.apm.matrix.trace.core.AppMethodBeat;
import com.beiying.apm.matrix.trace.core.UIThreadMonitor;
import com.beiying.apm.matrix.util.DeviceUtil;
import com.beiying.apm.matrix.util.TraceUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * 优点：对比watchDog方式，无侵入式，兼容性好。
 *      WatchDog简要原理： 通过一个子线程往主线程加入消息，以一定的间隔去不断轮询该消息是否被处理，
 *      如果没有被处理，则表示出现了卡顿。缺点是轮询的时间间隔不好把控。间隔太小则会会主线程有影响存在性能问题，太大则容易漏报。
 * 缺点：
 * 1、无法监控touch事件？
 * 2、无法监控idleHandler的queueIdle()？
 *      是的，因为idelHandler的处理不会回调logger的print方法。且每次触发消息，只会运行一次idleHandler，后续如果没有消息则会阻塞住，直到新消息的到来。
 * 3、无法监控SyncBarrier（同步屏障）?
 *      当我们每次通过invalidate来刷新UI时，最终都会调用到ViewRootImpl中的scheduleTraversals方法，会向主线程的Looper中post一个SyncBarrier，
 *      其目的是为了在刷新UI时，主线程的同步消息都被跳过，此时渲染UI的异步消息就可以得到优先处理。但是我们注意到这个方法是线程不安全的，
 *      如果在非主线程中调用到了这里，就有可能会同时post多个SyncBarrier，但只能remove掉最后一个，从而有一个SyncBarrier就永远无法被remove，
 *      就导致了主线程Looper无法处理同步消息（Message默认就是同步消息），导致卡死，那如何处理？
 * */
public class LooperAnrTracer extends Tracer {
    private static final String TAG = "Matrix.AnrTracer";
    private Handler anrHandler;
    private Handler lagHandler;
    private final TraceConfig traceConfig;
    private final AnrHandleTask anrTask = new AnrHandleTask();
    private final LagHandleTask lagTask = new LagHandleTask();
    private boolean isAnrTraceEnable;

    public LooperAnrTracer(TraceConfig traceConfig) {
        this.traceConfig = traceConfig;
        this.isAnrTraceEnable = traceConfig.isAnrTraceEnable();
    }

    @Override
    protected void onAlive() {
        super.onAlive();
        if (isAnrTraceEnable) {
            UIThreadMonitor.getMonitor().addObserver(this);
            this.anrHandler = new Handler(MatrixHandlerThread.getDefaultHandler().getLooper());
            this.lagHandler = new Handler(MatrixHandlerThread.getDefaultHandler().getLooper());
        }
    }

    @Override
    protected void onDead() {
        super.onDead();
        if (isAnrTraceEnable) {
            UIThreadMonitor.getMonitor().removeObserver(this);
            anrTask.getBeginRecord().release();
            anrHandler.removeCallbacksAndMessages(null);
            lagHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void dispatchBegin(long beginNs, long cpuBeginNs, long token) {
        super.dispatchBegin(beginNs, cpuBeginNs, token);
        //在anrtask中标记开始标志start，用来后面发生anr时候的用来截取sBuffer数组存储的执行过的方法。
        anrTask.beginRecord = AppMethodBeat.getInstance().maskIndex("AnrTracer#dispatchBegin");
        anrTask.token = token;

        if (traceConfig.isDevEnv()) {
            MatrixLog.v(TAG, "* [dispatchBegin] token:%s index:%s", token, anrTask.beginRecord.index);
        }

        long cost = (System.nanoTime() - token) / Constants.TIME_MILLIS_TO_NANO;
        //将anrTask加入到anrHandler的延时队列中，如果超过5s anrTask还没有被移除就会被执行
        // 有个小小的疑问？ 当looper被主线程阻塞的时候，这个怎么检测？
        // 答案：阻塞是正常的现象。当被MessageQueue的next方法阻塞时，也不会回调dispatchBegin，从而不会触发获取堆栈逻辑。
        anrHandler.postDelayed(anrTask, Constants.DEFAULT_ANR - cost);
        lagHandler.postDelayed(lagTask, Constants.DEFAULT_NORMAL_LAG - cost);
    }

    @Override
    public void dispatchEnd(long beginNs, long cpuBeginMs, long endNs, long cpuEndMs, long token, boolean isVsyncFrame) {
        super.dispatchEnd(beginNs, cpuBeginMs, endNs, cpuEndMs, token, isVsyncFrame);
        if (traceConfig.isDevEnv()) {
            long cost = (endNs - beginNs) / Constants.TIME_MILLIS_TO_NANO;
            MatrixLog.v(TAG, "[dispatchEnd] token:%s cost:%sms cpu:%sms usage:%s",
                    token, cost,
                    cpuEndMs - cpuBeginMs, TraceUtils.calculateCpuUsage(cpuEndMs - cpuBeginMs, cost));
        }
        anrTask.getBeginRecord().release();
        anrHandler.removeCallbacks(anrTask);
        lagHandler.removeCallbacks(lagTask);
    }

    private String printInputExpired(long inputCost) {
        StringBuilder print = new StringBuilder();
        String scene = AppActiveMatrixDelegate.INSTANCE.getVisibleScene();
        boolean isForeground = isForeground();
        // memory
        long[] memoryInfo = dumpMemory();
        // process
        int[] processStat = TraceUtils.getProcessPriority(Process.myPid());
        print.append(String.format("-\n>>>>>>>>>>>>>>>>>>>>>>> maybe happens Input ANR(%s ms)! <<<<<<<<<<<<<<<<<<<<<<<\n", inputCost));
        print.append("|* [Status]").append("\n");
        print.append("|*\t\tScene: ").append(scene).append("\n");
        print.append("|*\t\tForeground: ").append(isForeground).append("\n");
        print.append("|*\t\tPriority: ").append(processStat[0]).append("\tNice: ").append(processStat[1]).append("\n");
        print.append("|*\t\tis64BitRuntime: ").append(DeviceUtil.is64BitRuntime()).append("\n");
        print.append("|* [Memory]").append("\n");
        print.append("|*\t\tDalvikHeap: ").append(memoryInfo[0]).append("kb\n");
        print.append("|*\t\tNativeHeap: ").append(memoryInfo[1]).append("kb\n");
        print.append("|*\t\tVmSize: ").append(memoryInfo[2]).append("kb\n");
        print.append("=========================================================================");
        return print.toString();
    }

    private long[] dumpMemory() {
        long[] memory = new long[3];
        memory[0] = DeviceUtil.getDalvikHeap();
        memory[1] = DeviceUtil.getNativeHeap();
        memory[2] = DeviceUtil.getVmSize();
        return memory;
    }

    class AnrHandleTask implements Runnable {
        AppMethodBeat.IndexRecord beginRecord;
        long token;

        AnrHandleTask() {}

        AnrHandleTask(AppMethodBeat.IndexRecord record, long token) {
            this.beginRecord = record;
            this.token = token;
        }

        public AppMethodBeat.IndexRecord getBeginRecord() {
            return beginRecord;
        }

        @Override
        public void run() {
            long curTime = SystemClock.uptimeMillis();
            boolean isForeground = isForeground();
            // process
            int[] processStat = TraceUtils.getProcessPriority(Process.myPid());

            //获取需要分析的方法栈信息
            long[] data = AppMethodBeat.getInstance().copyData(beginRecord);
            beginRecord.release();
            String scene = AppActiveMatrixDelegate.INSTANCE.getVisibleScene();

            // memory
            // runtime 占用内存
            // native 堆内存，通过debug获取
            // 虚拟机内存，vmsize
            long[] memoryInfo = dumpMemory();
            // 线程状态
            Thread.State status = Looper.getMainLooper().getThread().getState();
            //堆栈信息
            StackTraceElement[] stackTrace = Looper.getMainLooper().getThread().getStackTrace();
            String dumpStack;
            if (traceConfig.getLooperPrinterStackStyle() == TraceConfig.STACK_STYLE_WHOLE) {
                dumpStack = TraceUtils.getWholeStack(stackTrace, "|*\t\t");
            } else {
                dumpStack = TraceUtils.getStack(stackTrace, "|*\t\t", 12);
            }

            // frame
            // 通过token（dispatchStart时间）获取不同Type 的耗费时间
            UIThreadMonitor monitor = UIThreadMonitor.getMonitor();
            long inputCost = monitor.getQueueCost(UIThreadMonitor.CALLBACK_INPUT, token);
            long animationCost = monitor.getQueueCost(UIThreadMonitor.CALLBACK_ANIMATION, token);
            long traversalCost = monitor.getQueueCost(UIThreadMonitor.CALLBACK_TRAVERSAL, token);

            //Trace
            LinkedList<MethodItem> stack = new LinkedList<>();
            if (data.length > 0) {
                // 根据之前 data 查到的 methodId ，拿到对应插桩函数的执行时间、执行深度，
                // 将每个函数的信息封装成 MethodItem，然后存储到 stack 链表当中
                TraceUtils.structuredDataToStack(data, stack, true, curTime);
                //根据规则 裁剪 stack 中的数据，
                TraceUtils.trimStack(stack, Constants.TARGET_EVIL_METHOD_STACK, new TraceUtils.IStructuredDataFilter() {
                    @Override
                    public boolean isFilter(long during, int filterCount) {
                        return during < filterCount * Constants.TIME_UPDATE_CYCLE_MS;
                    }

                    @Override
                    public int getFilterMaxCount() {
                        return Constants.FILTER_STACK_MAX_COUNT;
                    }

                    @Override
                    public void fallback(List<MethodItem> stack, int size) {
                        MatrixLog.w(TAG, "[fallback] size:%s targetSize:%s stack:%s", size, Constants.TARGET_EVIL_METHOD_STACK, stack);
                        Iterator iterator = stack.listIterator(Math.min(size, Constants.TARGET_EVIL_METHOD_STACK));
                        while (iterator.hasNext()) {
                            iterator.next();
                            iterator.remove();
                        }
                    }
                });
            }

            StringBuilder reportBuilder = new StringBuilder();
            StringBuilder logcatBuilder = new StringBuilder();
            //获取最大的耗时时间
            long stackCost = Math.max(Constants.DEFAULT_ANR, TraceUtils.stackToString(stack, reportBuilder, logcatBuilder));

            // stackKey
            // 查询出最耗时的 方法id
            String stackKey = TraceUtils.getTreeKey(stack, stackCost);
            MatrixLog.w(TAG, "%s \npostTime:%s curTime:%s",
                    printAnr(scene, processStat, memoryInfo, status, logcatBuilder, isForeground, stack.size(),
                            stackKey, dumpStack, inputCost, animationCost, traversalCost, stackCost),
                    token / Constants.TIME_MILLIS_TO_NANO, curTime); // for logcat
            //异常情况判断（当 AnrHandleTask 没有及时执行时会发生）
            if (stackCost >= Constants.DEFAULT_ANR_INVALID) {
                MatrixLog.w(TAG, "The checked anr task was not executed on time. "
                        + "The possible reason is that the current process has a low priority. just pass this report");
                return;
            }
            // report
            try {
                TracePlugin plugin = Matrix.with().getPluginByClass(TracePlugin.class);
                if (null == plugin) {
                    return;
                }
                JSONObject jsonObject = new JSONObject();
                jsonObject = DeviceUtil.getDeviceInfo(jsonObject, Matrix.with().getApplication());
                jsonObject.put(SharePluginInfo.ISSUE_STACK_TYPE, Constants.Type.ANR);
                jsonObject.put(SharePluginInfo.ISSUE_COST, stackCost);
                jsonObject.put(SharePluginInfo.ISSUE_STACK_KEY, stackKey);
                jsonObject.put(SharePluginInfo.ISSUE_SCENE, scene);
                jsonObject.put(SharePluginInfo.ISSUE_TRACE_STACK, reportBuilder.toString());
                if (traceConfig.getLooperPrinterStackStyle() == TraceConfig.STACK_STYLE_WHOLE) {
                    jsonObject.put(SharePluginInfo.ISSUE_THREAD_STACK, TraceUtils.getWholeStack(stackTrace));
                } else {
                    jsonObject.put(SharePluginInfo.ISSUE_THREAD_STACK, TraceUtils.getStack(stackTrace));
                }
                jsonObject.put(SharePluginInfo.ISSUE_PROCESS_PRIORITY, processStat[0]);
                jsonObject.put(SharePluginInfo.ISSUE_PROCESS_NICE, processStat[1]);
                jsonObject.put(SharePluginInfo.ISSUE_PROCESS_FOREGROUND, isForeground);
                // memory info
                JSONObject memJsonObject = new JSONObject();
                memJsonObject.put(SharePluginInfo.ISSUE_MEMORY_DALVIK, memoryInfo[0]);
                memJsonObject.put(SharePluginInfo.ISSUE_MEMORY_NATIVE, memoryInfo[1]);
                memJsonObject.put(SharePluginInfo.ISSUE_MEMORY_VM_SIZE, memoryInfo[2]);
                jsonObject.put(SharePluginInfo.ISSUE_MEMORY, memJsonObject);

                Issue issue = new Issue();
                issue.setKey(token + "");
                issue.setTag(SharePluginInfo.TAG_PLUGIN_EVIL_METHOD);
                issue.setContent(jsonObject);
                plugin.onDetectIssue(issue);

            } catch (JSONException e) {
                MatrixLog.e(TAG, "[JSONException error: %s", e);
            }
        }

        private String printAnr(String scene, int[] processStat, long[] memoryInfo, Thread.State state, StringBuilder stack, boolean isForeground,
                                long stackSize, String stackKey, String dumpStack, long inputCost, long animationCost, long traversalCost, long stackCost) {
            StringBuilder print = new StringBuilder();
            print.append(String.format("-\n>>>>>>>>>>>>>>>>>>>>>>> maybe happens ANR(%s ms)! <<<<<<<<<<<<<<<<<<<<<<<\n", stackCost));
            print.append("|* [Status]").append("\n");
            print.append("|*\t\tScene: ").append(scene).append("\n");
            print.append("|*\t\tForeground: ").append(isForeground).append("\n");
            print.append("|*\t\tPriority: ").append(processStat[0]).append("\tNice: ").append(processStat[1]).append("\n");
            print.append("|*\t\tis64BitRuntime: ").append(DeviceUtil.is64BitRuntime()).append("\n");

            print.append("|* [Memory]").append("\n");
            print.append("|*\t\tDalvikHeap: ").append(memoryInfo[0]).append("kb\n");
            print.append("|*\t\tNativeHeap: ").append(memoryInfo[1]).append("kb\n");
            print.append("|*\t\tVmSize: ").append(memoryInfo[2]).append("kb\n");
            print.append("|* [doFrame]").append("\n");
            print.append("|*\t\tinputCost:animationCost:traversalCost").append("\n");
            print.append("|*\t\t").append(inputCost).append(":").append(animationCost).append(":").append(traversalCost).append("\n");
            print.append("|* [Thread]").append("\n");
            print.append(String.format("|*\t\tStack(%s): ", state)).append(dumpStack);
            print.append("|* [Trace]").append("\n");
            if (stackSize > 0) {
                print.append("|*\t\tStackKey: ").append(stackKey).append("\n");
                print.append(stack.toString());
            } else {
                print.append(String.format("AppMethodBeat is close[%s].", AppMethodBeat.getInstance().isAlive())).append("\n");
            }
            print.append("=========================================================================");
            return print.toString();
        }

    }

    class LagHandleTask implements Runnable {
        @Override
        public void run() {
            String scene = AppActiveMatrixDelegate.INSTANCE.getVisibleScene();
            boolean isForeground = isForeground();
            try {
                TracePlugin plugin = Matrix.with().getPluginByClass(TracePlugin.class);
                if (null == plugin) {
                    return;
                }
                StackTraceElement[] stackTrace = Looper.getMainLooper().getThread().getStackTrace();
                String dumpStack = TraceUtils.getWholeStack(stackTrace);

                JSONObject jsonObject = new JSONObject();
                jsonObject = DeviceUtil.getDeviceInfo(jsonObject, Matrix.with().getApplication());
                jsonObject.put(SharePluginInfo.ISSUE_STACK_TYPE, Constants.Type.LAG);
                jsonObject.put(SharePluginInfo.ISSUE_SCENE, scene);
                jsonObject.put(SharePluginInfo.ISSUE_THREAD_STACK, dumpStack);
                jsonObject.put(SharePluginInfo.ISSUE_PROCESS_FOREGROUND, isForeground);

                Issue issue = new Issue();
                issue.setTag(SharePluginInfo.TAG_PLUGIN_EVIL_METHOD);
                issue.setContent(jsonObject);
                plugin.onDetectIssue(issue);
                MatrixLog.e(TAG, "happens lag : %s, scene : %s ", dumpStack, scene);
            } catch (JSONException e) {
                e.printStackTrace();
                MatrixLog.e(TAG, "[JSONException error: %s", e);
            }
        }
    }
}
