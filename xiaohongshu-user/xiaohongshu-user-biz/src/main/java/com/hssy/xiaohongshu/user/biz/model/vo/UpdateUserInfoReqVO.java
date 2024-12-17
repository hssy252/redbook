package com.hssy.xiaohongshu.user.biz.model.vo;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author 13759
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateUserInfoReqVO {

    /**
     * 头像
     */
    private MultipartFile avatar;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 小哈书 ID
     */
    private String xiaohongshuId;

    /**
     * 性别
     */
    private Integer sex;

    /**
     * 生日
     */
    private LocalDate birthday;

    /**
     * 个人介绍
     */
    private String introduction;

    /**
     * 背景图
     */
    private MultipartFile backgroundImg;

}