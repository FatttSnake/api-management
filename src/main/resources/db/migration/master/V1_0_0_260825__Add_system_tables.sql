drop table if exists t_s_user;
create table t_s_user
(
    id                     bigint       not null primary key,
    username               varchar(20)  not null comment 'Username',
    password               varchar(255) not null comment 'Password',
    two_factor             varchar(40)  null comment 'Two-factor authentication secret key',
    verify                 varchar(144) null comment 'Email verification code',
    forget                 varchar(144) null comment 'Retrieve password verification code',
    locking                int          not null comment 'Locking status',
    expiration             datetime     null comment 'Expiration time',
    credentials_expiration datetime     null comment 'Credentials expiration time',
    enable                 int          not null comment 'Enable status',
    current_login_time     datetime     null comment 'Current login time',
    current_login_ip       varchar(128) null comment 'Current login IP',
    last_login_time        datetime     null comment 'Last login time',
    last_login_ip          varchar(128) null comment 'Last login IP',
    create_time            datetime     not null default (utc_timestamp()) comment 'Create time',
    update_time            datetime     not null default (utc_timestamp()) comment 'Update time',
    deleted                bigint       not null default 0,
    version                int          not null default 0,
    constraint t_s_user_unique_username unique (username, deleted),
    constraint t_s_user_unique_verify unique (verify, deleted),
    constraint t_s_user_unique_forget unique (forget, deleted)
) comment 'System - User';

drop table if exists t_s_user_info;
create table t_s_user_info
(
    id          bigint       not null primary key,
    user_id     bigint       not null comment 'User ID',
    nickname    varchar(50)  not null comment 'Nickname',
    avatar      text         null comment 'Avatar',
    email       varchar(100) not null comment 'Email',
    create_time datetime     not null default (utc_timestamp()) comment 'Create time',
    update_time datetime     not null default (utc_timestamp()) comment 'Update time',
    deleted     bigint       not null default 0,
    version     int          not null default 0,
    constraint t_s_user_info_unique_user_id unique (user_id, deleted),
    constraint t_s_user_info_unique_email unique (email, deleted)
) comment 'System - User Info';

drop table if exists t_s_power;
create table t_s_power
(
    id      bigint not null primary key,
    type_id int    not null comment 'Power type ID'
) comment 'System - Power';

drop table if exists t_s_module;
create table t_s_module
(
    id   bigint       not null primary key,
    name varchar(100) not null comment 'Name'
) comment 'System - Module';

drop table if exists t_s_menu;
create table t_s_menu
(
    id        bigint       not null primary key,
    name      varchar(30)  not null comment 'Name',
    url       varchar(100) null comment 'URL',
    parent_id bigint       null comment 'Parent menu ID',
    module_id bigint       not null comment 'Module ID'
) comment 'System - Menu';

drop table if exists t_s_scope;
create table t_s_scope
(
    id      bigint       not null primary key,
    name    varchar(100) not null comment 'Name',
    menu_id bigint       not null comment 'Menu ID'
) comment 'System - Scope';

drop table if exists t_s_operation;
create table t_s_operation
(
    id       bigint      not null primary key,
    name     varchar(50) not null comment 'Name',
    code     varchar(50) null comment 'Operation code',
    scope_id bigint      not null comment 'Scope ID'
) comment 'System - Operation';

drop table if exists t_s_group;
create table t_s_group
(
    id          bigint      not null primary key,
    name        varchar(30) not null comment 'Name',
    enable      int         not null comment 'Enable status',
    create_time datetime    not null default (utc_timestamp()) comment 'Create time',
    update_time datetime    not null default (utc_timestamp()) comment 'Update time',
    deleted     bigint      not null default 0,
    version     int         not null default 0,
    constraint t_s_group_unique unique (name, deleted)
) comment 'System - User Group';

drop table if exists t_r_user_group;
create table t_r_user_group
(
    id       bigint not null primary key,
    user_id  bigint not null comment 'User ID',
    group_id bigint not null comment 'User group ID',
    deleted  bigint not null default 0,
    version  int    not null default 0
) comment 'Bridge - User - User Group';

drop table if exists t_s_role;
create table t_s_role
(
    id          bigint      not null primary key,
    name        varchar(20) not null comment 'Name',
    enable      int         not null comment 'Enable status',
    create_time datetime    not null default (utc_timestamp()) comment 'Create time',
    update_time datetime    not null default (utc_timestamp()) comment 'Update time',
    deleted     bigint      not null default 0,
    version     int         not null default 0,
    constraint t_s_role_unique unique (name, deleted)
) comment 'System - Role';

drop table if exists t_r_role_group;
create table t_r_role_group
(
    id       bigint not null primary key,
    role_id  bigint not null comment 'Role ID',
    group_id bigint not null comment 'User group ID',
    deleted  bigint not null default 0,
    version  int    not null default 0
) comment 'Bridge - Role - User Group';

drop table if exists t_r_user_role;
create table t_r_user_role
(
    id      bigint not null primary key,
    user_id bigint not null comment 'User ID',
    role_id bigint not null comment 'Role ID',
    deleted bigint not null default 0,
    version int    not null default 0
) comment 'Bridge - User - Role';

drop table if exists t_r_power_role;
create table t_r_power_role
(
    id       bigint not null primary key,
    power_id bigint not null comment 'Power ID',
    role_id  bigint not null comment 'Role ID',
    deleted  bigint not null default 0,
    version  int    not null default 0
) comment 'Bridge - Power - Role';

drop table if exists t_s_sensitive_word;
create table t_s_sensitive_word
(
    id      bigint       not null primary key,
    word    varchar(400) not null comment 'Word',
    use_for varchar(50)  null comment 'Use for',
    enable  int          not null default 1 comment 'Enable status',
    deleted bigint       not null default 0,
    version int          not null default 0,
    constraint t_s_sensitive_word_unique_word unique (word, deleted)
) comment 'System - Sensitive World';

drop table if exists t_s_storage_blob;
create table t_s_storage_blob
(
    file_hash       varchar(64) primary key comment 'Original file SHA-256 key',
    reference_count bigint   not null comment 'Blob reference count',
    create_time     datetime not null default (utc_timestamp()) comment 'Create time',
    update_time     datetime not null default (utc_timestamp()) comment 'Update time',
    version         int      not null default 0
) comment 'System - Storage blob';
