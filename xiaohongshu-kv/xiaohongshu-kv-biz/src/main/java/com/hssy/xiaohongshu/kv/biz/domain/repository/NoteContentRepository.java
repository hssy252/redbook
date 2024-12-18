package com.hssy.xiaohongshu.kv.biz.domain.repository;

import com.hssy.xiaohongshu.kv.biz.domain.dataobject.NoteContentDO;
import java.util.UUID;
import org.springframework.data.cassandra.repository.CassandraRepository;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/18 16:32
 */
public interface NoteContentRepository extends CassandraRepository<NoteContentDO, UUID> {


}
