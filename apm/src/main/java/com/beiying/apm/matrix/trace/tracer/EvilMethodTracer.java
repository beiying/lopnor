package com.beiying.apm.matrix.trace.tracer;

import com.beiying.apm.matrix.trace.TraceConfig;
import com.beiying.apm.matrix.trace.tracer.Tracer;

/**
 * 原理：
 *  在calss字节码转dex文件阶段，通过自定义transform插件，利用ASM工具来操作修改.class文件。在每个方法执行前后插入AppMethodBeat.i
 *  和AppMethodBeat.o方法，在运行期计算两个的差值就得到方法的耗时。
 *
 *  优点：
 *      兼容性好 无需通过hook手段
 *      优化插桩性能 在方法收集阶段会主动过滤简单的set/get方法，以及一些匿名的构造函数
 *
 *  缺点：
 *      无法统计到系统内执行的函数，但一般卡顿都是由我们自己的App代码导致，所以影响不大
 * */
public class EvilMethodTracer extends Tracer {
    private static final String TAG = "Matrix.EvilMethodTracer";
    private final TraceConfig config;
    private long evilThresholdMs;
    private boolean isEvilMethodTraceEnable;

    public EvilMethodTracer(TraceConfig config) {
        this.config = config;
        this.evilThresholdMs = config.getEvilThresholdMs();
        this.isEvilMethodTraceEnable = config.isEvilMethodTraceEnable();
    }

    @Override
    protected void onAlive() {
        super.onAlive();
    }
}
