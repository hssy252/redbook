package com.hssy.xiaohongshu.note.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.google.common.base.Preconditions;
import com.hssy.framework.biz.context.holder.LoginUserContextHolder;
import com.hssy.framework.commom.exception.BizException;
import com.hssy.framework.commom.response.Response;
import com.hssy.xiaohongshu.kv.dto.req.AddNoteContentReqDTO;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
        // 根据id查询笔记
        Long id = findNoteDetailReqVO.getId();
        NoteDO noteDO = noteDOMapper.selectByPrimaryKey(id);
        if (Objects.isNull(noteDO)){
            throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }

        // 查询用户信息
        Long userId = noteDO.getCreatorId();

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
}
