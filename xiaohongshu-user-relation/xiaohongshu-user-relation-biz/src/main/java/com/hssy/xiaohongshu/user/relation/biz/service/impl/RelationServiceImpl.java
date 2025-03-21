package com.hssy.xiaohongshu.user.relation.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.hssy.framework.biz.context.holder.LoginUserContextHolder;
import com.hssy.framework.commom.exception.BizException;
import com.hssy.framework.commom.response.PageResponse;
import com.hssy.framework.commom.response.Response;
import com.hssy.framework.commom.util.DateUtils;
import com.hssy.framework.commom.util.JsonUtils;
import com.hssy.xiaohongshu.user.api.api.UserFeignApi;
import com.hssy.xiaohongshu.user.api.dto.resp.FindUserByIdRspDTO;
import com.hssy.xiaohongshu.user.relation.biz.constants.FollowConstants;
import com.hssy.xiaohongshu.user.relation.biz.constants.MQConstants;
import com.hssy.xiaohongshu.user.relation.biz.constants.RedisKeyConstants;
import com.hssy.xiaohongshu.user.relation.biz.domain.dataobject.FansDO;
import com.hssy.xiaohongshu.user.relation.biz.domain.dataobject.FollowingDO;
import com.hssy.xiaohongshu.user.relation.biz.domain.mapper.FansDOMapper;
import com.hssy.xiaohongshu.user.relation.biz.domain.mapper.FollowingDOMapper;
import com.hssy.xiaohongshu.user.relation.biz.enums.LuaResultEnum;
import com.hssy.xiaohongshu.user.relation.biz.enums.ResponseCodeEnum;
import com.hssy.xiaohongshu.user.relation.biz.model.dto.FollowUserMqDTO;
import com.hssy.xiaohongshu.user.relation.biz.model.dto.UnfollowUserMqDTO;
import com.hssy.xiaohongshu.user.relation.biz.model.vo.FindFansUserReqVO;
import com.hssy.xiaohongshu.user.relation.biz.model.vo.FindFansUserRspVO;
import com.hssy.xiaohongshu.user.relation.biz.model.vo.FindFollowingListReqVO;
import com.hssy.xiaohongshu.user.relation.biz.model.vo.FindFollowingUserRspVO;
import com.hssy.xiaohongshu.user.relation.biz.model.vo.FollowUserReqVO;
import com.hssy.xiaohongshu.user.relation.biz.model.vo.UnfollowUserReqVO;
import com.hssy.xiaohongshu.user.relation.biz.rpc.UserRpcService;
import com.hssy.xiaohongshu.user.relation.biz.service.RelationService;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/22 18:55
 */
@Service
@Slf4j
public class RelationServiceImpl implements RelationService {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    @Resource
    private UserRpcService userRpcService;

    @Resource
    private FollowingDOMapper followingDOMapper;

    @Resource
    private FansDOMapper fansDOMapper;

    @Resource(name = "taskExecutor")
    private Executor taskExecutor;

    @Override
    public Response<?> followUser(FollowUserReqVO followUserReqVO) {
        // 判断被关注用户的id是不是自己
        Long userId = LoginUserContextHolder.getUserId();
        if (Objects.isNull(userId)){
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR);
        }
        Long followUserId = followUserReqVO.getFollowUserId();
        if (Objects.equals(userId,followUserId)){
            throw new BizException(ResponseCodeEnum.CANT_FOLLOW_YOURSELF);
        }

        // 判断用户是否存在
        Boolean existOrNot = userRpcService.userExistOrNot(userId);
        if (!Boolean.TRUE.equals(existOrNot)){
            throw new BizException(ResponseCodeEnum.FOLLOW_USER_NOT_EXIST);
        }

        // 用户关注数量是否达到上限,查询redis里的zset结构
        String followKey = RedisKeyConstants.buildFollowingUserKey(userId);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        // Lua 脚本路径
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_check_and_add.lua")));
        // 返回值类型
        script.setResultType(Long.class);

        // 获取当前时间戳
        LocalDateTime now = LocalDateTime.now();
        long timestamp = DateUtils.localDateTime2Timestamp(now);

        Long result = redisTemplate.execute(script, Collections.singletonList(followKey), followUserId, timestamp);

        // 校验 Lua 脚本执行结果
        checkLuaScriptResult(result);

        // ZSET 不存在
        if (Objects.equals(result, LuaResultEnum.ZSET_NOT_EXIST.getCode())) {
            // 从数据库查询当前用户的关注关系记录
            List<FollowingDO> followingDOS = followingDOMapper.selectByUserId(userId);

            // 随机过期时间
            // 保底1天+随机秒数
            long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);

            // 若记录为空，直接 ZADD 对象, 并设置过期时间
            if (CollUtil.isEmpty(followingDOS)) {
                DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
                script2.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_add_and_expire.lua")));
                script2.setResultType(Long.class);

                // TODO: 可以根据用户类型，设置不同的过期时间，若当前用户为大V, 则可以过期时间设置的长些或者不设置过期时间；如不是，则设置的短些
                // 如何判断呢？可以从计数服务获取用户的粉丝数，目前计数服务还没创建，则暂时采用统一的过期策略
                redisTemplate.execute(script2, Collections.singletonList(followKey), followUserId, timestamp, expireSeconds);
            } else { // 若记录不为空，则将关注关系数据全量同步到 Redis 中，并设置过期时间；
                // 构建 Lua 参数
                Object[] luaArgs = buildLuaArgs(followingDOS, expireSeconds);

                // 执行 Lua 脚本，批量同步关注关系数据到 Redis 中
                DefaultRedisScript<Long> script3 = new DefaultRedisScript<>();
                script3.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_batch_add_and_expire.lua")));
                script3.setResultType(Long.class);
                redisTemplate.execute(script3, Collections.singletonList(followKey), luaArgs);

                // 再次调用上面的 Lua 脚本：follow_check_and_add.lua , 将最新的关注关系添加进去
                result = redisTemplate.execute(script, Collections.singletonList(followKey), followUserId, timestamp);
                checkLuaScriptResult(result);
            }
        }

        // 然后通过MQ发消息，实现数据库异步入库（减轻数据库压力，削峰填谷
        FollowUserMqDTO mqDTO = FollowUserMqDTO.builder()
            .userId(userId)
            .followUserId(followUserId)
            .createTime(now)
            .build();

        // 构建消息对象
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(mqDTO)).build();

        // 构建Topic和Tag
        String destination = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW + ":" + MQConstants.TAG_FOLLOW;

        log.info("开始发送消息实体: {}",mqDTO);

        // 顺序key
        String hashKey = String.valueOf(userId);

        // 异步发送MQ消息
        rocketMQTemplate.asyncSendOrderly(destination, message,hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("MQ消息发送成功，发送结果：{}",sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("MQ消息发送失败，发送结果：",throwable);
            }
        });

        return Response.success();
    }

    /**
     * 取关用户
     *
     * @param unfollowUserReqVO
     * @return
     */
    @Override
    public Response<?> unfollow(UnfollowUserReqVO unfollowUserReqVO) {
        // 想要取关了用户 ID
        Long unfollowUserId = unfollowUserReqVO.getUnfollowUserId();
        // 当前登录用户 ID
        Long userId = LoginUserContextHolder.getUserId();

        // 无法取关自己
        if (Objects.equals(userId, unfollowUserId)) {
            throw new BizException(ResponseCodeEnum.CANT_UNFOLLOW_YOUR_SELF);
        }

        // 校验关注的用户是否存在
        Boolean existOrNot = userRpcService.userExistOrNot(unfollowUserId);

        if (Objects.isNull(existOrNot)||Boolean.FALSE.equals(existOrNot)) {
            throw new BizException(ResponseCodeEnum.FOLLOW_USER_NOT_EXIST);
        }

        // 当前用户的关注列表 Redis Key
        String followingRedisKey = RedisKeyConstants.buildFollowingUserKey(userId);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        // Lua 脚本路径
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/unfollow_check_and_delete.lua")));
        // 返回值类型
        script.setResultType(Long.class);

        // 执行 Lua 脚本，拿到返回结果
        Long result = redisTemplate.execute(script, Collections.singletonList(followingRedisKey), unfollowUserId);

        // 校验 Lua 脚本执行结果
        // 取关的用户不在关注列表中
        if (Objects.equals(result, LuaResultEnum.NOT_FOLLOWED.getCode())) {
            throw new BizException(ResponseCodeEnum.NOT_FOLLOWED);
        }

        if (Objects.equals(result, LuaResultEnum.ZSET_NOT_EXIST.getCode())) { // ZSET 关注列表不存在
            // 从数据库查询当前用户的关注关系记录
            List<FollowingDO> followingDOS = followingDOMapper.selectByUserId(userId);

            // 随机过期时间
            // 保底1天+随机秒数
            long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);

            // 若记录为空，则表示还未关注任何人，提示还未关注对方
            if (CollUtil.isEmpty(followingDOS)) {
                throw new BizException(ResponseCodeEnum.NOT_FOLLOWED);
            } else { // 若记录不为空，则将关注关系数据全量同步到 Redis 中，并设置过期时间；
                // 构建 Lua 参数
                Object[] luaArgs = buildLuaArgs(followingDOS, expireSeconds);

                // 执行 Lua 脚本，批量同步关注关系数据到 Redis 中
                DefaultRedisScript<Long> script3 = new DefaultRedisScript<>();
                script3.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_batch_add_and_expire.lua")));
                script3.setResultType(Long.class);
                redisTemplate.execute(script3, Collections.singletonList(followingRedisKey), luaArgs);

                // 再次调用上面的 Lua 脚本：unfollow_check_and_delete.lua , 将取关的用户删除
                result = redisTemplate.execute(script, Collections.singletonList(followingRedisKey), unfollowUserId);
                // 再次校验结果
                if (Objects.equals(result, LuaResultEnum.NOT_FOLLOWED.getCode())) {
                    throw new BizException(ResponseCodeEnum.NOT_FOLLOWED);
                }
            }
        }

        // 发送 MQ
        // 构建消息体 DTO
        UnfollowUserMqDTO unfollowUserMqDTO = UnfollowUserMqDTO.builder()
            .userId(userId)
            .unfollowUserId(unfollowUserId)
            .createTime(LocalDateTime.now())
            .build();

        // 构建消息对象，并将 DTO 转成 Json 字符串设置到消息体中
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(unfollowUserMqDTO))
            .build();

        // 通过冒号连接, 可让 MQ 发送给主题 Topic 时，携带上标签 Tag
        String destination = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW + ":" + MQConstants.TAG_UNFOLLOW;

        // 顺序key
        String hashKey = String.valueOf(userId);

        log.info("==> 开始发送取关操作 MQ, 消息体: {}", unfollowUserMqDTO);

        // 异步发送 MQ 消息，提升接口响应速度
        rocketMQTemplate.asyncSendOrderly(destination, message,hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> MQ 发送异常: ", throwable);
            }
        });

        return Response.success();
    }

    /**
     * 获取用户的关注列表
     * @param findFollowingListReqVO
     * @return
     */
    @Override
    public PageResponse<FindFollowingUserRspVO> findFollowingList(FindFollowingListReqVO findFollowingListReqVO) {
        // 获取要查询的用户id
        Long userId = findFollowingListReqVO.getUserId();
        // 获取要查询的页码
        Integer pageNo = findFollowingListReqVO.getPageNo();

        //构建redis的key,去redis缓存里查
        String followKey = RedisKeyConstants.buildFollowingUserKey(userId);
        Long size = redisTemplate.opsForZSet().zCard(followKey);
        long count = Objects.isNull(size)? 0L:size;
        // 返参
        List<FindFollowingUserRspVO> findFollowingUserRspVOS = null;
        long limit = 10L;

        if (count>0){
            // 判断要查询的页码数是否超过总页数
            // 获取总页数
            long totalPage = PageResponse.getTotalPage(count, limit);

            if (pageNo>totalPage){
                return PageResponse.success(null,pageNo,count);
            }

            // 否则则从redis缓存中获取用户的id,rev代表从高到低，这里的score是关注的时间戳，也就是按最新关注时间排序
            long offset = (pageNo-1)*limit;

            Set<Object> redisValues = redisTemplate.opsForZSet()
                .reverseRangeByScore(followKey, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, offset, limit);

            if (CollUtil.isNotEmpty(redisValues)){
                List<Long> ids = redisValues.stream().map(id -> Long.valueOf((String) id)).toList();
                List<FindUserByIdRspDTO> userInfoList = userRpcService.findByIds(ids);

                if (CollUtil.isNotEmpty(userInfoList)){
                    findFollowingUserRspVOS = userInfoList.stream().map(info -> FindFollowingUserRspVO.builder()
                        .userId(info.getId())
                        .avatar(info.getAvatar())
                        .nickname(info.getNickName())
                        .introduction(info.getIntroduction())
                        .build()).toList();
                }
            }

        }else {
            // redis里没有就查数据库
            count = followingDOMapper.selectCount(userId);

            long totalPage = PageResponse.getTotalPage(count, limit);

            // 如果查询页码大于总页码就返回空
            if(pageNo>totalPage){
                return PageResponse.success(null,pageNo,count);
            }
            // 构建分页查询偏移量用于数据库查询
            long offset = PageResponse.getPageOffset(pageNo, limit);

            List<FollowingDO> list =  followingDOMapper.selectPageListById(userId,offset,limit);

            // 如果非空就调用rpc服务，并将do转化为dto
            if (CollUtil.isNotEmpty(list)){
                List<Long> userIds = list.stream().map(FollowingDO::getFollowingUserId).toList();

                List<FindUserByIdRspDTO> rspDTOS = userRpcService.findByIds(userIds);

                if (CollUtil.isNotEmpty(rspDTOS)){
                    findFollowingUserRspVOS = rspDTOS.stream().map(dto-> FindFollowingUserRspVO.builder()
                        .userId(dto.getId())
                        .nickname(dto.getNickName())
                        .avatar(dto.getAvatar())
                        .introduction(dto.getIntroduction())
                        .build()
                    ).toList();
                }

                // 异步将关注列表全量同步到redis中
                taskExecutor.execute(()->syncFollowingList2Redis(userId));

            }

        }

        return PageResponse.success(findFollowingUserRspVOS,pageNo,count);
    }

    /**
     * 获取用户的粉丝列表
     * @param findFansUserReqVO
     * @return
     */
    @Override
    public PageResponse<FindFansUserRspVO> findFansList(FindFansUserReqVO findFansUserReqVO) {
        Long userId = findFansUserReqVO.getUserId();
        Long pageNo = findFansUserReqVO.getPageNo();
        // 先去redis里查
        String key = RedisKeyConstants.buildFansUserKey(userId);
        long limit = 10L;

        Long count = redisTemplate.opsForZSet().zCard(key);
        count = Objects.isNull(count)?0:count;

        // 返参
        List<FindFansUserRspVO> findFansUserRspVOS = null;


        if (count > 0) {
            // 根据查出来的数据判断页码是否超出范围
            long totalPage = PageResponse.getTotalPage(count, limit);
            if (pageNo > totalPage){
                return PageResponse.success(null,pageNo,count);
            }
            long offset = PageResponse.getPageOffset(pageNo, limit);

            // 查询redis
            Set<Object> redisValues = redisTemplate.opsForZSet()
                .reverseRangeByScore(key, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, offset, limit);

            if (CollUtil.isNotEmpty(redisValues)){
                List<Long> fansIds = redisValues.stream().map(id -> Long.valueOf((String) id)).toList();

                // RPC: 批量查询用户信息
                findFansUserRspVOS = rpcUserServiceAndCountServiceAndDTO2VO(fansIds, findFansUserRspVOS);
            }

        }
        else {
            // 若 Redis 缓存中无数据，则查询数据库
            // 先查询记录总量
            count = fansDOMapper.selectCountByUserId(userId);

            // 计算一共多少页
            long totalPage = PageResponse.getTotalPage(count, limit);

            // 请求的页码超出了总页数（只允许查询前 500 页）
            if (pageNo > 500 || pageNo > totalPage) return PageResponse.success(null, pageNo, count);

            // 偏移量
            long offset = PageResponse.getPageOffset(pageNo, limit);

            // 分页查询
            List<FansDO> fansDOS = fansDOMapper.selectPageListByUserId(userId, offset, limit);

            // 若记录不为空
            if (CollUtil.isNotEmpty(fansDOS)) {
                // 提取所有粉丝用户 ID 到集合中
                List<Long> userIds = fansDOS.stream().map(FansDO::getFansUserId).toList();

                // RPC: 调用用户服务、计数服务，并将 DTO 转换为 VO
                findFansUserRspVOS = rpcUserServiceAndCountServiceAndDTO2VO(userIds, findFansUserRspVOS);

                // 异步将粉丝列表同步到 Redis（最多5000条）
                taskExecutor.execute(() -> syncFansList2Redis(userId));
            }
        }

        return PageResponse.success(findFansUserRspVOS, pageNo, count);
    }

    /**
     * 粉丝列表同步到 Redis（最多5000条）
     * @param userId
     */
    private void syncFansList2Redis(Long userId) {
        // 查询粉丝列表（最多5000位用户）
        List<FansDO> fansDOS = fansDOMapper.select5000FansByUserId(userId);
        if (CollUtil.isNotEmpty(fansDOS)) {
            // 用户粉丝列表 Redis Key
            String fansListRedisKey = RedisKeyConstants.buildFansUserKey(userId);
            // 随机过期时间
            // 保底1天+随机秒数
            long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);
            // 构建 Lua 参数
            Object[] luaArgs = buildFansZSetLuaArgs(fansDOS, expireSeconds);

            // 执行 Lua 脚本，批量同步关注关系数据到 Redis 中
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_batch_add_and_expire.lua")));
            script.setResultType(Long.class);
            redisTemplate.execute(script, Collections.singletonList(fansListRedisKey), luaArgs);
        }
    }

    /**
     * 全量同步关注列表至 Redis 中
     */
    private void syncFollowingList2Redis(Long userId) {
        // 查询用户关注列表的最新的一千个关注
        List<FollowingDO> list =  followingDOMapper.selectAllByUserId(userId);
        if (CollUtil.isNotEmpty(list)){
            // 构建redis的缓存key
            String key = RedisKeyConstants.buildFollowingUserKey(userId);

            // 生成缓存过期时间
            long expiredSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);
            // 生成lua脚本参数
            Object[] luaArgs = buildLuaArgs(list, expiredSeconds);

            // 执行lua脚本，将用户的关注ids和对应关注时间缓存到redis里面
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_batch_add_and_expire.lua")));
            script.setResultType(Long.class);

            redisTemplate.execute(script,Collections.singletonList(key),luaArgs);
        }
    }

    /**
     * RPC: 调用用户服务、计数服务，并将 DTO 转换为 VO 粉丝列表
     * @param userIds
     * @param findFansUserRspVOS
     * @return
     */
    private List<FindFansUserRspVO> rpcUserServiceAndCountServiceAndDTO2VO(List<Long> userIds, List<FindFansUserRspVO> findFansUserRspVOS) {
        // RPC: 批量查询用户信息
        List<FindUserByIdRspDTO> findUserByIdRspDTOS = userRpcService.findByIds(userIds);

        // TODO RPC: 批量查询用户的计数数据（笔记总数、粉丝总数）

        // 若不为空，DTO 转 VO
        if (CollUtil.isNotEmpty(findUserByIdRspDTOS)) {
            findFansUserRspVOS = findUserByIdRspDTOS.stream()
                .map(dto -> FindFansUserRspVO.builder()
                    .userId(dto.getId())
                    .avatar(dto.getAvatar())
                    .nickname(dto.getNickName())
                    .noteTotal(0L) // TODO: 这块的数据暂无，后续补充
                    .fansTotal(0L) // TODO: 这块的数据暂无，后续补充
                    .build())
                .toList();
        }
        return findFansUserRspVOS;
    }


    /**
     * 校验 Lua 脚本结果，根据状态码抛出对应的业务异常
     * @param result
     */
    private static void checkLuaScriptResult(Long result) {
        LuaResultEnum luaResultEnum = LuaResultEnum.valueOf(result);

        if (Objects.isNull(luaResultEnum)) throw new RuntimeException("Lua 返回结果错误");
        // 校验 Lua 脚本执行结果
        switch (luaResultEnum) {
            // 关注数已达到上限
            case FOLLOW_LIMIT -> throw new BizException(ResponseCodeEnum.FOLLOW_USER_EXCEED_MAX_COUNT);
            // 已经关注了该用户
            case ALREADY_FOLLOWED -> throw new BizException(ResponseCodeEnum.ALREADY_FOLLOWED);
        }
    }

    /**
     * 构建 Lua 脚本参数
     *
     * @param followingDOS
     * @param expireSeconds
     * @return
     */
    private static Object[] buildLuaArgs(List<FollowingDO> followingDOS, long expireSeconds) {
        int argsLength = followingDOS.size() * 2 + 1; // 每个关注关系有 2 个参数（score 和 value），再加一个过期时间
        Object[] luaArgs = new Object[argsLength];

        int i = 0;
        for (FollowingDO following : followingDOS) {
            luaArgs[i] = DateUtils.localDateTime2Timestamp(following.getCreateTime()); // 关注时间作为 score
            luaArgs[i + 1] = following.getFollowingUserId();          // 关注的用户 ID 作为 ZSet value
            i += 2;
        }

        luaArgs[argsLength - 1] = expireSeconds; // 最后一个参数是 ZSet 的过期时间
        return luaArgs;
    }

    /**
     * 构建 Lua 脚本参数：粉丝列表
     * @param fansDOS
     * @param expireSeconds
     * @return
     */
    private static Object[] buildFansZSetLuaArgs(List<FansDO> fansDOS, long expireSeconds) {
        int argsLength = fansDOS.size() * 2 + 1; // 每个粉丝关系有 2 个参数（score 和 value），再加一个过期时间
        Object[] luaArgs = new Object[argsLength];

        int i = 0;
        for (FansDO fansDO : fansDOS) {
            luaArgs[i] = DateUtils.localDateTime2Timestamp(fansDO.getCreateTime()); // 粉丝的关注时间作为 score
            luaArgs[i + 1] = fansDO.getFansUserId();          // 粉丝的用户 ID 作为 ZSet value
            i += 2;
        }

        luaArgs[argsLength - 1] = expireSeconds; // 最后一个参数是 ZSet 的过期时间
        return luaArgs;
    }
}
