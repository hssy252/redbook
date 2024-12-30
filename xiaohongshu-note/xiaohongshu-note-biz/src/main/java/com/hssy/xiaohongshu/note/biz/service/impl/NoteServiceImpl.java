package com.hssy.xiaohongshu.note.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.hssy.framework.biz.context.holder.LoginUserContextHolder;
import com.hssy.framework.commom.exception.BizException;
import com.hssy.framework.commom.response.Response;
import com.hssy.framework.commom.util.DateUtils;
import com.hssy.framework.commom.util.JsonUtils;
import com.hssy.xiaohongshu.note.biz.constants.MQConstants;
import com.hssy.xiaohongshu.note.biz.constants.RedisKeyConstants;
import com.hssy.xiaohongshu.note.biz.domain.dataobject.NoteCollectionDO;
import com.hssy.xiaohongshu.note.biz.domain.dataobject.NoteDO;
import com.hssy.xiaohongshu.note.biz.domain.dataobject.NoteLikeDO;
import com.hssy.xiaohongshu.note.biz.domain.mapper.NoteCollectionDOMapper;
import com.hssy.xiaohongshu.note.biz.domain.mapper.NoteDOMapper;
import com.hssy.xiaohongshu.note.biz.domain.mapper.NoteLikeDOMapper;
import com.hssy.xiaohongshu.note.biz.domain.mapper.TopicDOMapper;
import com.hssy.xiaohongshu.note.biz.enums.LikeUnlikeNoteTypeEnum;
import com.hssy.xiaohongshu.note.biz.enums.NoteCollectLuaResultEnum;
import com.hssy.xiaohongshu.note.biz.enums.NoteLikeLuaResultEnum;
import com.hssy.xiaohongshu.note.biz.enums.NoteStatusEnum;
import com.hssy.xiaohongshu.note.biz.enums.NoteTypeEnum;
import com.hssy.xiaohongshu.note.biz.enums.NoteVisibleEnum;
import com.hssy.xiaohongshu.note.biz.enums.ResponseCodeEnum;
import com.hssy.xiaohongshu.note.biz.model.dto.LikeUnlikeNoteMqDTO;
import com.hssy.xiaohongshu.note.biz.model.dto.UnlikeNoteReqVO;
import com.hssy.xiaohongshu.note.biz.model.vo.CollectNoteReqVO;
import com.hssy.xiaohongshu.note.biz.model.vo.DeleteNoteReqVO;
import com.hssy.xiaohongshu.note.biz.model.vo.FindNoteDetailReqVO;
import com.hssy.xiaohongshu.note.biz.model.vo.FindNoteDetailRspVO;
import com.hssy.xiaohongshu.note.biz.model.vo.LikeNoteReqVO;
import com.hssy.xiaohongshu.note.biz.model.vo.PublishNoteReqVO;
import com.hssy.xiaohongshu.note.biz.model.vo.TopNoteReqVO;
import com.hssy.xiaohongshu.note.biz.model.vo.UpdateNoteReqVO;
import com.hssy.xiaohongshu.note.biz.model.vo.UpdateNoteVisibleOnlyMeReqVO;
import com.hssy.xiaohongshu.note.biz.rpc.DistributedIdGeneratorRpcService;
import com.hssy.xiaohongshu.note.biz.rpc.KeyValueRpcService;
import com.hssy.xiaohongshu.note.biz.rpc.UserRpcService;
import com.hssy.xiaohongshu.note.biz.service.NoteService;
import com.hssy.xiaohongshu.user.api.dto.resp.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Resource
    private NoteLikeDOMapper noteLikeDOMapper;

    @Resource
    private NoteCollectionDOMapper noteCollectionDOMapper;

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
    @SneakyThrows
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
        // 并发优化查询
        Long creatorId = noteDO.getCreatorId();
        CompletableFuture<FindUserByIdRspDTO> userInfoFuture = CompletableFuture.supplyAsync(() -> {
             return userRpcService.findUserInfoById(creatorId);
        }, threadPoolTaskExecutor);

        // RPC: 调用 K-V 存储服务获取内容
        // 并发优化查询
        CompletableFuture<String> contentResultFuture = CompletableFuture.completedFuture(null);
        if (Objects.equals(noteDO.getIsContentEmpty(), Boolean.FALSE)) {
            contentResultFuture = CompletableFuture
                .supplyAsync(() -> keyValueRpcService.findNoteContentById(noteDO.getContentUuid()), threadPoolTaskExecutor);
        }

        CompletableFuture<String> finalContentResultFuture = contentResultFuture;
        CompletableFuture<FindNoteDetailRspVO> resultFuture = CompletableFuture
            .allOf(userInfoFuture, contentResultFuture)
            .thenApply(s -> {
                // 获取 Future 返回的结果
                FindUserByIdRspDTO findUserByIdRspDTO = userInfoFuture.join();
                String content = finalContentResultFuture.join();

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
                return FindNoteDetailRspVO.builder()
                    .id(noteDO.getId())
                    .type(noteDO.getType())
                    .title(noteDO.getTitle())
                    .content(content)
                    .imgUris(imgUris)
                    .topicId(noteDO.getTopicId())
                    .topicName(noteDO.getTopicName())
                    .creatorId(userId)
                    .creatorName(findUserByIdRspDTO.getNickName())
                    .avatar(findUserByIdRspDTO.getAvatar())
                    .videoUri(noteDO.getVideoUri())
                    .updateTime(noteDO.getUpdateTime())
                    .visible(noteDO.getVisible())
                    .build();

            });

        // 获取拼装后的 FindNoteDetailRspVO
        FindNoteDetailRspVO findNoteDetailRspVO = resultFuture.get();


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
     * 笔记更新
     *
     * @param updateNoteReqVO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> updateNote(UpdateNoteReqVO updateNoteReqVO) {
        // 笔记 ID
        Long noteId = updateNoteReqVO.getId();
        // 笔记类型
        Integer type = updateNoteReqVO.getType();

        // 获取对应类型的枚举
        NoteTypeEnum noteTypeEnum = NoteTypeEnum.valueOf(type);

        // 若非图文、视频，抛出业务业务异常
        if (Objects.isNull(noteTypeEnum)) {
            throw new BizException(ResponseCodeEnum.NOTE_TYPE_ERROR);
        }

        String imgUris = null;
        String videoUri = null;
        switch (noteTypeEnum) {
            case IMAGE_TEXT: // 图文笔记
                List<String> imgUriList = updateNoteReqVO.getImgUris();
                // 校验图片是否为空
                Preconditions.checkArgument(CollUtil.isNotEmpty(imgUriList), "笔记图片不能为空");
                // 校验图片数量
                Preconditions.checkArgument(imgUriList.size() <= 8, "笔记图片不能多于 8 张");

                imgUris = StringUtils.join(imgUriList, ",");
                break;
            case VIDEO: // 视频笔记
                videoUri = updateNoteReqVO.getVideoUri();
                // 校验视频链接是否为空
                Preconditions.checkArgument(StringUtils.isNotBlank(videoUri), "笔记视频不能为空");
                break;
            default:
                break;
        }

        // 当前登录用户 ID
        Long currUserId = LoginUserContextHolder.getUserId();
        NoteDO selectNoteDO = noteDOMapper.selectByPrimaryKey(noteId);

        // 笔记不存在
        if (Objects.isNull(selectNoteDO)) {
            throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }

        // 判断权限：非笔记发布者不允许更新笔记
        if (!Objects.equals(currUserId, selectNoteDO.getCreatorId())) {
            throw new BizException(ResponseCodeEnum.NOTE_CANT_OPERATE);
        }

        // 话题
        Long topicId = updateNoteReqVO.getTopicId();
        String topicName = null;
        if (Objects.nonNull(topicId)) {
            topicName = topicDOMapper.selectNameByPrimaryKey(topicId);

            // 判断一下提交的话题, 是否是真实存在的
            if (StringUtils.isBlank(topicName)) {
                throw new BizException(ResponseCodeEnum.TOPIC_NOT_FOUND);
            }
        }


        // 更新笔记元数据表 t_note
        String content = updateNoteReqVO.getContent();
        NoteDO noteDO = NoteDO.builder()
            .id(noteId)
            .isContentEmpty(StringUtils.isBlank(content))
            .imgUris(imgUris)
            .title(updateNoteReqVO.getTitle())
            .topicId(updateNoteReqVO.getTopicId())
            .topicName(topicName)
            .type(type)
            .updateTime(LocalDateTime.now())
            .videoUri(videoUri)
            .build();

        noteDOMapper.updateByPrimaryKey(noteDO);

        // 删除 Redis 缓存
        String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
        redisTemplate.delete(noteDetailRedisKey);

        // 删除本地缓存
        // LOCAL_CACHE.invalidate(noteId);

        // 同步发送广播模式 MQ，将所有实例中的本地缓存都删除掉（解决本地缓存与redis缓存不一致的问题）
        rocketMQTemplate.syncSend(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE, noteId);
        log.info("====> MQ：删除笔记本地缓存发送成功...");

        // 笔记内容更新
        // 查询此篇笔记内容对应的 UUID
        NoteDO noteDO1 = noteDOMapper.selectByPrimaryKey(noteId);
        String contentUuid = noteDO1.getContentUuid();

        // 笔记内容是否更新成功
        boolean isUpdateContentSuccess = false;
        if (StringUtils.isBlank(content)) {
            // 若笔记内容为空，则删除 K-V 存储
            isUpdateContentSuccess = keyValueRpcService.deleteNoteContent(contentUuid);
        } else {
            // 调用 K-V 更新短文本
            isUpdateContentSuccess = keyValueRpcService.saveNoteContent(contentUuid, content);
        }

        // 如果更新失败，抛出业务异常，回滚事务
        if (!isUpdateContentSuccess) {
            throw new BizException(ResponseCodeEnum.NOTE_UPDATE_FAIL);
        }

        return Response.success();
    }

    /**
     * 删除本地笔记缓存
     * @param noteId
     */
    @Override
    public void deleteNoteLocalCache(Long noteId) {
        LOCAL_CACHE.invalidate(noteId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> deleteNote(DeleteNoteReqVO deleteNoteReqVO) {
        // 笔记 ID
        Long noteId = deleteNoteReqVO.getId();

        NoteDO selectNoteDO = noteDOMapper.selectByPrimaryKey(noteId);

        // 判断笔记是否存在
        if (Objects.isNull(selectNoteDO)) {
            throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }

        // 判断权限：非笔记发布者不允许删除笔记
        Long currUserId = LoginUserContextHolder.getUserId();
        if (!Objects.equals(currUserId, selectNoteDO.getCreatorId())) {
            throw new BizException(ResponseCodeEnum.NOTE_CANT_OPERATE);
        }

        // 逻辑删除
        NoteDO noteDO = NoteDO.builder()
            .id(noteId)
            .status(NoteStatusEnum.DELETED.getCode())
            .updateTime(LocalDateTime.now())
            .build();

        int count = noteDOMapper.updateByPrimaryKeySelective(noteDO);

        // 若影响的行数为 0，则表示该笔记不存在
        if (count == 0) {
            throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }

        // 删除缓存
        String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
        redisTemplate.delete(noteDetailRedisKey);

        // 同步发送广播模式 MQ，将所有实例中的本地缓存都删除掉
        rocketMQTemplate.syncSend(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE, noteId);
        log.info("====> MQ：删除笔记本地缓存发送成功...");

        return Response.success();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Response<?> visibleOnlyMe(UpdateNoteVisibleOnlyMeReqVO updateNoteVisibleOnlyMeReqVO) {
        Long noteId = updateNoteVisibleOnlyMeReqVO.getId();

        NoteDO selectNoteDO = noteDOMapper.selectByPrimaryKey(noteId);

        // 判断笔记是否存在
        if (Objects.isNull(selectNoteDO)) {
            throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }

        // 判断权限：非笔记发布者不允许修改笔记权限
        Long currUserId = LoginUserContextHolder.getUserId();
        if (!Objects.equals(currUserId, selectNoteDO.getCreatorId())) {
            throw new BizException(ResponseCodeEnum.NOTE_CANT_OPERATE);
        }

        // 构建更新 DO 实体类
        NoteDO noteDO = NoteDO.builder()
            .id(noteId)
            .visible(NoteVisibleEnum.PRIVATE.getCode()) // 可见性设置为仅对自己可见
            .updateTime(LocalDateTime.now())
            .build();

        int count = noteDOMapper.updateVisibleOnlyMe(noteDO);
        if (count==0){
            throw new BizException(ResponseCodeEnum.NOTE_CANT_VISIBLE_ONLY_ME);
        }

        // 删除redis缓存
        String key = RedisKeyConstants.buildNoteDetailKey(noteId);
        redisTemplate.delete(key);

        // 删除本地缓存
        rocketMQTemplate.syncSend(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE,noteId);
        log.info("发送消息到mq，笔记Id为：{}",noteId);

        return Response.success();
    }

    /**
     * 笔记置顶 / 取消置顶
     *
     * @param topNoteReqVO
     * @return
     */
    @Override
    public Response<?> topNote(TopNoteReqVO topNoteReqVO) {
        // 笔记 ID
        Long noteId = topNoteReqVO.getId();
        // 是否置顶
        Boolean isTop = topNoteReqVO.getIsTop();

        // 当前登录用户 ID
        Long currUserId = LoginUserContextHolder.getUserId();

        // 构建置顶/取消置顶 DO 实体类
        NoteDO noteDO = NoteDO.builder()
            .id(noteId)
            .isTop(isTop)
            .updateTime(LocalDateTime.now())
            .creatorId(currUserId) // 只有笔记所有者，才能置顶/取消置顶笔记
            .build();

        int count = noteDOMapper.updateIsTop(noteDO);

        if (count == 0) {
            throw new BizException(ResponseCodeEnum.NOTE_CANT_OPERATE);
        }

        // 删除 Redis 缓存
        String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
        redisTemplate.delete(noteDetailRedisKey);

        // 同步发送广播模式 MQ，将所有实例中的本地缓存都删除掉
        rocketMQTemplate.syncSend(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE, noteId);
        log.info("====> MQ：删除笔记本地缓存发送成功...");

        return Response.success();
    }

    @Override
    public Response<?> likeNote(LikeNoteReqVO likeNoteReqVO) {
        // 1. 校验被点赞的笔记是否存在
        Long noteId = likeNoteReqVO.getId();
        checkNoteIsExist(noteId);

        // 2. 判断目标笔记，是否已经点赞过
        Long userId = LoginUserContextHolder.getUserId();
        String bloomKey = RedisKeyConstants.buildBloomUserNoteLikeListKey(userId);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_note_like_check.lua")));

        Long result = redisTemplate.execute(script, Collections.singletonList(bloomKey), noteId);
        NoteLikeLuaResultEnum resultEnum = NoteLikeLuaResultEnum.valueOf(result);

        String zSetKey = RedisKeyConstants.buildUserNoteLikeZSetKey(userId);

       if (resultEnum!=null){
           switch (resultEnum){
               case NOT_EXIST -> {
                   // 从数据库中校验笔记是否被点赞，并异步初始化布隆过滤器，设置过期时间
                   int count = noteLikeDOMapper.selectCountByUserIdAndNoteId(userId, noteId);

                   // 保底1天+随机秒数
                   long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);

                   // 目标笔记已经被点赞
                   if (count > 0) {
                       // 异步初始化布隆过滤器
                       asyncBatchAddNoteLike2BloomAndExpire(userId, expireSeconds,bloomKey);
                       throw new BizException(ResponseCodeEnum.NOTE_ALREADY_LIKED);
                   }

                   // 若数据库中也没有点赞记录，说明该用户还未点赞过该笔记
                   // Lua 脚本路径
                   script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_add_note_like_and_expire.lua")));
                   // 返回值类型
                   script.setResultType(Long.class);
                   redisTemplate.execute(script, Collections.singletonList(bloomKey), noteId, expireSeconds);
                   // 同步数据库里的数据
                   asyncBatchAddNoteLike2BloomAndExpire(userId,expireSeconds,bloomKey);

               }
               case NOTE_LIKED -> {
                   // 因为布隆过滤器无法删除，所以会存在误判，于是还是要查询用户的zset点赞缓存列表
                   Double score = redisTemplate.opsForZSet().score(zSetKey, noteId);
                   // 如果zset里不存在，还要去查数据库，因为zset只保存最近一百个点赞(也有可能是zset过期了)
                   if (Objects.isNull(score)){
                       int count = noteLikeDOMapper.selectNoteIsLiked(userId, noteId);
                       if (count > 0){
                           // 数据库里面有点赞记录，而 Redis 中 ZSet 不存在，需要重新异步初始化 ZSet
                           asyncInitUserNoteLikesZSet(userId, zSetKey);
                           // 如果数据库存在
                           throw new BizException(ResponseCodeEnum.NOTE_ALREADY_LIKED);
                       }
                   }else {
                       throw new BizException(ResponseCodeEnum.NOTE_ALREADY_LIKED);
                   }
               }
           }
       }

        // 3. 更新用户 ZSET 点赞列表
        LocalDateTime now = LocalDateTime.now();

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setResultType(Long.class);
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/note_like_check_and_update_zset.lua")));

        Long zSetResult = redisTemplate.execute(redisScript, Collections.singletonList(zSetKey), noteId, DateUtils.localDateTime2Timestamp(now));

        // zset不存在
        if (Objects.equals(NoteLikeLuaResultEnum.valueOf(zSetResult),NoteLikeLuaResultEnum.NOT_EXIST)){
            // 初始化Zset
            List<NoteLikeDO> likeDOList = noteLikeDOMapper.selectLikedByUserIdAndLimit(userId, 100);
            if (CollUtil.isNotEmpty(likeDOList)){
                // 保底1天+随机秒数
                long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);
                // 构建 Lua 参数
                Object[] luaArgs = buildNoteLikeZSetLuaArgs(likeDOList, expireSeconds);

                DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
                // Lua 脚本路径
                script2.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/batch_add_note_like_zset_and_expire.lua")));
                // 返回值类型
                script2.setResultType(Long.class);

                redisTemplate.execute(script2, Collections.singletonList(zSetKey), luaArgs);

                // 再次调用 note_like_check_and_update_zset.lua 脚本，将点赞的笔记添加到 zset 中
                redisTemplate.execute(script, Collections.singletonList(zSetKey), noteId, DateUtils.localDateTime2Timestamp(now));
            }
        }


        // 4. 发送 MQ, 将点赞数据落库
        LikeUnlikeNoteMqDTO mqDTO = LikeUnlikeNoteMqDTO.builder()
            .userId(userId)
            .createTime(now)
            .type(LikeUnlikeNoteTypeEnum.LIKE.getCode())
            .build();

        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(mqDTO)).build();
        String topic = MQConstants.TOPIC_LIKE_OR_UNLIKE + ":" + MQConstants.TAG_LIKE;
        String hashKey = String.valueOf(userId);

        rocketMQTemplate.asyncSendOrderly(topic, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("## 【笔记点赞】消息发送成功，sendRedult: {}",sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("## 【笔记点赞】消息发送失败, throwable：" + throwable);
            }
        });

        return Response.success();
    }

    /**
     * 取消点赞笔记
     *
     * @param unlikeNoteReqVO
     * @return
     */
    @Override
    public Response<?> unlikeNote(UnlikeNoteReqVO unlikeNoteReqVO) {
        // 笔记ID
        Long noteId = unlikeNoteReqVO.getId();

        // 1. 校验笔记是否真实存在
        checkNoteIsExist(noteId);

        // 2. 校验笔记是否被点赞过
        Long userId = LoginUserContextHolder.getUserId();
        String bloomKey = RedisKeyConstants.buildBloomUserNoteLikeListKey(userId);
        // 先查bloom过滤器，如果过滤器没有就异步初始化过滤器
        DefaultRedisScript<Long> bloomScript = new DefaultRedisScript<>();
        bloomScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_note_dislike_check.lua")));
        bloomScript.setResultType(Long.class);

        Long result = redisTemplate.execute(bloomScript, Collections.singletonList(bloomKey), noteId);
        NoteLikeLuaResultEnum resultEnum = NoteLikeLuaResultEnum.valueOf(result);

        switch (resultEnum){
            case NOT_EXIST -> {
                // 保底1天+随机秒数
                long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);

                // 异步初始化布隆过滤器
                asyncBatchAddNoteLike2BloomAndExpire(userId, expireSeconds, bloomKey);

                // 从数据库中校验笔记是否被点赞
                int count = noteLikeDOMapper.selectCountByUserIdAndNoteId(userId, noteId);

                // 未点赞，无法取消点赞操作，抛出业务异常
                if (count == 0) throw new BizException(ResponseCodeEnum.NOTE_NOT_LIKED);
            }
            case NOTE_NOT_LIKED -> {
                // 笔记未点赞，绝对正确
                throw new BizException(ResponseCodeEnum.NOTE_NOT_LIKED);
            }
        }

        // 3. 删除 ZSET 中已点赞的笔记 ID
        // 用户点赞列表 ZSet Key
        String userNoteLikeZSetKey = RedisKeyConstants.buildUserNoteLikeZSetKey(userId);

        redisTemplate.opsForZSet().remove(userNoteLikeZSetKey, noteId);

        // 4. 发送 MQ, 数据更新落库
        LikeUnlikeNoteMqDTO mqDTO = LikeUnlikeNoteMqDTO.builder()
            .userId(userId)
            .noteId(noteId)
            .type(LikeUnlikeNoteTypeEnum.UNLIKE.getCode())
            .createTime(LocalDateTime.now())
            .build();

        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(mqDTO)).build();

        // 通过冒号连接, 可让 MQ 发送给主题 Topic 时，携带上标签 Tag
        String destination = MQConstants.TOPIC_LIKE_OR_UNLIKE + ":" + MQConstants.TAG_UNLIKE;

        String hashKey = String.valueOf(userId);

        // 异步发送 MQ 消息，提升接口响应速度
        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【笔记取消点赞】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【笔记取消点赞】MQ 发送异常: ", throwable);
            }
        });

        return Response.success();
    }

    /**
     * 收藏笔记
     *
     * @param collectNoteReqVO
     * @return
     */
    @Override
    public Response<?> collectNote(CollectNoteReqVO collectNoteReqVO) {
        // 笔记ID
        Long noteId = collectNoteReqVO.getId();

        // 1. 校验被收藏的笔记是否存在
        checkNoteIsExist(noteId);

        //2. 判断目标笔记，是否已经收藏过
        // 当前登录用户ID
        Long userId = LoginUserContextHolder.getUserId();

        // 布隆过滤器 Key
        String bloomUserNoteCollectListKey = RedisKeyConstants.buildBloomUserNoteCollectListKey(userId);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        // Lua 脚本路径
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_note_collect_check.lua")));
        // 返回值类型
        script.setResultType(Long.class);

        // 执行 Lua 脚本，拿到返回结果
        Long result = redisTemplate.execute(script, Collections.singletonList(bloomUserNoteCollectListKey), noteId);

        NoteCollectLuaResultEnum noteCollectLuaResultEnum = NoteCollectLuaResultEnum.valueOf(result);

        String userNoteCollectZSetKey = RedisKeyConstants.buildUserNoteCollectZSetKey(userId);

        switch (noteCollectLuaResultEnum) {
            // Redis 中布隆过滤器不存在
            case NOT_EXIST -> {
                // 从数据库中校验笔记是否被收藏，并异步初始化布隆过滤器，设置过期时间
                int count = noteCollectionDOMapper.selectCountByUserIdAndNoteId(userId, noteId);

                // 保底1天+随机秒数
                long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);

                // 目标笔记已经被收藏
                if (count > 0) {
                    // 异步初始化布隆过滤器
                    threadPoolTaskExecutor.submit(() ->
                        batchAddNoteCollect2BloomAndExpire(userId, expireSeconds, bloomUserNoteCollectListKey));
                    throw new BizException(ResponseCodeEnum.NOTE_ALREADY_LIKED);
                }

                // 若目标笔记未被收藏，查询当前用户是否有收藏其他笔记，有则同步初始化布隆过滤器
                batchAddNoteCollect2BloomAndExpire(userId, expireSeconds, bloomUserNoteCollectListKey);

                // 添加当前收藏笔记 ID 到布隆过滤器中
                // Lua 脚本路径
                script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_add_note_collect_and_expire.lua")));
                // 返回值类型
                script.setResultType(Long.class);
                redisTemplate.execute(script, Collections.singletonList(bloomUserNoteCollectListKey), noteId, expireSeconds);

            }
            // 目标笔记已经被收藏 (可能存在误判，需要进一步确认)
            case NOTE_COLLECTED -> {
                // 校验 ZSet 列表中是否包含被收藏的笔记ID
                Double score = redisTemplate.opsForZSet().score(userNoteCollectZSetKey, noteId);

                if (Objects.nonNull(score)) {
                    throw new BizException(ResponseCodeEnum.NOTE_ALREADY_COLLECTED);
                }

                // 若 Score 为空，则表示 ZSet 收藏列表中不存在，查询数据库校验
                int count = noteCollectionDOMapper.selectNoteIsCollected(userId, noteId);

                if (count > 0) {
                    // 数据库里面有收藏记录，而 Redis 中 ZSet 未初始化，需要重新异步初始化 ZSet
                    asynInitUserNoteCollectsZSet(userId, userNoteCollectZSetKey);

                    throw new BizException(ResponseCodeEnum.NOTE_ALREADY_COLLECTED);
                }
            }
        }


        // 3. 更新用户 ZSET 收藏列表
        LocalDateTime now = LocalDateTime.now();
        // Lua 脚本路径
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/note_collect_check_and_update_zset.lua")));
        // 返回值类型
        script.setResultType(Long.class);

        // 执行 Lua 脚本，拿到返回结果
        result = redisTemplate.execute(script, Collections.singletonList(userNoteCollectZSetKey), noteId, DateUtils.localDateTime2Timestamp(now));

        // 若 ZSet 列表不存在，需要重新初始化
        if (Objects.equals(result, NoteCollectLuaResultEnum.NOT_EXIST.getCode())) {
            // 查询当前用户最新收藏的 300 篇笔记
            List<NoteCollectionDO> noteCollectionDOS = noteCollectionDOMapper.selectCollectedByUserIdAndLimit(userId, 300);

            if (CollUtil.isNotEmpty(noteCollectionDOS)) {
                // 保底1天+随机秒数
                long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);
                // 构建 Lua 参数
                Object[] luaArgs = buildNoteCollectZSetLuaArgs(noteCollectionDOS, expireSeconds);

                DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
                // Lua 脚本路径
                script2.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/batch_add_note_collect_zset_and_expire.lua")));
                // 返回值类型
                script2.setResultType(Long.class);

                redisTemplate.execute(script2, Collections.singletonList(userNoteCollectZSetKey), luaArgs);

                // 再次调用 note_collect_check_and_update_zset.lua 脚本，将当前收藏的笔记添加到 zset 中
                redisTemplate.execute(script, Collections.singletonList(userNoteCollectZSetKey), noteId, DateUtils.localDateTime2Timestamp(now));
            }
        }


        // TODO: 4. 发送 MQ, 将收藏数据落库

        return Response.success();
    }

    /**
     * 初始化笔记收藏布隆过滤器
     * @param userId
     * @param expireSeconds
     * @param bloomUserNoteCollectListKey
     */
    private void batchAddNoteCollect2BloomAndExpire(Long userId, long expireSeconds, String bloomUserNoteCollectListKey) {
        try {
            // 异步全量同步一下，并设置过期时间
            List<NoteCollectionDO> noteCollectionDOS = noteCollectionDOMapper.selectByUserId(userId);

            if (CollUtil.isNotEmpty(noteCollectionDOS)) {
                DefaultRedisScript<Long> script = new DefaultRedisScript<>();
                // Lua 脚本路径
                script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_batch_add_note_collect_and_expire.lua")));
                // 返回值类型
                script.setResultType(Long.class);

                // 构建 Lua 参数
                List<Object> luaArgs = Lists.newArrayList();
                noteCollectionDOS.forEach(noteCollectionDO -> luaArgs.add(noteCollectionDO.getNoteId())); // 将每个收藏的笔记 ID 传入
                luaArgs.add(expireSeconds);  // 最后一个参数是过期时间（秒）
                redisTemplate.execute(script, Collections.singletonList(bloomUserNoteCollectListKey), luaArgs.toArray());
            }
        } catch (Exception e) {
            log.error("## 异步初始化【笔记收藏】布隆过滤器异常: ", e);
        }
    }

    /**
     * 异步初始化用户收藏笔记 ZSet
     * @param userId
     * @param userNoteCollectZSetKey
     */
    private void asynInitUserNoteCollectsZSet(Long userId, String userNoteCollectZSetKey) {
        threadPoolTaskExecutor.execute(() -> {
            // 判断用户笔记收藏 ZSET 是否存在
            boolean hasKey = redisTemplate.hasKey(userNoteCollectZSetKey);

            // 不存在，则重新初始化
            if (!hasKey) {
                // 查询当前用户最新收藏的 300 篇笔记
                List<NoteCollectionDO> noteCollectionDOS = noteCollectionDOMapper.selectCollectedByUserIdAndLimit(userId, 300);
                if (CollUtil.isNotEmpty(noteCollectionDOS)) {
                    // 保底1天+随机秒数
                    long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);
                    // 构建 Lua 参数
                    Object[] luaArgs = buildNoteCollectZSetLuaArgs(noteCollectionDOS, expireSeconds);

                    DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
                    // Lua 脚本路径
                    script2.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/batch_add_note_collect_zset_and_expire.lua")));
                    // 返回值类型
                    script2.setResultType(Long.class);

                    redisTemplate.execute(script2, Collections.singletonList(userNoteCollectZSetKey), luaArgs);
                }
            }
        });
    }


    /**
     * 构建笔记收藏 ZSET Lua 脚本参数
     *
     * @param noteCollectionDOS
     * @param expireSeconds
     * @return
     */
    private static Object[] buildNoteCollectZSetLuaArgs(List<NoteCollectionDO> noteCollectionDOS, long expireSeconds) {
        int argsLength = noteCollectionDOS.size() * 2 + 1; // 每个笔记收藏关系有 2 个参数（score 和 value），最后再跟一个过期时间
        Object[] luaArgs = new Object[argsLength];

        int i = 0;
        for (NoteCollectionDO noteCollectionDO : noteCollectionDOS) {
            luaArgs[i] = DateUtils.localDateTime2Timestamp(noteCollectionDO.getCreateTime()); // 收藏时间作为 score
            luaArgs[i + 1] = noteCollectionDO.getNoteId();          // 笔记ID 作为 ZSet value
            i += 2;
        }

        luaArgs[argsLength - 1] = expireSeconds; // 最后一个参数是 ZSet 的过期时间
        return luaArgs;
    }



    private void asyncInitUserNoteLikesZSet(Long userId, String zSetKey) {
        threadPoolTaskExecutor.execute(() -> {
            // 判断用户笔记点赞 ZSET 是否存在
            boolean hasKey = redisTemplate.hasKey(zSetKey);

            // 不存在，则重新初始化
            if (!hasKey) {
                // 查询当前用户最新点赞的 100 篇笔记
                List<NoteLikeDO> noteLikeDOS = noteLikeDOMapper.selectLikedByUserIdAndLimit(userId, 100);
                if (CollUtil.isNotEmpty(noteLikeDOS)) {
                    // 保底1天+随机秒数
                    long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);
                    // 构建 Lua 参数
                    Object[] luaArgs = buildNoteLikeZSetLuaArgs(noteLikeDOS, expireSeconds);

                    DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
                    // Lua 脚本路径
                    script2.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/batch_add_note_like_zset_and_expire.lua")));
                    // 返回值类型
                    script2.setResultType(Long.class);

                    redisTemplate.execute(script2, Collections.singletonList(zSetKey), luaArgs);
                }
            }
        });
    }

    private Object[] buildNoteLikeZSetLuaArgs(List<NoteLikeDO> likeDOList, long expireSeconds) {
        Object[] luaArgs  = new Object[likeDOList.size()*2+1];
        int index = 0;
        for (NoteLikeDO noteLikeDO : likeDOList) {
            luaArgs[index] = DateUtils.localDateTime2Timestamp(noteLikeDO.getCreateTime());
            luaArgs[index+1] = noteLikeDO.getNoteId();
            index+=2;
        }
        luaArgs[index]=expireSeconds;
        return luaArgs;
    }

    /**
     * 异步初始化布隆过滤器
     * @param userId
     * @param expireSeconds
     * @param bloomUserNoteLikeListKey
     */
    private void asyncBatchAddNoteLike2BloomAndExpire(Long userId, long expireSeconds, String bloomUserNoteLikeListKey) {
        threadPoolTaskExecutor.submit(() -> {
            try {
                // 异步全量同步一下，并设置过期时间
                List<NoteLikeDO> noteLikeDOS = noteLikeDOMapper.selectByUserId(userId);

                if (CollUtil.isNotEmpty(noteLikeDOS)) {
                    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
                    // Lua 脚本路径
                    script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_batch_add_note_like_and_expire.lua")));
                    // 返回值类型
                    script.setResultType(Long.class);

                    // 构建 Lua 参数
                    List<Object> luaArgs = Lists.newArrayList();
                    noteLikeDOS.forEach(noteLikeDO -> luaArgs.add(noteLikeDO.getNoteId())); // 将每个点赞的笔记 ID 传入
                    luaArgs.add(expireSeconds);  // 最后一个参数是过期时间（秒）
                    redisTemplate.execute(script, Collections.singletonList(bloomUserNoteLikeListKey), luaArgs.toArray());
                }
            } catch (Exception e) {
                log.error("## 异步初始化布隆过滤器异常: ", e);
            }
        });
    }


    /**
     * 校验笔记是否存在
     * @param noteId
     */
    private void checkNoteIsExist(Long noteId) {
        // 先从本地缓存校验
        String findNoteDetailRspVOStrLocalCache = LOCAL_CACHE.getIfPresent(noteId);
        // 解析 Json 字符串为 VO 对象
        FindNoteDetailRspVO findNoteDetailRspVO = JsonUtils.parseObject(findNoteDetailRspVOStrLocalCache, FindNoteDetailRspVO.class);

        // 若本地缓存没有
        if (Objects.isNull(findNoteDetailRspVO)) {
            // 再从 Redis 中校验
            String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);

            String noteDetailJson = redisTemplate.opsForValue().get(noteDetailRedisKey);

            // 解析 Json 字符串为 VO 对象
            findNoteDetailRspVO = JsonUtils.parseObject(noteDetailJson, FindNoteDetailRspVO.class);

            // 都不存在，再查询数据库校验是否存在
            if (Objects.isNull(findNoteDetailRspVO)) {
                int count = noteDOMapper.selectCountByNoteId(noteId);

                // 若数据库中也不存在，提示用户
                if (count == 0) {
                    throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
                }

                // 若数据库中存在，异步同步一下缓存
                threadPoolTaskExecutor.submit(() -> {
                    FindNoteDetailReqVO findNoteDetailReqVO = FindNoteDetailReqVO.builder().id(noteId).build();
                    findNoteDetail(findNoteDetailReqVO);
                });
            }
        }
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
