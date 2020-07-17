package com.beiying.apm.core.job.net.okhttp3;


import com.beiying.apm.api.ApmTask;
import com.beiying.apm.api.Client;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import okhttp3.OkHttpClient;

/**
 * OKHTTP3切面文件
 *
 * @author ArgusAPM Team
 */
@Aspect
public class OkHttp3Aspect {

    @Pointcut("call(public okhttp3.OkHttpClient build())")
    public void build() {

    }

    @Around("build()")
    public Object aroundBuild(ProceedingJoinPoint joinPoint) throws Throwable {
        Object target = joinPoint.getTarget();

        if (target instanceof OkHttpClient.Builder && Client.isTaskRunning(ApmTask.TASK_NET)) {
            OkHttpClient.Builder builder = (OkHttpClient.Builder) target;
            builder.addInterceptor(new NetWorkInterceptor());
        }

        return joinPoint.proceed();
    }
}