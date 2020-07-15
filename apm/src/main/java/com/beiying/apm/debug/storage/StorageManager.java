package com.beiying.apm.debug.storage;


import com.beiying.apm.Env;

/**
 * 日志数据存储管理类
 *
 * @author ArgusAPM Team
 */
public class StorageManager {
    /**
     * 按行保存到文本文件
     *
     * @param line
     */
    public static void saveToFile(String line) {
        TraceWriter.log(Env.TAG, line);
    }

}
