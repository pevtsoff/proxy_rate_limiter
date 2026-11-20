-- Exact sliding-window log ("moving-window").
-- KEYS[1] counter key; ARGV[1] limit; ARGV[2] window in ms; ARGV[3] unique member id.
-- Returns "allowed:remaining:retryAfterMs".
local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local member = ARGV[3]

-- Redis' clock, not the caller's: every instance sees the same time.
local time = redis.call('TIME')
local now = tonumber(time[1]) * 1000 + math.floor(tonumber(time[2]) / 1000)

redis.call('ZREMRANGEBYSCORE', key, '-inf', now - window)
local count = redis.call('ZCARD', key)

if count < limit then
    redis.call('ZADD', key, now, member)
    redis.call('PEXPIRE', key, window)
    return '1:' .. (limit - count - 1) .. ':0'
end

local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
local retry = tonumber(oldest[2]) + window - now
if retry < 1 then
    retry = 1
end
return '0:0:' .. string.format('%d', retry)
