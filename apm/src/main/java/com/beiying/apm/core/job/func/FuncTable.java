package com.beiying.apm.core.job.func;

import android.text.TextUtils;

import com.beiying.apm.api.ApmTask;
import com.beiying.apm.core.storage.DbHelper;
import com.beiying.apm.core.storage.ITable;


/**
 * @author ArgusAPM Team
 */
public class FuncTable implements ITable {
    @Override
    public String createSql() {
        return TextUtils.concat(
                DbHelper.CREATE_TABLE_PREFIX + getTableName(),
                "(", FuncInfo.KEY_ID_RECORD, " INTEGER PRIMARY KEY AUTOINCREMENT,",
                FuncInfo.KEY_TIME_RECORD, DbHelper.DATA_TYPE_INTEGER,
                FuncInfo.KEY_TYPE, DbHelper.DATA_TYPE_TEXT,
                FuncInfo.KEY_PARAM, DbHelper.DATA_TYPE_TEXT,
                FuncInfo.KEY_RESERVE_1, DbHelper.DATA_TYPE_TEXT,
                FuncInfo.KEY_RESERVE_2, DbHelper.DATA_TYPE_TEXT_SUF
        ).toString();
    }

    @Override
    public String getTableName() {
        return ApmTask.TASK_FUNC;
    }
}
