package com.hssy.xiaohongshu.auth.service.impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.hssy.framework.commom.enums.DeletedEnum;
import com.hssy.framework.commom.enums.StatusEnum;
import com.hssy.framework.commom.exception.BizException;
import com.hssy.framework.commom.response.Response;
import com.hssy.framework.commom.util.JsonUtils;
import com.hssy.xiaohongshu.auth.constant.RedisKeyConstants;
import com.hssy.xiaohongshu.auth.constant.RoleConstants;
import com.hssy.xiaohongshu.auth.domain.dataobject.RoleDO;
import com.hssy.xiaohongshu.auth.domain.dataobject.UserDO;
import com.hssy.xiaohongshu.auth.domain.dataobject.UserRoleDO;
import com.hssy.xiaohongshu.auth.domain.mapper.RoleDOMapper;
import com.hssy.xiaohongshu.auth.domain.mapper.UserDOMapper;
import com.hssy.xiaohongshu.auth.domain.mapper.UserRoleDOMapper;
import com.hssy.xiaohongshu.auth.enums.LoginTypeEnum;
import com.hssy.xiaohongshu.auth.enums.ResponseCodeEnum;
import com.hssy.xiaohongshu.auth.filter.LoginUserContextHolder;
import com.hssy.xiaohongshu.auth.model.vo.user.UpdatePasswordReqVO;
import com.hssy.xiaohongshu.auth.model.vo.user.UserLoginReqVO;
import com.hssy.xiaohongshu.auth.service.UserService;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/11 21:07
 */

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private UserDOMapper userDOMapper;

    @Resource
    private UserRoleDOMapper userRoleDOMapper;

    @Resource
    private RoleDOMapper roleDOMapper;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    public Response<String> loginAndRegister(UserLoginReqVO userLoginReqVO) {
        String phone = userLoginReqVO.getPhone();
        Integer type = userLoginReqVO.getType();

        LoginTypeEnum loginTypeEnum = LoginTypeEnum.valueOf(type);

        Long userId = null;
        // 判断登录类型
        switch (loginTypeEnum) {
            case VERIFICATION_CODE: // 验证码登录
                String verificationCode = userLoginReqVO.getCode();

                // 校验入参验证码是否为空
                Preconditions.checkArgument(StringUtils.isNotBlank(verificationCode), "验证码不能为空");

                // 构建验证码 Redis Key
                String key = RedisKeyConstants.buildVerificationCodeKey(phone);
                // 查询存储在 Redis 中该用户的登录验证码
                String sentCode = (String) redisTemplate.opsForValue().get(key);

                // 判断用户提交的验证码，与 Redis 中的验证码是否一致
                if (!StringUtils.equals(verificationCode, sentCode)) {
                    throw new BizException(ResponseCodeEnum.VERIFICATION_CODE_ERROR);
                }

                // 通过手机号查询记录
                UserDO userDO = userDOMapper.selectByPhone(phone);

                log.info("==> 用户是否注册, phone: {}, userDO: {}", phone, JsonUtils.toJsonString(userDO));

                // 判断是否注册
                if (Objects.isNull(userDO)) {
                    // 若此用户还没有注册，系统自动注册该用户
                    userId = registerUser(phone);

                } else {
                    // 已注册，则获取其用户 ID
                    userId = userDO.getId();
                }
                break;
            case PASSWORD: // 密码登录
                // todo

                break;
            default:
                break;
        }

        // SaToken 登录用户, 入参为用户 ID
        StpUtil.login(userId);

        // 获取 Token 令牌
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

        // 返回 Token 令牌
        return Response.success(tokenInfo.tokenValue);

    }

    /**
     * 用户退出登录逻辑
     * @param userId
     * @return
     */
    @Override
    public Response<?> logout(Long userId) {
        // 退出登录（指定用户id）
        StpUtil.logout(userId);

        return Response.success();
    }

    @Override
    public Response<?> updatePassword(UpdatePasswordReqVO updatePasswordReqVO) {
        // 获取新密码
        String newPassword = updatePasswordReqVO.getNewPassword();

        String encodedPassword = passwordEncoder.encode(newPassword);

        Long userId = LoginUserContextHolder.getUserId();

        UserDO userDO = UserDO.builder()
            .id(userId)
            .password(encodedPassword)
            .updateTime(LocalDateTime.now())
            .build();

        userDOMapper.updateByPrimaryKeySelective(userDO);

        return Response.success();
    }

    /**
     * 系统自动注册用户，使用编程式事务
     * @param phone
     * @return
     */
    public Long registerUser(String phone) {
        return transactionTemplate.execute(status -> {
            try {
                // 获取全局自增的小红书 ID
                Long xiaohongshuId = redisTemplate.opsForValue().increment(RedisKeyConstants.XIAOHONGSHU_ID_GENERATOR_KEY);

                UserDO userDO = UserDO.builder()
                    .phone(phone)
                    .xiaohongshuId(String.valueOf(xiaohongshuId)) // 自动生成小红书号 ID
                    .nickname("小红薯" + xiaohongshuId) // 自动生成昵称, 如：小红薯10000
                    .status(StatusEnum.ENABLE.getValue()) // 状态为启用
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .isDeleted(DeletedEnum.NO.getValue()) // 逻辑删除
                    .build();

                // 添加入库
                userDOMapper.insert(userDO);
                
                // 获取刚刚添加入库的用户 ID
                Long userId = userDO.getId();

                // 给该用户分配一个默认角色
                UserRoleDO userRoleDO = UserRoleDO.builder()
                    .userId(userId)
                    .roleId(RoleConstants.COMMON_USER_ROLE_ID)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .isDeleted(DeletedEnum.NO.getValue())
                    .build();
                userRoleDOMapper.insert(userRoleDO);

                RoleDO roleDO = roleDOMapper.selectByPrimaryKey(RoleConstants.COMMON_USER_ROLE_ID);

                // 将该用户的角色key 存入 Redis 中，指定初始容量为 1，这样可以减少在扩容时的性能开销
                List<String> roles = new ArrayList<>(1);
                roles.add(roleDO.getRoleKey());

                String userRolesKey = RedisKeyConstants.buildUserRoleKey(userId);
                redisTemplate.opsForValue().set(userRolesKey, JsonUtils.toJsonString(roles));

                return userId;
            } catch (Exception e) {
                status.setRollbackOnly(); // 标记事务为回滚
                log.error("==> 系统注册用户异常: ", e);
                return null;
            }
        });
    }

}
