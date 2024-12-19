package com.hssy.xiaohongshu.user.biz.rpc;

import com.hssy.xiaohongshu.distributed.id.generator.api.DistributedIdGeneratorFeignApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/19 21:25
 */
@Component
public class DistributedIdGeneratorRpcService {

    @Resource
    private DistributedIdGeneratorFeignApi idGeneratorFeignApi;

    /**
     * Leaf 号段模式：小红书 ID 业务标识
     */
    private static final String BIZ_TAG_XIAOHONGSHU_ID = "leaf-segment-xiaohongshu-id";

    /**
     * Leaf 号段模式：用户 ID 业务标识
     */
    private static final String BIZ_TAG_USER_ID = "leaf-segment-user-id";

    /**
     * 调用分布式 ID 生成服务生成小哈书 ID
     *
     * @return
     */
    public String getXiaohongshuId() {
        return idGeneratorFeignApi.getSegmentId(BIZ_TAG_XIAOHONGSHU_ID);
    }

    /**
     * 调用分布式id服务生成用户id
     * @return
     */
    public String getUserId(){
        return idGeneratorFeignApi.getSegmentId(BIZ_TAG_USER_ID);
    }

}
