package com.beiying.apm.core.job.activity;

import android.database.Cursor;

import com.beiying.apm.api.ApmTask;
import com.beiying.apm.core.BaseInfo;
import com.beiying.apm.core.IInfo;
import com.beiying.apm.core.Manager;
import com.beiying.apm.core.storage.TableStorage;
import com.beiying.apm.utils.IOStreamUtils;
import com.beiying.apm.utils.LogX;

import java.util.LinkedList;
import java.util.List;

import static com.beiying.apm.Env.TAG;


/**
 * Activity存储类
 *
 * @author ArgusAPM Team
 */
public class ActivityStorage extends TableStorage {
    private final String SUB_TAG = "ActivityStorage";

    @Override
    public String getName() {
        return ApmTask.TASK_ACTIVITY;
    }

    @Override
    public List<IInfo> readDb(String selection) {
        List<IInfo> infoList = new LinkedList<IInfo>();
        Cursor cursor = null;
        try {
            cursor = Manager.getInstance().getConfig().appContext.getContentResolver()
                    .query(getTableUri(), null, selection, null, null);
            if (null == cursor || !cursor.moveToFirst()) {
                IOStreamUtils.closeQuietly(cursor);
                return infoList;
            }
            int idIndex = cursor.getColumnIndex(ActivityInfo.KEY_ID_RECORD);
            int indexTimeRecord = cursor.getColumnIndex(ActivityInfo.KEY_TIME_RECORD);
            int indexName = cursor.getColumnIndex(ActivityInfo.KEY_NAME);
            int indexStartType = cursor.getColumnIndex(ActivityInfo.KEY_START_TYPE);
            int indexTime = cursor.getColumnIndex(ActivityInfo.KEY_TIME);
            int indexLifeCycle = cursor.getColumnIndex(ActivityInfo.KEY_LIFE_CYCLE);
            int indexAppName = cursor.getColumnIndex(BaseInfo.KEY_APP_NAME);
            int indexAppVer = cursor.getColumnIndex(BaseInfo.KEY_APP_VER);
            do {
                ActivityInfo activityInfo = new ActivityInfo();
                activityInfo.setId(cursor.getInt(idIndex));
                activityInfo.activityName = cursor.getString(indexName);
                activityInfo.setRecordTime(cursor.getLong(indexTimeRecord));
                activityInfo.startType = cursor.getInt(indexStartType);
                activityInfo.time = cursor.getLong(indexTime);
                activityInfo.lifeCycle = cursor.getInt(indexLifeCycle);
                activityInfo.pluginName = cursor.getString(indexAppName);
                activityInfo.pluginVer = cursor.getString(indexAppVer);
                infoList.add(activityInfo);
            } while (cursor.moveToNext());
        } catch (Exception e) {
            LogX.e(TAG, SUB_TAG, e.toString());
        } finally {
            IOStreamUtils.closeQuietly(cursor);
        }
        return infoList;
    }
}