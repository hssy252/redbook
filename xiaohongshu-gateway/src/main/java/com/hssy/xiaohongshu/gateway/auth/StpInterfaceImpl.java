package com.hssy.xiaohongshu.gateway.auth;

import cn.dev33.satoken.stp.StpInterface;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 功能简述
 *  自定义权限校验扩展接口
 * @author hssy
 * @version 1.0
 * @since 2024/12/14 13:24
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 返回此 loginId 拥有的权限列表

        // todo 从 redis 获取

        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 返回此 loginId 拥有的角色列表

        // todo 从 redis 获取

        return Collections.emptyList();
    }
}
