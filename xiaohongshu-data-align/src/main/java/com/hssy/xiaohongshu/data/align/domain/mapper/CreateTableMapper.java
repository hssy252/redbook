package com.hssy.xiaohongshu.data.align.domain.mapper;

/**
 * 自动创建表
 */
public interface CreateTableMapper {

    /**
     * 创建日增量表：关注数计数变更
     * @param tableNameSuffix
     */
    void createDataAlignFollowingCountTempTable(String tableNameSuffix);
}