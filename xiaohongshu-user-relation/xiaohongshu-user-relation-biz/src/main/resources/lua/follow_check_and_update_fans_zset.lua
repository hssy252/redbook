---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by 13759.
--- DateTime: 2024/12/23 22:11
---

local key = KEYS[1]

local fansId = ARGV[1]
local timestamp = ARGV[2]

-- 判断key是否存在，不存在直接返回
local exist = redis.call('EXISTS',key)
if exist == 0 then
    return -1
end

-- 判断粉丝缓存数是否小于5000
local count = redis.call('ZCARD',key)
-- 小于5000就删除最早的
if count>=5000 then
   redis.call('ZPOPMIN',key)
end
redis.call('ZADD',key,timestamp,fansId)
return 0