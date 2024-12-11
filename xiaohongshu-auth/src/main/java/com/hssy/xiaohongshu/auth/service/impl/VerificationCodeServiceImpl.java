package com.hssy.xiaohongshu.auth.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.hssy.framework.commom.response.Response;
import com.hssy.xiaohongshu.auth.constant.RedisKeyConstants;
import com.hssy.xiaohongshu.auth.enums.ResponseCodeEnum;
import com.hssy.xiaohongshu.auth.model.vo.verificationcode.SendVerificationCodeReqVO;
import com.hssy.xiaohongshu.auth.service.VerificationCodeService;
import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 功能简述 实现短信验证码功能
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/11 16:44
 */
@Service
@Slf4j
public class VerificationCodeServiceImpl implements VerificationCodeService {

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    /**
     * 发送短信验证码
     * @param sendVerificationCodeReqVO
     * @return
     */
    @Override
    public Response<?> send(SendVerificationCodeReqVO sendVerificationCodeReqVO) {
        //获取手机号
        String phone = sendVerificationCodeReqVO.getPhone();
        String key = RedisKeyConstants.buildVerificationCodeKey(phone);
        if(Boolean.TRUE.equals(redisTemplate.hasKey(key))){
            return Response.fail(ResponseCodeEnum.VERIFICATION_CODE_SEND_FREQUENTLY);
        }
        // 生成 6 位随机数字验证码
        String verificationCode = RandomUtil.randomNumbers(6);

        // 调用工具类发送验证码


        log.info("==> 手机号: {}, 已发送验证码：【{}】", phone, verificationCode);

        // 存储验证码到 redis, 并设置过期时间为 3 分钟
        redisTemplate.opsForValue().set(key, verificationCode, 3, TimeUnit.MINUTES);

        return Response.success();
    }
}
