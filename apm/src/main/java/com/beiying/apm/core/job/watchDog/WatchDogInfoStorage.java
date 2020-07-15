package com.beiying.apm.core.job.watchDog;

import android.database.Cursor;


import com.beiying.apm.api.ApmTask;
import com.beiying.apm.core.IInfo;
import com.beiying.apm.core.Manager;
import com.beiying.apm.core.job.block.BlockInfo;
import com.beiying.apm.core.storage.TableStorage;
import com.beiying.apm.utils.IOStreamUtils;
import com.beiying.apm.utils.LogX;

import java.util.LinkedList;
import java.util.List;

import static com.beiying.apm.Env.DEBUG;
import static com.beiying.apm.Env.TAG;


/**
 * @author ArgusAPM Team
 */
public class WatchDogInfoStorage extends TableStorage {
    private final String SUB_TAG = "WatchDogInfoStorage";

    @Override
    public String getName() {
        return ApmTask.TASK_WATCHDOG;
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
            int indexTimeRecord = cursor.getColumnIndex(BlockInfo.KEY_TIME_RECORD);
            int indexProcessName = cursor.getColumnIndex(BlockInfo.DBKey.PROCESS_NAME);
            int indexBlockStack = cursor.getColumnIndex(BlockInfo.DBKey.BLOCK_STACK);
            int indexBlockTime = cursor.getColumnIndex(BlockInfo.DBKey.BLOCK_TIME);
            do {
                WatchDogInfo wachDogInfo = new WatchDogInfo();
                wachDogInfo.processName = cursor.getString(indexProcessName);
                wachDogInfo.blockStack = cursor.getString(indexBlockStack);
                wachDogInfo.blockTime = cursor.getInt(indexBlockTime);
                wachDogInfo.setRecordTime(cursor.getLong(indexTimeRecord));
                infos.add(wachDogInfo);
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
