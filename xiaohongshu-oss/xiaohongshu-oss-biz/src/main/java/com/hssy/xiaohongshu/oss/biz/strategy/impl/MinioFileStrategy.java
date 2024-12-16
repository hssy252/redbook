package com.hssy.xiaohongshu.oss.biz.strategy.impl;

import com.hssy.xiaohongshu.oss.biz.strategy.FileStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/16 17:25
 */
@Slf4j
public class MinioFileStrategy implements FileStrategy {

    @Override
    public String uploadFile(MultipartFile file, String bucketName) {
        log.info("======== Minio上传文件");
        return null;
    }
}
