package com.hssy.xiaohongshu.data.align.job;

import com.hssy.xiaohongshu.data.align.constant.TableConstants;
import com.hssy.xiaohongshu.data.align.domain.mapper.CreateTableMapper;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Component
@RefreshScope
public class CreateTableXxlJob {

    /**
     * 表总分片数
     */
    @Value("${table.shards}")
    private int tableShards;

    @Resource
    private CreateTableMapper createTableMapper;

    /**
     * 1、简单任务示例（Bean模式）
     */
    @XxlJob("createTableJobHandler")
    public void createTableJobHandler() throws Exception {
        // 表后缀
        String date = LocalDate.now().plusDays(1) // 明日的日期
            .format(DateTimeFormatter.ofPattern("yyyyMMdd")); // 转字符串

        XxlJobHelper.log("## 开始创建日增量数据表，日期: {}...", date);

        if (tableShards > 0) {
            for (int hashKey = 0; hashKey < tableShards; hashKey++) {
                // 表名后缀
                String tableNameSuffix = TableConstants.buildTableNameSuffix(date, hashKey);

                // 创建表
                createTableMapper.createDataAlignFollowingCountTempTable(tableNameSuffix);
            }
        }

        XxlJobHelper.log("## 结束创建日增量数据表，日期: {}...", date);
    }


}