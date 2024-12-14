package com.hssy.xiaohongshu.auth.runner;

import cn.hutool.core.collection.CollUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hssy.framework.commom.util.JsonUtils;
import com.hssy.xiaohongshu.auth.constant.RedisKeyConstants;
import com.hssy.xiaohongshu.auth.domain.dataobject.PermissionDO;
import com.hssy.xiaohongshu.auth.domain.dataobject.RoleDO;
import com.hssy.xiaohongshu.auth.domain.dataobject.RolePermissionDO;
import com.hssy.xiaohongshu.auth.domain.mapper.PermissionDOMapper;
import com.hssy.xiaohongshu.auth.domain.mapper.RoleDOMapper;
import com.hssy.xiaohongshu.auth.domain.mapper.RolePermissionDOMapper;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.C;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/12 13:34
 */

@Component
@Slf4j
public class PushRolePermissions2RedisRunner implements ApplicationRunner {

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    @Resource
    private RoleDOMapper roleDOMapper;

    @Resource
    private RolePermissionDOMapper rolePermissionDOMapper;

    @Resource
    private PermissionDOMapper permissionDOMapper;

    // 权限同步标记 Key,防止runner多次启动
    private static final String PUSH_PERMISSION_FLAG = "push_permission_flag";

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("==> 服务启动，开始同步角色权限数据到 Redis 中...");

        try {
            // 是否能够同步数据: 原子操作，只有在键 PUSH_PERMISSION_FLAG 不存在时，才会设置该键的值为 "1"，并设置过期时间为 1 天
            boolean canPushed = redisTemplate.opsForValue().setIfAbsent(PUSH_PERMISSION_FLAG, "1", 1, TimeUnit.DAYS);

            // 如果无法同步权限数据
            if (!canPushed) {
                log.warn("==> 角色权限数据已经同步至 Redis 中，不再同步...");
                return;
            }

            //查询所有启用的角色id
            List<RoleDO> roleDOS = roleDOMapper.selectEnabledList();
            if (CollUtil.isNotEmpty(roleDOS)) {
                List<Long> roleIds = roleDOS.stream().map((RoleDO::getId)).collect(Collectors.toList());

                // 根据角色id查询对应的权限
                List<RolePermissionDO> rolePermissionDOList = rolePermissionDOMapper.selectByRoleIds(roleIds);
                Map<Long, List<Long>> roleIdPermissionIdsMap = rolePermissionDOList.stream().collect(Collectors.groupingBy(RolePermissionDO::getRoleId,
                    Collectors.mapping(RolePermissionDO::getPermissionId, Collectors.toList())));

                // 查询 APP 端所有被启用的权限
                List<PermissionDO> permissionDOS = permissionDOMapper.selectAppEnabledList();

                // 权限 ID - 权限 DO
                Map<Long, PermissionDO> permissionIdDOMap = permissionDOS.stream().collect(
                    Collectors.toMap(PermissionDO::getId, permissionDO -> permissionDO)
                );

                // 组织 角色-权限 关系
                Map<String, List<String>> roleKeyPermissionsMap = Maps.newHashMap();

                // 循环所有角色
                roleDOS.forEach(roleDO -> {
                    // 当前角色 ID
                    Long roleId = roleDO.getId();
                    // 当前角色 roleKey
                    String roleKey = roleDO.getRoleKey();
                    // 当前角色 ID 对应的权限 ID 集合
                    List<Long> permissionIds = roleIdPermissionIdsMap.get(roleId);
                    if (CollUtil.isNotEmpty(permissionIds)) {
                        List<String> permissionKeys = Lists.newArrayList();
                        permissionIds.forEach(permissionId -> {
                            // 根据权限 ID 获取具体的权限 DO 对象
                            PermissionDO permissionDO = permissionIdDOMap.get(permissionId);
                            permissionKeys.add(permissionDO.getPermissionKey());
                        });
                        roleKeyPermissionsMap.put(roleKey, permissionKeys);
                    }
                });

                // 同步至 Redis 中，方便后续网关查询 Redis, 用于鉴权
                roleKeyPermissionsMap.forEach((roleKey, permissions) -> {
                    String key = RedisKeyConstants.buildRolePermissionsKey(roleKey);
                    redisTemplate.opsForValue().set(key, JsonUtils.toJsonString(permissions));
                });
            }
            log.info("==> 服务启动，成功同步角色权限数据到 Redis 中...");
        } catch (Exception e) {
            log.error("==> 同步角色权限数据到 Redis 中失败: ", e);
        }

    }
}
