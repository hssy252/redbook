package com.hssy.xiaohongshu.note.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.hssy.framework.biz.context.holder.LoginUserContextHolder;
import com.hssy.framework.commom.exception.BizException;
import com.hssy.framework.commom.response.Response;
import com.hssy.framework.commom.util.JsonUtils;
import com.hssy.xiaohongshu.kv.dto.req.AddNoteContentReqDTO;
import com.hssy.xiaohongshu.note.biz.constants.RedisKeyConstants;
import com.hssy.xiaohongshu.note.biz.domain.dataobject.NoteDO;
import com.hssy.xiaohongshu.note.biz.domain.mapper.NoteDOMapper;
import com.hssy.xiaohongshu.note.biz.domain.mapper.TopicDOMapper;
import com.hssy.xiaohongshu.note.biz.enums.NoteStatusEnum;
import com.hssy.xiaohongshu.note.biz.enums.NoteTypeEnum;
import com.hssy.xiaohongshu.note.biz.enums.NoteVisibleEnum;
import com.hssy.xiaohongshu.note.biz.enums.ResponseCodeEnum;
import com.hssy.xiaohongshu.note.biz.model.vo.FindNoteDetailReqVO;
import com.hssy.xiaohongshu.note.biz.model.vo.FindNoteDetailRspVO;
import com.hssy.xiaohongshu.note.biz.model.vo.PublishNoteReqVO;
import com.hssy.xiaohongshu.note.biz.rpc.DistributedIdGeneratorRpcService;
import com.hssy.xiaohongshu.note.biz.rpc.KeyValueRpcService;
import com.hssy.xiaohongshu.note.biz.rpc.UserRpcService;
import com.hssy.xiaohongshu.note.biz.service.NoteService;
import com.hssy.xiaohongshu.user.api.dto.resp.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/20 16:07
 */
@Service
@Slf4j
public class NoteServiceImpl implements NoteService {

    @Resource
    private DistributedIdGeneratorRpcService distributedIdGeneratorRpcService;

    @Resource
    private KeyValueRpcService keyValueRpcService;

    @Resource
    private TopicDOMapper topicDOMapper;

    @Resource
    private NoteDOMapper noteDOMapper;

    @Resource
    private UserRpcService userRpcService;

    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Resource
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 笔记详情本地缓存
     */
    private static final Cache<Long, String> LOCAL_CACHE = Caffeine.newBuilder()
        .initialCapacity(10000) // 设置初始容量为 10000 个条目
        .maximumSize(10000) // 设置缓存的最大容量为 10000 个条目
        .expireAfterWrite(1, TimeUnit.HOURS) // 设置缓存条目在写入后 1 小时过期
        .build();


    /**
     * 发布笔记内容
     * @param publishNoteReqVO
     * @return
     */
    @Override
    public Response<?> publishNote(PublishNoteReqVO publishNoteReqVO) {
        //判断笔记类型是否有误
        Integer type = publishNoteReqVO.getType();

        NoteTypeEnum noteTypeEnum = NoteTypeEnum.valueOf(type);
        if (Objects.isNull(noteTypeEnum)){
            throw new BizException(ResponseCodeEnum.NOTE_TYPE_ERROR);
        }

        String imgUris = null;
        // 笔记内容是否为空，默认值为 true，即空
        Boolean isContentEmpty = true;
        String videoUri = null;

        switch (noteTypeEnum){
            // 图文笔记
            case IMAGE_TEXT -> {
                List<String> uris = publishNoteReqVO.getImgUris();
                Preconditions.checkArgument(CollUtil.isNotEmpty(uris),"图片不能为空");
                Preconditions.checkArgument(uris.size()<=8,"图片数量不能大于8张");
                imgUris = StringUtils.join(uris,",");
            }
            case VIDEO -> {
                String uri = publishNoteReqVO.getVideoUri();
                Preconditions.checkArgument(StringUtils.isNotBlank(uri),"视频不能为空");
                videoUri = uri;
            }
        }

        String noteId = null;

        // 生产笔记id
        noteId = distributedIdGeneratorRpcService.getSnowflakeId();

        // 笔记内容不为空，要保存到Cassandra
        String contentUuid = null;
        String content = publishNoteReqVO.getContent();
        if(StringUtils.isNotBlank(content)){
            // 内容是否为空，置为 false，即不为空
            isContentEmpty = false;
            // 生成笔记内容 UUID
            contentUuid  = UUID.randomUUID().toString();
            // 保存到cassandra数据库里
            boolean isSuccess = keyValueRpcService.saveNoteContent(contentUuid, content);
            if(!isSuccess){
                throw new BizException(ResponseCodeEnum.NOTE_PUBLISH_FAIL);
            }
        }

        // 话题
        Long topicId = publishNoteReqVO.getTopicId();
        String topicName = null;
        if (Objects.nonNull(topicId)) {
            // 获取话题名称
            topicName = topicDOMapper.selectNameByPrimaryKey(topicId);
        }

        // 发布者用户 ID
        Long creatorId = LoginUserContextHolder.getUserId();

        // 构建笔记 DO 对象
        NoteDO noteDO = NoteDO.builder()
            .id(Long.valueOf(noteId))
            .isContentEmpty(isContentEmpty)
            .creatorId(creatorId)
            .imgUris(imgUris)
            .title(publishNoteReqVO.getTitle())
            .topicId(topicId)
            .topicName(topicName)
            .type(type)
            .visible(NoteVisibleEnum.PUBLIC.getCode())
            .createTime(LocalDateTime.now())
            .updateTime(LocalDateTime.now())
            .status(NoteStatusEnum.NORMAL.getCode())
            .isTop(Boolean.FALSE)
            .videoUri(videoUri)
            .contentUuid(contentUuid)
            .build();

        try {
            // 笔记入库存储
            noteDOMapper.insert(noteDO);
        } catch (Exception e) {
            log.error("==> 笔记存储失败", e);

            // RPC: 笔记保存失败，则删除笔记内容
            if (StringUtils.isNotBlank(contentUuid)) {
                keyValueRpcService.deleteNoteContent(contentUuid);
            }
        }

        return Response.success();
    }

    @Override
    public Response<FindNoteDetailRspVO> findNoteDetail(FindNoteDetailReqVO findNoteDetailReqVO) {
        // 当前登录用户
        Long userId = LoginUserContextHolder.getUserId();

        // 笔记id
        Long noteId = findNoteDetailReqVO.getId();

        // 先从本地缓存中查询
        String findNoteDetailRspVOStrLocalCache = LOCAL_CACHE.getIfPresent(noteId);
        if (StringUtils.isNotBlank(findNoteDetailRspVOStrLocalCache)) {
            FindNoteDetailRspVO findNoteDetailRspVO = JsonUtils.parseObject(findNoteDetailRspVOStrLocalCache, FindNoteDetailRspVO.class);
            log.info("==> 命中了本地缓存；{}", findNoteDetailRspVOStrLocalCache);
            // 可见性校验
            checkNoteVisibleFromVO(userId, findNoteDetailRspVO);
            return Response.success(findNoteDetailRspVO);
        }

        // 先查redis里有没有
        String key = RedisKeyConstants.buildNoteDetailKey(findNoteDetailReqVO.getId());
        String noteJsonStr = redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(noteJsonStr)){
            // 如果是null说明缓存穿透了
            if (StringUtils.equals("null",noteJsonStr)){
                return null;
            }
            FindNoteDetailRspVO findNoteDetailRspVO = JsonUtils.parseObject(noteJsonStr, FindNoteDetailRspVO.class);
            // 可见性校验
            if (Objects.nonNull(findNoteDetailRspVO)) {
                Integer visible = findNoteDetailRspVO.getVisible();
                checkNoteVisible(visible, userId, findNoteDetailRspVO.getCreatorId());
            }
            // 异步线程中将用户信息存入本地缓存
            threadPoolTaskExecutor.submit(() -> {
                // 写入本地缓存
                LOCAL_CACHE.put(noteId, JsonUtils.toJsonString(findNoteDetailRspVO));
            });
            return Response.success(findNoteDetailRspVO);
        }

        // 根据id查询笔记
        NoteDO noteDO = noteDOMapper.selectByPrimaryKey(noteId);
        if (Objects.isNull(noteDO)){
            // 缓存击穿，设置null值
            threadPoolTaskExecutor.execute(()->{
                int expiredSeconds = 60 + RandomUtil.randomInt(60);
                redisTemplate.opsForValue().set(key,"null",expiredSeconds, TimeUnit.SECONDS);
            });
            throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }



        // 可见性校验
        Integer visible = noteDO.getVisible();
        checkNoteVisible(visible, userId, noteDO.getCreatorId());

        // RPC: 调用用户服务
        Long creatorId = noteDO.getCreatorId();
        FindUserByIdRspDTO findUserByIdRspDTO = userRpcService.findUserInfoById(creatorId);


        // RPC: 调用 K-V 存储服务获取内容
        String content = null;
        if (Objects.equals(noteDO.getIsContentEmpty(), Boolean.FALSE)) {
            content = keyValueRpcService.findNoteContentById(noteDO.getContentUuid());
        }

        // 笔记类型
        Integer noteType = noteDO.getType();
        // 图文笔记图片链接(字符串)
        String imgUrisStr = noteDO.getImgUris();
        // 图文笔记图片链接(集合)
        List<String> imgUris = null;
        // 如果查询的是图文笔记，需要将图片链接的逗号分隔开，转换成集合
        if (Objects.equals(noteType, NoteTypeEnum.IMAGE_TEXT.getCode())
            && StringUtils.isNotBlank(imgUrisStr)) {
            imgUris = List.of(imgUrisStr.split(","));
        }

        // 构建返参 VO 实体类
        FindNoteDetailRspVO findNoteDetailRspVO = FindNoteDetailRspVO.builder()
            .id(noteDO.getId())
            .type(noteDO.getType())
            .title(noteDO.getTitle())
            .content(content)
            .imgUris(imgUris)
            .topicId(noteDO.getTopicId())
            .topicName(noteDO.getTopicName())
            .creatorId(noteDO.getCreatorId())
            .creatorName(findUserByIdRspDTO.getNickName())
            .avatar(findUserByIdRspDTO.getAvatar())
            .videoUri(noteDO.getVideoUri())
            .updateTime(noteDO.getUpdateTime())
            .visible(noteDO.getVisible())
            .build();

        // 将结果缓存到redis当中
        threadPoolTaskExecutor.submit(()->{
            String noteDetailJson = JsonUtils.toJsonString(findNoteDetailRspVO);
            // 过期时间（保底1天 + 随机秒数，将缓存过期时间打散，防止同一时间大量缓存失效，导致数据库压力太大）
            long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);
            redisTemplate.opsForValue().set(key, noteDetailJson, expireSeconds, TimeUnit.SECONDS);
        });

        return Response.success(findNoteDetailRspVO);
    }

    /**
     * 校验笔记的可见性
     * @param visible 是否可见
     * @param currUserId 当前用户 ID
     * @param creatorId 笔记创建者
     */
    private void checkNoteVisible(Integer visible, Long currUserId, Long creatorId) {
        if (Objects.equals(visible, NoteVisibleEnum.PRIVATE.getCode())
            && !Objects.equals(currUserId, creatorId)) { // 仅自己可见, 并且访问用户为笔记创建者
            throw new BizException(ResponseCodeEnum.NOTE_PRIVATE);
        }
    }

    /**
     * 校验笔记的可见性（针对 VO 实体类）
     * @param userId
     * @param findNoteDetailRspVO
     */
    private void checkNoteVisibleFromVO(Long userId, FindNoteDetailRspVO findNoteDetailRspVO) {
        if (Objects.nonNull(findNoteDetailRspVO)) {
            Integer visible = findNoteDetailRspVO.getVisible();
            checkNoteVisible(visible, userId, findNoteDetailRspVO.getCreatorId());
        }
    }
}
