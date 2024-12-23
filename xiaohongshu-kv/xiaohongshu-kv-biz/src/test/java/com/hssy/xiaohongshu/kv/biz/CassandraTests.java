package com.hssy.xiaohongshu.kv.biz;

import com.hssy.framework.commom.util.JsonUtils;
import com.hssy.xiaohongshu.kv.biz.domain.dataobject.NoteContentDO;
import com.hssy.xiaohongshu.kv.biz.domain.repository.NoteContentRepository;
import jakarta.annotation.Resource;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
class CassandraTests {

    @Resource
    private NoteContentRepository noteContentRepository;

    /**
     * 测试插入数据
     */
    @Test
    void testInsert() {
        NoteContentDO nodeContent = NoteContentDO.builder()
            .id(UUID.randomUUID())
            .content("代码测试笔记内容插入")
            .build();

        noteContentRepository.save(nodeContent);
    }

    /**
     * 测试查询数据
     */
    @Test
    void testSelect() {
        Optional<NoteContentDO> optional = noteContentRepository.findById(UUID.fromString("4b1e839f-62fb-4a7d-b9fb-d6be248fba9a"));
        optional.ifPresent(noteContentDO -> log.info("查询结果：{}", JsonUtils.toJsonString(noteContentDO)));
    }

}