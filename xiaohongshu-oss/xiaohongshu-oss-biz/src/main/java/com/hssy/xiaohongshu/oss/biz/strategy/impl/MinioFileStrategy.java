package com.hssy.xiaohongshu.oss.biz.strategy.impl;

import com.hssy.xiaohongshu.oss.biz.strategy.FileStrategy;
import com.hssy.xiaohongshu.oss.biz.utils.MinioUtil;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
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

    @Resource
    private MinioUtil minioUtil;

    @Override
    @SneakyThrows
    public String uploadFile(MultipartFile file) {
        log.info("======== Minio上传文件");
        return minioUtil.uploadFile(file);
    }
}
