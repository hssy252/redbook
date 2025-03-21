package com.hssy.xiaohongshu.data.align.constant;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2025/3/21 14:28
 */
public class TableConstants {


    /**
     * 表名中的分隔符
     */
    private static final String TABLE_NAME_SEPARATE = "_";

    /**
     * 拼接表名后缀
     * @param hashKey
     * @return
     */
    public static String buildTableNameSuffix(String date, int hashKey) {
        // 拼接完整的表名
        return date + TABLE_NAME_SEPARATE + hashKey;
    }

}
