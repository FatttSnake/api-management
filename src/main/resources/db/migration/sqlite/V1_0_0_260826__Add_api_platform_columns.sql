alter table t_l_sys_log add column api_key_id integer null; -- API key ID
alter table t_l_event_log add column detail text null; -- Event detail
alter table t_l_event_log add column api_key_id integer null; -- Affected API key ID
alter table t_l_event_log add column target_user_id integer null; -- Target user ID
