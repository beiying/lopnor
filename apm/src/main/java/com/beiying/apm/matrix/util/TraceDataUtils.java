package com.beiying.apm.matrix.util;

import com.beiying.apm.matrix.trace.MethodItem;

import java.util.LinkedList;
import java.util.List;

public class TraceDataUtils {

    public interface IStructuredDataFilter {
        boolean isFilter(long during, int filterCount);

        int getFilterMaxCount();

        void fallback(List<MethodItem> stack, int size);
    }

    public static void structuredDataToStack(long[] buffer, LinkedList<MethodItem> result, boolean isStrict, long endTime) {
        long lastInId = 0L;
        int depth = 0;
        LinkedList<Long> rawData = new LinkedList<>();
        boolean isBegin = !isStrict;
    }

    public static void trimStack(List<MethodItem> stack, int targetCount, IStructuredDataFilter filter) {
        if (0 > targetCount) {
            stack.clear();
            return;
        }
    }
}
