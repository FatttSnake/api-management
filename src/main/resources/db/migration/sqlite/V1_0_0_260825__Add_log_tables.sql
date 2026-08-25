drop table if exists t_l_sys_log;
create table t_l_sys_log -- Local - System Log
(
    id                     integer not null,
    log_type               text    not null,                                                -- Log type
    trace_id               text    null     default null,                                   -- Trace ID
    operate_user_id        integer not null,                                                -- Operation user ID
    operate_time           text    not null default (strftime('%Y-%m-%d %H:%M:%f', 'now')), -- Operation time
    request_uri            text    null     default null,                                   -- Request URI
    request_method         text    null     default null,                                   -- Request method
    request_params         text    null,                                                    -- Request parameters
    request_ip             text    not null,                                                -- Request IP
    request_server_address text    not null,                                                -- Request server address
    exception              integer not null default 0,                                      -- Exception status
    exception_info         text    null,                                                    -- Exception info
    start_time             text    not null,                                                -- Execution start time
    end_time               text    not null,                                                -- Execution end time
    execute_time           integer null     default null,                                   -- Execution takes time
    user_agent             text    null     default null                                    -- User agent
);

drop table if exists t_l_event_log;
create table t_l_event_log -- Local - Event Log
(
    id              integer not null primary key,
    event           text    not null,                                               -- Event,
    operate_user_id integer not null,                                               -- Operation user ID
    operate_time    text    not null default (strftime('%Y-%m-%d %H:%M:%f', 'now')) -- Operation time
);

drop table if exists t_l_statistics_log;
create table t_l_statistics_log -- Local - Statistics Log
(
    id          integer not null primary key,
    key         text    not null,                                               -- Record key
    value       text    not null,                                               -- Record value
    record_time text    not null default (strftime('%Y-%m-%d %H:%M:%f', 'now')) -- Record time
);
