package com.beiying.apm.core.job.activity;

import android.app.Activity;
import android.content.Context;

import com.beiying.apm.aop.IActivityHelper;


/**
 * 用于AOP
 *
 * @author ArgusAPM Team
 */
public class ActivityHelper implements IActivityHelper {
    @Override
    public void applicationAttachBaseContext(Context context) {
        AH.applicationAttachBaseContext(context);
    }


    @Override
    public void invoke(Activity activity, long startTime, String lifeCycle, Object... args) {
        AH.invoke(activity, startTime, lifeCycle, args);
    }

    @Override
    public void applicationOnCreate(Context context) {
        AH.applicationOnCreate(context);
    }

    @Override
    public Object parse(Object... args) {
        return null;
    }
}
