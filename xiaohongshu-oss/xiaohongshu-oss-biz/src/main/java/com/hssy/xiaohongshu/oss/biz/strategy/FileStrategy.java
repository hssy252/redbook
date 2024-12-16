package com.hssy.xiaohongshu.oss.biz.strategy;

import org.springframework.web.multipart.MultipartFile;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/16 17:24
 */
public interface FileStrategy {

    /**
     * 文件上传
     * @param file
     * @param bucketName
     * @return
     */
    String uploadFile(MultipartFile file);

}
