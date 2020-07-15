package com.beiying.apm.core.job.appstart;

import android.database.Cursor;

import com.beiying.apm.api.ApmTask;
import com.beiying.apm.core.IInfo;
import com.beiying.apm.core.Manager;
import com.beiying.apm.core.storage.TableStorage;
import com.beiying.apm.utils.IOStreamUtils;
import com.beiying.apm.utils.LogX;

import java.util.LinkedList;
import java.util.List;

import static com.beiying.apm.Env.DEBUG;
import static com.beiying.apm.Env.TAG;


/**
 * AppStart存储类
 *
 * @author ArgusAPM Team
 */
public class AppStartStorage extends TableStorage {
    private final String SUB_TAG = "AppStartStorage";

    @Override
    public String getName() {
        return ApmTask.TASK_APP_START;
    }

    @Override
    public List<IInfo> readDb(String selection) {
        List<IInfo> infos = new LinkedList<IInfo>();
        Cursor cursor = null;
        try {
            cursor = Manager.getInstance().getConfig().appContext.getContentResolver()
                    .query(getTableUri(), null, selection, null, null);
            if (null == cursor || !cursor.moveToFirst()) {
                IOStreamUtils.closeQuietly(cursor);
                return infos;
            }
            int indexId = cursor.getColumnIndex(AppStartInfo.KEY_ID_RECORD);
            int indexStartTime = cursor.getColumnIndex(AppStartInfo.KEY_START_TIME);
            int indexTimeRecord = cursor.getColumnIndex(AppStartInfo.KEY_TIME_RECORD);
            do {
                infos.add(new AppStartInfo(
                        cursor.getInt(indexId),
                        cursor.getLong(indexTimeRecord),
                        cursor.getInt(indexStartTime)
                ));
            } while (cursor.moveToNext());
        } catch (Exception e) {
            if (DEBUG) {
                LogX.e(TAG, SUB_TAG, getName() + "; " + e.toString());
            }
        } finally {
            IOStreamUtils.closeQuietly(cursor);
        }
        return infos;
    }


}
