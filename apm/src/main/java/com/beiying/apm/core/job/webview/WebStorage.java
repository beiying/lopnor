package com.beiying.apm.core.job.webview;

import android.database.Cursor;


import com.beiying.apm.api.ApmTask;
import com.beiying.apm.core.IInfo;
import com.beiying.apm.core.Manager;
import com.beiying.apm.core.storage.TableStorage;
import com.beiying.apm.utils.IOStreamUtils;
import com.beiying.apm.utils.LogX;

import java.util.LinkedList;
import java.util.List;

import static com.beiying.apm.Env.TAG;


/**
 * @author ArgusAPM Team
 */
public class WebStorage extends TableStorage {
    @Override
    public List<IInfo> readDb(String selection) {
        List<IInfo> infoList = new LinkedList<IInfo>();
        Cursor cursor = null;
        try {
            cursor = Manager.getInstance().getConfig().appContext.getContentResolver()
                    .query(getTableUri(), null, selection, null, null);
            if (null == cursor || !cursor.moveToNext()) {
                IOStreamUtils.closeQuietly(cursor);
                return infoList;
            }
            int indexId = cursor.getColumnIndex(WebInfo.KEY_ID_RECORD);
            int indexTimeRecord = cursor.getColumnIndex(WebInfo.KEY_TIME_RECORD);
            int indexUrl = cursor.getColumnIndex(WebInfo.DBKey.KEY_URL);
            int indexIsWifi = cursor.getColumnIndex(WebInfo.DBKey.KEY_IS_WIFI);
            int indexNavigationStart = cursor.getColumnIndex(WebInfo.DBKey.KEY_NAVIGATION_START);
            int indexResponseStart = cursor.getColumnIndex(WebInfo.DBKey.KEY_RESPONSE_START);
            int indexPageTime = cursor.getColumnIndex(WebInfo.DBKey.KEY_PAGE_TIME);
            int indexParams = cursor.getColumnIndex(WebInfo.KEY_PARAM);
            do {
                WebInfo webInfo = new WebInfo();
                webInfo.setId(cursor.getInt(indexId));
                webInfo.url = cursor.getString(indexUrl);
                webInfo.isWifi = cursor.getInt(indexIsWifi) == 1;
                webInfo.navigationStart = cursor.getLong(indexNavigationStart);
                webInfo.responseStart = cursor.getLong(indexResponseStart);
                webInfo.pageTime = cursor.getLong(indexPageTime);
                webInfo.setParams(cursor.getString(indexParams));
                webInfo.setRecordTime(cursor.getLong(indexTimeRecord));
                infoList.add(webInfo);
            } while (cursor.moveToNext());

        } catch (Exception e) {
            LogX.e(TAG, SUB_TAG, getName() + "; " + e.toString());
        } finally {
            IOStreamUtils.closeQuietly(cursor);
        }
        return infoList;
    }

    @Override
    public String getName() {
        return ApmTask.TASK_WEBVIEW;
    }
}
