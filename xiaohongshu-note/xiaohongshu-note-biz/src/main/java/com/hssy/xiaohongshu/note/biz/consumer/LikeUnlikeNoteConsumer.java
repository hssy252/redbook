package com.hssy.xiaohongshu.note.biz.consumer;

import com.google.common.util.concurrent.RateLimiter;
import com.hssy.framework.commom.util.JsonUtils;
import com.hssy.xiaohongshu.note.biz.constants.MQConstants;
import com.hssy.xiaohongshu.note.biz.domain.dataobject.NoteLikeDO;
import com.hssy.xiaohongshu.note.biz.domain.mapper.NoteLikeDOMapper;
import com.hssy.xiaohongshu.note.biz.model.dto.LikeUnlikeNoteMqDTO;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/30 11:31
 */

@Slf4j
@Component
@RocketMQMessageListener(consumerGroup = "xiaohongshu_group_" + MQConstants.TOPIC_LIKE_OR_UNLIKE,
    topic = MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE,
    consumeMode = ConsumeMode.ORDERLY
)
public class LikeUnlikeNoteConsumer implements RocketMQListener<Message> {

    // 每秒创建 5000 个令牌
    private RateLimiter rateLimiter = RateLimiter.create(5000);

    @Resource
    private NoteLikeDOMapper noteLikeDOMapper;

    @Override
    public void onMessage(Message message) {
        // 流量削峰：通过获取令牌，如果没有令牌可用，将阻塞，直到获得
        rateLimiter.acquire();

        // 幂等性: 通过联合唯一索引保证

        // 消息体
        String bodyJsonStr = new String(message.getBody());
        // 标签
        String tags = message.getTags();

        log.info("==> LikeUnlikeNoteConsumer 消费了消息 {}, tags: {}", bodyJsonStr, tags);

        // 根据 MQ 标签，判断操作类型
        if (Objects.equals(tags, MQConstants.TAG_LIKE)) { // 点赞笔记
            handleLikeNoteTagMessage(bodyJsonStr);
        } else if (Objects.equals(tags, MQConstants.TAG_UNLIKE)) { // 取消点赞笔记
            handleUnlikeNoteTagMessage(bodyJsonStr);
        }
    }

    /**
     * 笔记点赞
     * @param bodyJsonStr
     */
    private void handleLikeNoteTagMessage(String bodyJsonStr) {
        // 消息体 JSON 字符串转 DTO
        LikeUnlikeNoteMqDTO likeNoteMqDTO = JsonUtils.parseObject(bodyJsonStr, LikeUnlikeNoteMqDTO.class);

        if (Objects.isNull(likeNoteMqDTO)) return;

        // 用户ID
        Long userId = likeNoteMqDTO.getUserId();
        // 点赞的笔记ID
        Long noteId = likeNoteMqDTO.getNoteId();
        // 操作类型
        Integer type = likeNoteMqDTO.getType();
        // 点赞时间
        LocalDateTime createTime = likeNoteMqDTO.getCreateTime();

        // 构建 DO 对象
        NoteLikeDO noteLikeDO = NoteLikeDO.builder()
            .userId(userId)
            .noteId(noteId)
            .createTime(createTime)
            .status(type)
            .build();

        // 添加或更新笔记点赞记录
        int count = noteLikeDOMapper.insertOrUpdate(noteLikeDO);

        // TODO: 发送计数 MQ
    }

    /**
     * 笔记取消点赞
     * @param bodyJsonStr
     */
    private void handleUnlikeNoteTagMessage(String bodyJsonStr) {
    }
}
