package com.beiying.apm.core.job.func;


import com.beiying.apm.aop.IAopTraceHelper;

/**
 * @author ArgusAPM Team
 */
public class AopFuncTrace implements IAopTraceHelper {
    @Override
    public void dispatch(long startTime, String kind, String sign, Object[] args, Object target, Object thiz, String location, String staticPartStr, String methodName, Object result) {
        FuncTrace.dispatch(startTime, kind, sign, args, target, thiz, location, staticPartStr, methodName, result);
    }
}
