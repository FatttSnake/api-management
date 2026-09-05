drop table if exists t_b_api_plugin;
create table t_b_api_plugin
(
    id                  bigint         not null primary key,
    plugin_id           varchar(64)    not null comment 'Plugin ID',
    name                varchar(100)   not null comment 'Plugin name',
    description         varchar(255)   null comment 'Description',
    enable              int            not null default 1 comment 'Enable status',
    default_price       decimal(10, 4) null comment 'Default price per call (null=free)',
    default_rate_limit  int            null comment 'Default rate limit per minute (null=unlimited)',
    default_access_mode varchar(20)    not null default 'RESTRICTED' comment 'Default access mode [DEFAULT, RESTRICTED]',
    source              varchar(20)    not null default 'UPLOADED' comment 'Plugin source [BUILT_IN, UPLOADED]',
    version_name        varchar(32)    null comment 'Plugin version name, e.g. 1.2.0',
    version_code        int            not null default 0 comment 'Plugin version code (monotonic)',
    file_hash           varchar(64)    null comment 'Plugin jar file hash (t_s_storage_blob.file_hash)',
    jar_name            varchar(255)   null comment 'Original jar file name',
    signer_key_id       varchar(64)    null comment 'Signer public key fingerprint (t_b_api_plugin_trust_key.key_id)',
    openapi             longtext       null comment 'Embedded OpenAPI fragment JSON',
    load_error          varchar(512)   null comment 'Last mount error message',
    create_time         datetime       not null default (utc_timestamp()) comment 'Create time',
    update_time         datetime       not null default (utc_timestamp()) comment 'Update time',
    deleted             bigint         not null default 0,
    version             int            not null default 0,
    constraint t_b_api_plugin_unique_plugin_id unique (plugin_id, deleted)
) comment 'API Platform - Plugin';

drop table if exists t_b_api_interface;
create table t_b_api_interface
(
    id           bigint         not null primary key,
    plugin_id    varchar(64)    not null comment 'Plugin ID',
    code         varchar(100)   not null comment 'API scoping code',
    name         varchar(100)   not null comment 'API name',
    description  varchar(255)   null comment 'Description',
    path         varchar(200)   not null comment 'Request path',
    method       varchar(10)    not null comment 'HTTP method',
    api_version  int            not null default 1 comment 'API version',
    price        decimal(10, 4) null comment 'Price per call (null=inherit plugin default)',
    billing_mode varchar(20)    not null default 'SUCCESS_ONLY' comment 'Billing mode [FREE, SUCCESS_ONLY, ALWAYS]',
    need_key     int            not null default 1 comment 'Need API key status',
    rate_limit   int            null comment 'Rate limit per minute (null=inherit plugin default)',
    enable       int            not null default 1 comment 'Enable status',
    access_mode  varchar(20)    null comment 'Access mode [DEFAULT, RESTRICTED]; null=inherit plugin',
    create_time  datetime       not null default (utc_timestamp()) comment 'Create time',
    update_time  datetime       not null default (utc_timestamp()) comment 'Update time',
    deleted      bigint         not null default 0,
    version      int            not null default 0,
    constraint t_b_api_interface_unique_code unique (code, deleted),
    constraint t_b_api_interface_unique_path_method unique (plugin_id, path, method, deleted)
) comment 'API Platform - Interface';

drop table if exists t_b_api_key;
create table t_b_api_key
(
    id              bigint       not null primary key,
    user_id         bigint       not null comment 'Owner user ID',
    access_key      varchar(64)  not null comment 'Access key',
    secret_key_hash varchar(64)  not null comment 'Secret key SHA-256 hash',
    name            varchar(100) not null comment 'Key name',
    permissions     text         null comment 'Scoped API codes snapshot',
    enable          int          not null default 1 comment 'Enabled status',
    expire_time     datetime     null comment 'Expire time',
    ip_whitelist    varchar(500) null comment 'IP whitelist',
    rate_limit      int          not null default 0 comment 'Per-key rate limit per minute (0=unlimited)',
    quota           bigint       not null default 0 comment 'Quota requests per period (0=unlimited)',
    quota_period    int          not null default 86400 comment 'Quota period in seconds',
    last_used_time  datetime     null comment 'Last used time',
    remark          varchar(255) null comment 'Remark',
    create_time     datetime     not null default (utc_timestamp()) comment 'Create time',
    update_time     datetime     not null default (utc_timestamp()) comment 'Update time',
    deleted         bigint       not null default 0,
    version         int          not null default 0,
    constraint t_b_api_key_unique_access_key unique (access_key, deleted)
) comment 'API Platform - API Key';
create index t_b_api_key_index_user_id on t_b_api_key (user_id, deleted);

drop table if exists t_b_api_usage;
create table t_b_api_usage
(
    id             bigint         not null primary key,
    api_key_id     bigint         null comment 'API key ID',
    api_id         bigint         not null comment 'API ID',
    api_code       varchar(100)   not null comment 'API scoping code',
    user_id        bigint         null comment 'Owner user ID',
    request_path   varchar(200)   not null comment 'Request path',
    request_method varchar(10)    not null comment 'HTTP method',
    response_code  int            null comment 'Response code',
    success        int            not null default 1 comment 'Success status',
    execute_time   bigint         null comment 'Execute time in ms',
    request_ip     varchar(128)   null comment 'Request IP',
    trace_id       varchar(64)    null comment 'Trace ID',
    cost           decimal(10, 4) not null default 0 comment 'Billed cost',
    billing_mode   varchar(20)    not null default 'SUCCESS_ONLY' comment 'Billing mode [FREE, SUCCESS_ONLY, ALWAYS]',
    create_time    datetime       not null default (utc_timestamp()) comment 'Create time',
    update_time    datetime       not null default (utc_timestamp()) comment 'Update time',
    deleted        bigint         not null default 0,
    version        int            not null default 0
) comment 'API Platform - API Usage';
create index t_b_api_usage_index_api_key_time on t_b_api_usage (api_key_id, create_time);
create index t_b_api_usage_index_api_time on t_b_api_usage (api_id, create_time);
create index t_b_api_usage_index_user_time on t_b_api_usage (user_id, create_time);

drop table if exists t_b_api_account;
create table t_b_api_account
(
    id          bigint         not null primary key,
    user_id     bigint         not null comment 'Owner user ID',
    balance     decimal(14, 4) not null default 0 comment 'Prepaid balance',
    enable      int            not null default 1 comment 'Account status',
    create_time datetime       not null default (utc_timestamp()) comment 'Create time',
    update_time datetime       not null default (utc_timestamp()) comment 'Update time',
    deleted     bigint         not null default 0,
    version     int            not null default 0,
    constraint t_b_api_account_unique_user_id unique (user_id, deleted)
) comment 'API Platform - API Account';

drop table if exists t_b_api_transaction;
create table t_b_api_transaction
(
    id            bigint         not null primary key,
    user_id       bigint         not null comment 'Owner user ID',
    api_key_id    bigint         null comment 'API key ID',
    api_usage_id  bigint         null comment 'API usage ID',
    order_no      varchar(64)    null comment 'Top-up order number',
    type          varchar(20)    not null comment 'Transaction type [TOPUP, DEDUCT, REFUND, ADJUST]',
    amount        decimal(14, 4) not null comment 'Signed amount',
    balance_after decimal(14, 4) not null comment 'Balance after transaction',
    remark        varchar(255)   null comment 'Remark',
    create_time   datetime       not null default (utc_timestamp()) comment 'Create time',
    update_time   datetime       not null default (utc_timestamp()) comment 'Update time',
    deleted       bigint         not null default 0,
    version       int            not null default 0,
    constraint t_b_api_transaction_unique_order_no unique (order_no, deleted),
    constraint t_b_api_transaction_unique_api_usage unique (api_usage_id, deleted)
) comment 'System - API Transaction';
create index t_b_api_transaction_index_user_time on t_b_api_transaction (user_id, create_time);

drop table if exists t_b_api_plugin_trust_key;
create table t_b_api_plugin_trust_key
(
    id          bigint       not null primary key,
    key_id      varchar(64)  not null comment 'Public key fingerprint (SHA-256 of SPKI, hex)',
    alias       varchar(100) null comment 'Key alias',
    public_key  text         not null comment 'Public key (SPKI PEM)',
    enable      int          not null default 1 comment 'Enable status',
    created_by  bigint       null comment 'Created by user ID',
    create_time datetime     not null default (utc_timestamp()) comment 'Create time',
    update_time datetime     not null default (utc_timestamp()) comment 'Update time',
    deleted     bigint       not null default 0,
    version     int          not null default 0,
    constraint t_b_api_plugin_trust_key_unique_key_id unique (key_id, deleted)
) comment 'API Platform - Plugin Trust Key';

drop table if exists t_b_api_plugin_setting;
create table t_b_api_plugin_setting
(
    id            bigint       not null primary key,
    plugin_id     varchar(64)  not null comment 'Plugin ID',
    setting_key   varchar(100) not null comment 'Setting key',
    setting_value text         null comment 'Setting value',
    create_time   datetime     not null default (utc_timestamp()) comment 'Create time',
    update_time   datetime     not null default (utc_timestamp()) comment 'Update time',
    deleted       bigint       not null default 0,
    version       int          not null default 0,
    constraint t_b_api_plugin_setting_unique_key unique (plugin_id, setting_key, deleted)
) comment 'API Platform - Plugin Setting';

drop table if exists t_b_api_plugin_datasource;
create table t_b_api_plugin_datasource
(
    plugin_id   varchar(64)  not null primary key comment 'Plugin ID',
    db_type     varchar(20)  not null default 'MYSQL' comment 'Database type [MYSQL, SQLITE]',
    url         varchar(500) null comment 'JDBC URL',
    username    varchar(100) null comment 'Username',
    password    text         null comment 'Encrypted password',
    create_time datetime     not null default (utc_timestamp()) comment 'Create time',
    update_time datetime     not null default (utc_timestamp()) comment 'Update time',
    deleted     bigint       not null default 0,
    version     int          not null default 0
) comment 'API Platform - Plugin Datasource';

alter table t_s_menu
    add column plugin_id varchar(64) null comment 'API Platform plugin ID (plugin-created menu)';
alter table t_s_menu
    add unique key t_s_menu_unique_plugin_id (plugin_id);
