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
    config_schema       text           null comment 'Embedded plugin config schema JSON snapshot (META-INF/plugin-config.json)',
    descriptor_format   int            not null default 1 comment 'Plugin descriptor format',
    gateway_api_version int            not null default 1 comment 'Gateway API major version the plugin was compiled against',
    requires_gateway    varchar(100)   not null default '>=1.0.0 <2.0.0' comment 'Required gateway version range',
    requires_gateway_api varchar(100)  not null default '^1.0' comment 'Required gateway API version range',
    dependency_lock     longtext       null comment 'Resolved dependency lock JSON',
    lock_hash           varchar(64)    null comment 'Dependency lock SHA-256',
    load_status         varchar(20)    not null default 'LOADED' comment 'Load status [LOADED, LOAD_ERROR, DISABLED, UNRESOLVED]',
    load_error_code     varchar(40)    null comment 'Load error code',
    load_error          varchar(512)   null comment 'Last mount error message',
    create_time         datetime       not null default (utc_timestamp()) comment 'Create time',
    update_time         datetime       not null default (utc_timestamp()) comment 'Update time',
    version             int            not null default 0,
    constraint t_b_api_plugin_unique_plugin_id unique (plugin_id)
) comment 'API Platform - Plugin';

drop table if exists t_b_api_interface;
create table t_b_api_interface
(
    id           bigint         not null primary key,
    plugin_id    varchar(64)    not null comment 'Plugin ID',
    code         varchar(200)   not null comment 'API scoping code',
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
    version      int            not null default 0,
    constraint t_b_api_interface_unique_code unique (code),
    constraint t_b_api_interface_unique_path_method unique (plugin_id, path, method)
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
    api_code       varchar(200)   not null comment 'API scoping code',
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
    version     int            not null default 0,
    constraint t_b_api_account_unique_user_id unique (user_id)
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
    version       int            not null default 0,
    constraint t_b_api_transaction_unique_order_no unique (order_no),
    constraint t_b_api_transaction_unique_api_usage unique (api_usage_id)
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
    version       int          not null default 0,
    constraint t_b_api_plugin_setting_unique_key unique (plugin_id, setting_key)
) comment 'API Platform - Plugin Setting';

drop table if exists t_b_plugin_module;
create table t_b_plugin_module
(
    id          bigint       not null primary key,
    group_id       varchar(200) not null comment 'Maven group',
    artifact_id    varchar(200) not null comment 'Maven artifact',
    module_version varchar(100) not null comment 'Resolved Maven version',
    sha256         varchar(64)  not null comment 'Module jar SHA-256',
    size           bigint       not null default 0 comment 'Module jar size in bytes',
    source         varchar(500) null comment 'Repository the module was resolved from',
    install_time   datetime     not null default (utc_timestamp()) comment 'Install time',
    create_time    datetime     not null default (utc_timestamp()) comment 'Create time',
    update_time    datetime     not null default (utc_timestamp()) comment 'Update time',
    version        int          not null default 0,
    constraint t_b_plugin_module_unique_coordinate unique (group_id, artifact_id, module_version),
    constraint t_b_plugin_module_unique_sha unique (sha256)
) comment 'API Platform - Plugin Module';
create index t_b_plugin_module_index_sha on t_b_plugin_module (sha256);

drop table if exists t_b_plugin_module_edge;
create table t_b_plugin_module_edge
(
    id                  bigint not null primary key,
    module_id           bigint not null comment 'Dependent module ID',
    dependency_module_id bigint not null comment 'Direct dependency module ID',
    version             int    not null default 0,
    constraint t_b_plugin_module_edge_unique unique (module_id, dependency_module_id)
) comment 'API Platform - Plugin Module Edge';
create index t_b_plugin_module_edge_index_dependency on t_b_plugin_module_edge (dependency_module_id);

drop table if exists t_b_plugin_module_ref;
create table t_b_plugin_module_ref
(
    id          bigint      not null primary key,
    plugin_id   varchar(64) not null comment 'Plugin ID',
    module_id   bigint      not null comment 'Module ID',
    create_time datetime    not null default (utc_timestamp()) comment 'Create time',
    version     int         not null default 0,
    constraint t_b_plugin_module_ref_unique unique (plugin_id, module_id)
) comment 'API Platform - Plugin Module Reference';
create index t_b_plugin_module_ref_index_plugin on t_b_plugin_module_ref (plugin_id);
create index t_b_plugin_module_ref_index_module on t_b_plugin_module_ref (module_id);

alter table t_s_menu
    add column plugin_id varchar(64) null comment 'API Platform plugin ID (plugin-created menu)';
alter table t_s_menu
    add unique key t_s_menu_unique_plugin_id (plugin_id);
