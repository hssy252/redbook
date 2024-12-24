package com.hssy.xiaohongshu.user.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.nacos.shaded.com.google.common.collect.Maps;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.hssy.framework.biz.context.holder.LoginUserContextHolder;
import com.hssy.framework.commom.enums.DeletedEnum;
import com.hssy.framework.commom.enums.StatusEnum;
import com.hssy.framework.commom.exception.BizException;
import com.hssy.framework.commom.response.Response;
import com.hssy.framework.commom.util.JsonUtils;
import com.hssy.framework.commom.util.ParamUtils;
import com.hssy.xiaohongshu.oss.api.FileFeignApi;
import com.hssy.xiaohongshu.user.api.dto.req.FindUserByIdReqDTO;
import com.hssy.xiaohongshu.user.api.dto.req.FindUserByPhoneReqDTO;
import com.hssy.xiaohongshu.user.api.dto.req.FindUsersByIdsReqDTO;
import com.hssy.xiaohongshu.user.api.dto.req.RegisterUserReqDTO;
import com.hssy.xiaohongshu.user.api.dto.req.UpdateUserPasswordReqDTO;
import com.hssy.xiaohongshu.user.api.dto.req.UserExistReqDTO;
import com.hssy.xiaohongshu.user.api.dto.resp.FindUserByIdRspDTO;
import com.hssy.xiaohongshu.user.api.dto.resp.FindUserByPhoneRspDTO;
import com.hssy.xiaohongshu.user.biz.constant.RedisKeyConstants;
import com.hssy.xiaohongshu.user.biz.constant.RoleConstants;
import com.hssy.xiaohongshu.user.biz.domain.dataobject.RoleDO;
import com.hssy.xiaohongshu.user.biz.domain.dataobject.UserDO;
import com.hssy.xiaohongshu.user.biz.domain.dataobject.UserRoleDO;
import com.hssy.xiaohongshu.user.biz.domain.mapper.RoleDOMapper;
import com.hssy.xiaohongshu.user.biz.domain.mapper.UserDOMapper;
import com.hssy.xiaohongshu.user.biz.domain.mapper.UserRoleDOMapper;
import com.hssy.xiaohongshu.user.biz.enums.ResponseCodeEnum;
import com.hssy.xiaohongshu.user.biz.enums.SexEnum;
import com.hssy.xiaohongshu.user.biz.model.vo.UpdateUserInfoReqVO;
import com.hssy.xiaohongshu.user.biz.rpc.DistributedIdGeneratorRpcService;
import com.hssy.xiaohongshu.user.biz.rpc.OssRpcService;
import com.hssy.xiaohongshu.user.biz.service.UserService;
import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.boot.autoconfigure.cache.CacheProperties.Redis;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/16 21:53
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Resource
    private UserDOMapper userDOMapper;

    @Resource
    private OssRpcService ossRpcService;

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    @Resource
    private UserRoleDOMapper userRoleDOMapper;

    @Resource
    private RoleDOMapper roleDOMapper;

    @Resource
    private DistributedIdGeneratorRpcService distributedIdGeneratorRpcService;

    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    /**
     * 用户信息本地缓存
     */
    private static final Cache<Long, FindUserByIdRspDTO> LOCAL_CACHE = Caffeine.newBuilder()
        .initialCapacity(10000) // 设置初始容量为 10000 个条目
        .maximumSize(10000) // 设置缓存的最大容量为 10000 个条目
        .expireAfterWrite(1, TimeUnit.HOURS) // 设置缓存条目在写入后 1 小时过期
        .build();


    /**
     * 更新用户信息
     *
     * @param updateUserInfoReqVO
     * @return
     */
    @Override
    public Response<?> updateUserInfo(UpdateUserInfoReqVO updateUserInfoReqVO) {
        UserDO userDO = new UserDO();
        // 设置当前需要更新的用户 ID
        userDO.setId(LoginUserContextHolder.getUserId());
        // 标识位：是否需要更新
        boolean needUpdate = false;

        // 头像
        MultipartFile avatarFile = updateUserInfoReqVO.getAvatar();

        if (Objects.nonNull(avatarFile)) {
            // 调用对象存储服务上传文件
            String response = ossRpcService.uploadFile(avatarFile);
            log.info("==> 调用 oss 服务成功，上传头像，url：{}", response);
            if(Objects.isNull(response)){
                throw new BizException(ResponseCodeEnum.UPLOAD_AVATAR_FAIL);
            }
            userDO.setAvatar(response);
            needUpdate = true;
        }

        // 昵称
        String nickname = updateUserInfoReqVO.getNickname();
        if (StringUtils.isNotBlank(nickname)) {
            Preconditions.checkArgument(ParamUtils.checkNickname(nickname), ResponseCodeEnum.NICK_NAME_VALID_FAIL.getErrorMessage());
            userDO.setNickname(nickname);
            needUpdate = true;
        }

        // 小哈书号
        String xiaohongshuId = updateUserInfoReqVO.getXiaohongshuId();
        if (StringUtils.isNotBlank(xiaohongshuId)) {
            Preconditions.checkArgument(ParamUtils.checkXiaohongshuId(xiaohongshuId), ResponseCodeEnum.XIAOHONGSHU_ID_VALID_FAIL.getErrorMessage());
            userDO.setXiaohongshuId(xiaohongshuId);
            needUpdate = true;
        }

        // 性别
        Integer sex = updateUserInfoReqVO.getSex();
        if (Objects.nonNull(sex)) {
            Preconditions.checkArgument(SexEnum.isValid(sex), ResponseCodeEnum.SEX_VALID_FAIL.getErrorMessage());
            userDO.setSex(sex);
            needUpdate = true;
        }

        // 生日
        LocalDate birthday = updateUserInfoReqVO.getBirthday();
        if (Objects.nonNull(birthday)) {
            userDO.setBirthday(birthday);
            needUpdate = true;
        }

        // 个人简介
        String introduction = updateUserInfoReqVO.getIntroduction();
        if (StringUtils.isNotBlank(introduction)) {
            Preconditions.checkArgument(ParamUtils.checkLength(introduction, 100), ResponseCodeEnum.INTRODUCTION_VALID_FAIL.getErrorMessage());
            userDO.setIntroduction(introduction);
            needUpdate = true;
        }

        // 背景图
        MultipartFile backgroundImgFile = updateUserInfoReqVO.getBackgroundImg();
        if (Objects.nonNull(backgroundImgFile)) {
            // 调用对象存储服务上传文件
            String response = ossRpcService.uploadFile(backgroundImgFile);
            log.info("==> 调用 oss 服务成功，上传背景图，url：{}", response);
            if (Objects.isNull(response)){
                throw new BizException(ResponseCodeEnum.UPLOAD_BACKGROUND_IMG_FAIL);
            }
            userDO.setBackgroundImg(response);
            needUpdate = true;
        }

        if (needUpdate) {
            // 更新用户信息
            userDO.setUpdateTime(LocalDateTime.now());
            userDOMapper.updateByPrimaryKeySelective(userDO);
        }
        return Response.success();
    }

    /**
     * 用户注册
     *
     * @param registerUserReqDTO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Long> register(RegisterUserReqDTO registerUserReqDTO) {
        String phone = registerUserReqDTO.getPhone();

        // 先判断该手机号是否已被注册
        UserDO userDO1 = userDOMapper.selectByPhone(phone);

        log.info("==> 用户是否注册, phone: {}, userDO: {}", phone, JsonUtils.toJsonString(userDO1));

        // 若已注册，则直接返回用户 ID
        if (Objects.nonNull(userDO1)) {
            return Response.success(userDO1.getId());
        }

        // 否则注册新用户
        // 获取全局自增的小哈书 ID
        // Long xiaohongshuId = redisTemplate.opsForValue().increment(RedisKeyConstants.XIAOHONGSHU_ID_GENERATOR_KEY);

        Long xiaohongshuId = Long.valueOf(distributedIdGeneratorRpcService.getXiaohongshuId());
        Long userId = Long.valueOf(distributedIdGeneratorRpcService.getUserId());

        UserDO userDO = UserDO.builder()
            .id(userId)
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
//        Long userId = userDO.getId();

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

        // 将该用户的角色 ID 存入 Redis 中
        List<String> roles = new ArrayList<>(1);
        roles.add(roleDO.getRoleKey());

        String userRolesKey = RedisKeyConstants.buildUserRoleKey(userId);
        redisTemplate.opsForValue().set(userRolesKey, JsonUtils.toJsonString(roles));

        return Response.success(userId);
    }

    /**
     * 根据手机号查询用户信息
     *
     * @param findUserByPhoneReqDTO
     * @return
     */
    @Override
    public Response<FindUserByPhoneRspDTO> findByPhone(FindUserByPhoneReqDTO findUserByPhoneReqDTO) {
        String phone = findUserByPhoneReqDTO.getPhone();

        // 根据手机号查询用户信息
        UserDO userDO = userDOMapper.selectByPhone(phone);

        // 判空
        if (Objects.isNull(userDO)) {
            throw new BizException(ResponseCodeEnum.USER_NOT_FOUND);
        }

        // 构建返参
        FindUserByPhoneRspDTO findUserByPhoneRspDTO = FindUserByPhoneRspDTO.builder()
            .id(userDO.getId())
            .password(userDO.getPassword())
            .build();

        return Response.success(findUserByPhoneRspDTO);
    }

    /**
     * 更新密码
     *
     * @param updateUserPasswordReqDTO
     * @return
     */
    @Override
    public Response<?> updatePassword(UpdateUserPasswordReqDTO updateUserPasswordReqDTO) {
        // 获取当前请求对应的用户 ID
        Long userId = LoginUserContextHolder.getUserId();

        UserDO userDO = UserDO.builder()
            .id(userId)
            .password(updateUserPasswordReqDTO.getEncodePassword()) // 加密后的密码
            .updateTime(LocalDateTime.now())
            .build();
        // 更新密码
        userDOMapper.updateByPrimaryKeySelective(userDO);

        return Response.success();
    }

    @Override
    public Response<FindUserByIdRspDTO> findById(FindUserByIdReqDTO findUserByIdReqDTO) {
        Long id = findUserByIdReqDTO.getId();

        // 先从本地缓存中查询
        FindUserByIdRspDTO findUserByIdRspDTOLocalCache = LOCAL_CACHE.getIfPresent(id);
        if (Objects.nonNull(findUserByIdRspDTOLocalCache)) {
            log.info("==> 命中了本地缓存；{}", findUserByIdRspDTOLocalCache);
            return Response.success(findUserByIdRspDTOLocalCache);
        }


        // 再去去redis缓存查找
        String userInfoKey = RedisKeyConstants.buildUserInfoKey(id);

        String userInfoString = (String) redisTemplate.opsForValue().get(userInfoKey);

        if (StringUtils.isNotBlank(userInfoString)){
            if (StringUtils.equals("null",userInfoString)) {
                return null;
            }
            FindUserByIdRspDTO findUserByIdRspDTO = JsonUtils.parseObject(userInfoString, FindUserByIdRspDTO.class);
            // 异步线程中将用户信息存入本地缓存
            taskExecutor.submit(() -> {
                    // 写入本地缓存
                    LOCAL_CACHE.put(id, findUserByIdRspDTO);
            });
            return Response.success(findUserByIdRspDTO);
        }

        // redis没有就走数据库
        // 根据用户 ID 查询用户信息
        UserDO userDO = userDOMapper.selectByPrimaryKey(id);

        // 判空
        if (Objects.isNull(userDO)) {
            throw new BizException(ResponseCodeEnum.USER_NOT_FOUND);
        }

        // 构建返参
        FindUserByIdRspDTO findUserByIdRspDTO = FindUserByIdRspDTO.builder()
            .id(userDO.getId())
            .nickName(userDO.getNickname())
            .avatar(userDO.getAvatar())
            .introduction(userDO.getIntroduction())
            .build();

        if (Objects.isNull(findUserByIdRspDTO)){
            // 如果为空，就要做缓存穿透的逻辑
            taskExecutor.execute(()->{
                // 防止缓存穿透，将空数据存入 Redis 缓存 (过期时间不宜设置过长)
                // 保底1分钟 + 随机秒数
                long expireSeconds = 60 + RandomUtil.randomInt(60);
                redisTemplate.opsForValue().set(userInfoKey, "null", expireSeconds, TimeUnit.SECONDS);
            });
            throw new BizException(ResponseCodeEnum.USER_NOT_FOUND);
        }

        // 非空就要存入redis里
        // 异步将用户信息存入 Redis 缓存，提升响应速度
        taskExecutor.submit(() -> {
            // 过期时间（保底1天 + 随机秒数，将缓存过期时间打散，防止同一时间大量缓存失效，导致数据库压力太大）
            long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);
            redisTemplate.opsForValue()
                .set(userInfoKey, JsonUtils.toJsonString(findUserByIdRspDTO), expireSeconds, TimeUnit.SECONDS);
        });

        return Response.success(findUserByIdRspDTO);
    }

    @Override
    public Response<Boolean> userExistOrNot(UserExistReqDTO userExistReqDTO) {
        Long userId = userExistReqDTO.getUserId();
        UserDO userDO = userDOMapper.selectByPrimaryKey(userId);
        if (Objects.isNull(userDO)){
            return Response.success(Boolean.FALSE);
        }

        return Response.success(Boolean.TRUE);
    }

    @Override
    public Response<List<FindUserByIdRspDTO>> findByIds(FindUsersByIdsReqDTO findUsersByIdsReqDTO) {
        // 先查缓存，看缓存里有没有
        List<Long> userIds = findUsersByIdsReqDTO.getIds();
        // 构建redis的key
        List<String> keys = userIds.stream().map(RedisKeyConstants::buildUserInfoKey).toList();

        // 获取缓存中的用户信息
        List<Object> redisValues = redisTemplate.opsForValue().multiGet(keys);

        // 这里先处理缓存不为空但可能不全的逻辑，因为无论是缓存为空还是缓存不全，最后都要查数据库
        if (CollUtil.isNotEmpty(redisValues)){
            // 如果缓存不为空，过滤缓存中为用户值为空的信息
            redisValues = redisValues.stream().filter(Objects::nonNull).toList();
            
        }

        // 返参
        List<FindUserByIdRspDTO> findUserByIdRspDTOS = Lists.newArrayList();

        // 再次判断过滤后是否为空，若不为空则开始转化实体类
        if (CollUtil.isNotEmpty(redisValues)){
            findUserByIdRspDTOS = redisValues.stream().map(
                value->JsonUtils.parseObject((String) value,FindUserByIdRspDTO.class)
            ).toList();
        }

        // 如果被查询的用户信息，都在 Redis 缓存中, 则直接返回
        if (CollUtil.size(userIds) == CollUtil.size(findUserByIdRspDTOS)) {
            return Response.success(findUserByIdRspDTOS);
        }


        // 还有另外两种情况：一种是缓存里没有用户信息数据，还有一种是缓存里数据不全，需要从数据库中补充
        // 筛选出缓存里没有的用户数据，去查数据库
        List<Long> userIdsNeedQuery = null;

        // 判断哪些需要去数据库查
        if (CollUtil.isNotEmpty(findUserByIdRspDTOS)){
            Map<Long, FindUserByIdRspDTO> findUserByIdRspDTOMap = findUserByIdRspDTOS.stream().collect(Collectors.toMap(
                FindUserByIdRspDTO::getId, findUserByIdRspDTO -> findUserByIdRspDTO
            ));

            // 如果在map中没查到就是要去数据库中查
            userIdsNeedQuery = userIds.stream().filter(id->Objects.nonNull(findUserByIdRspDTOMap.get(id))).toList();
        }

        if (Objects.isNull(userIdsNeedQuery)){
            userIdsNeedQuery = userIds;
        }

        List<UserDO> userDOS = userDOMapper.selectByIds(userIdsNeedQuery);

        List<FindUserByIdRspDTO> list = null;
        if (CollUtil.isNotEmpty(userDOS)) {
            list = userDOS.stream().map(userDO -> FindUserByIdRspDTO.builder()
                .id(userDO.getId())
                .nickName(userDO.getNickname())
                .avatar(userDO.getAvatar())
                .introduction(userDO.getIntroduction())
                .build()).toList();

            // 将数据库中查到的数据异步存入redis中
            List<FindUserByIdRspDTO> finalFindUserByIdRspDTOs = list;
            taskExecutor.submit(()->{
                // 执行pipeline操作
                redisTemplate.executePipelined((RedisCallback<Void>) connection -> {
                    finalFindUserByIdRspDTOs.forEach(dto->{
                        Long id = dto.getId();

                        // 构建redis的缓存key和value
                        String key = RedisKeyConstants.buildUserInfoKey(id);
                        String jsonStr = JsonUtils.toJsonString(dto);

                        // 设置随机缓存过期时间
                        long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);
                        redisTemplate.opsForValue().set(key,jsonStr,expireSeconds,TimeUnit.SECONDS);
                    });
                return null;
                });
            });
        }

        // 汇总结果
        if (CollUtil.isNotEmpty(list)) {
            findUserByIdRspDTOS.addAll(list);
        }

        return Response.success(findUserByIdRspDTOS);

    }

}
