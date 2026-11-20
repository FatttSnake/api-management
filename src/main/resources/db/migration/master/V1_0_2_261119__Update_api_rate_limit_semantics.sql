alter table t_b_api_plugin
    modify column default_rate_limit int null comment 'Default per-API total rate limit per minute (null/0=unlimited)';

alter table t_b_api_interface
    modify column rate_limit int null comment 'Per-API total rate limit per minute across all callers (null/0=inherit plugin default)';

alter table t_b_api_key
    modify column rate_limit int not null default 0 comment 'Per-key rate limit per minute (0=inherit user default)',
    modify column quota bigint not null default 0 comment 'Optional per-key quota per period (0=no per-key cap; the per-user quota still applies)',
    modify column quota_period int not null default 0 comment 'Per-key quota period in seconds (0=use the system period)';

-- 1.0.1 wrote the DDL default 86400 into every key, so an explicit override cannot be told
-- apart. Zero makes the key follow the system period; the current system period is also 86400,
-- so the behavior at migration time is unchanged.
update t_b_api_key set quota_period = 0 where quota_period = 86400;
