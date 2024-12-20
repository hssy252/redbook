package com.hssy.xiaohongshu.note.biz.rpc;

import com.hssy.xiaohongshu.distributed.id.generator.api.DistributedIdGeneratorFeignApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/20 15:59
 */

@Component
public class DistributedIdGeneratorRpcService {

    @Resource
    private DistributedIdGeneratorFeignApi distributedIdGeneratorFeignApi;

    /**
     * 获取笔记Id
     * @return
     */
    public String getSnowflakeId(){
        return distributedIdGeneratorFeignApi.getSnowflakeId("note");
    }

}
