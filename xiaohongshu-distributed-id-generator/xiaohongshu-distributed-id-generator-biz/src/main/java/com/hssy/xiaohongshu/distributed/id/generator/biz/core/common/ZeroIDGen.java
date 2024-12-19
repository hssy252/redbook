package com.hssy.xiaohongshu.distributed.id.generator.biz.core.common;

import com.hssy.xiaohongshu.distributed.id.generator.biz.core.IDGen;
import com.hssy.xiaohongshu.distributed.id.generator.biz.core.common.Result;

public class ZeroIDGen implements IDGen {
    @Override
    public Result get(String key) {
        return new Result(0, Status.SUCCESS);
    }

    @Override
    public boolean init() {
        return true;
    }
}
