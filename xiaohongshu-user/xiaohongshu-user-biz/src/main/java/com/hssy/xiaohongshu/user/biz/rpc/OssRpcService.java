package com.hssy.xiaohongshu.user.biz.rpc;

import com.hssy.framework.commom.response.Response;
import com.hssy.xiaohongshu.oss.api.FileFeignApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/17 18:24
 */

@Component
public class OssRpcService {

    @Resource
    private FileFeignApi fileFeignApi;

    public String uploadFile(MultipartFile file){
        Response<?> response = fileFeignApi.uploadFile(file);

        if(!response.isSuccess()){
            return null;
        }
        return (String) response.getData();
    }

}
