package com.hssy.xiaohongshu.note.biz.constants;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/21 17:21
 */
public class RedisKeyConstants {

    /**
     * 笔记详情 KEY 前缀
     */
    public static final String NOTE_DETAIL_KEY = "note:detail:";


    /**
     * 构建完整的笔记详情 KEY
     * @param noteId
     * @return
     */
    public static String buildNoteDetailKey(Long noteId) {
        return NOTE_DETAIL_KEY + noteId;
    }

}
