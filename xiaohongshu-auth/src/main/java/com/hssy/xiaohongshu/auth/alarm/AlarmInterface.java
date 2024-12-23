package com.hssy.xiaohongshu.auth.alarm;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/13 11:17
 */
public interface AlarmInterface {

    /**
     * 发送告警信息
     *
     * @param message
     * @return
     */
    boolean send(String message);

}
