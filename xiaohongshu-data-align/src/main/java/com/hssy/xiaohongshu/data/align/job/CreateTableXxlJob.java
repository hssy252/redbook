package com.hssy.xiaohongshu.data.align.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;

@Component
public class CreateTableXxlJob {

    /**
     * 1、简单任务示例（Bean模式）
     */
    @XxlJob("createTableJobHandler")
    public void createTableJobHandler() throws Exception {
        XxlJobHelper.log("## 开始初始化明日增量数据表...");

        // TODO
    }

}