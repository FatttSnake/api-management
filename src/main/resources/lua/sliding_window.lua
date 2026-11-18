-- Sliding window counter.
-- KEYS[1] hash key, fields are window indices
-- ARGV[1] limit, ARGV[2] window seconds, ARGV[3] key TTL seconds
-- Returns: allowed,effective,retryAfterSeconds,remaining

if redis.replicate_commands then
    redis.replicate_commands()
end

local key = KEYS[1]
local limit = tonumber(ARGV[1])
local windowSeconds = tonumber(ARGV[2])
local ttlSeconds = tonumber(ARGV[3])

local now = redis.call('TIME')
local nowMs = tonumber(now[1]) * 1000 + math.floor(tonumber(now[2]) / 1000)
local windowMs = windowSeconds * 1000
local current = math.floor(nowMs / windowMs)
local previous = current - 1
local elapsed = nowMs - current * windowMs
local weight = (windowMs - elapsed) / windowMs

local currentField = tostring(current)
local previousField = tostring(previous)
local previousCount = tonumber(redis.call('HGET', key, previousField) or '0')
local currentCount = tonumber(redis.call('HGET', key, currentField) or '0')
local effective = math.floor(previousCount * weight + currentCount)

local allowed = 0
if effective < limit then
    redis.call('HINCRBY', key, currentField, 1)
    currentCount = currentCount + 1
    effective = math.floor(previousCount * weight + currentCount)
    allowed = 1
end

redis.call('EXPIRE', key, ttlSeconds)
redis.call('HDEL', key, tostring(previous - 1))

local windowEnd = math.ceil((windowMs - elapsed) / 1000)
local retry
if allowed == 1 then
    retry = windowEnd
elseif previousCount > 0 and limit > currentCount then
    -- Seconds until previousCount * ((windowMs - t) / windowMs) + currentCount < limit.
    local seconds = windowMs * (1 - (limit - currentCount) / previousCount) / 1000
    retry = math.ceil(math.max(seconds, 0)) + 1
else
    -- The current window is exhausted, or the next window still starts above the limit.
    retry = windowEnd + 1
end
if retry < 1 then
    retry = 1
end

local remaining = limit - effective
if remaining < 0 then
    remaining = 0
end

return allowed .. ',' .. effective .. ',' .. retry .. ',' .. remaining
