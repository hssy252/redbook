package com.hssy.xiaohongshu.note.biz.model.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 13759
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindNoteDetailRspVO {

    private Long id;

    private Integer type;

    private String title;

    private String content;

    private List<String> imgUris;

    private Long topicId;

    private String topicName;

    private Long creatorId;

    private String creatorName;

    private String avatar;

    private String videoUri;

    /**
     * 编辑时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 是否可见
     */
    private Integer visible;

}