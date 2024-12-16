package com.hssy.xiaohongshu.oss.biz.service.impl;

import com.hssy.framework.commom.response.Response;
import com.hssy.xiaohongshu.oss.biz.service.FileService;
import com.hssy.xiaohongshu.oss.biz.strategy.FileStrategy;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/16 17:32
 */
@Service
public class FileServiceImpl implements FileService {


    @Resource
    private FileStrategy fileStrategy;

    @Override
    public Response<?> uploadFile(MultipartFile file) {
        // 上传文件到
        String url = fileStrategy.uploadFile(file);

        return Response.success(url);
    }
}
