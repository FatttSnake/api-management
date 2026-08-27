insert into t_s_power (id, type_id)
    values (1540000, 2),
           (1540100, 3),
           (1540200, 3),
           (1540300, 3),
           (1540400, 3),
           (1540500, 3),
           (1540600, 3),
           (1540700, 3),
           (1540101, 4),
           (1540102, 4),
           (1540103, 4),
           (1540104, 4),
           (1540105, 4),
           (1540106, 4),
           (1540201, 4),
           (1540202, 4),
           (1540301, 4),
           (1540401, 4),
           (1540402, 4),
           (1540403, 4),
           (1540501, 4),
           (1540601, 4),
           (1540701, 4),
           (1540702, 4),
           (1530105, 4),
           (1530305, 4) as new_value
on duplicate key update type_id = new_value.type_id;

insert into t_s_menu (id, name, url, parent_id, module_id)
    values (1540000, 'API 平台', '^/system/api(/.*)?$', 1990000, 1000000) as new_value
on duplicate key update name      =new_value.name,
                        url       =new_value.url,
                        parent_id =new_value.parent_id;

insert into t_s_scope(id, name, menu_id)
    values (1540100, '密钥', 1540000),
           (1540200, '账户', 1540000),
           (1540300, '用量', 1540000),
           (1540400, '统计', 1540000),
           (1540500, '监控', 1540000),
           (1540600, '审计', 1540000),
           (1540700, '注册表', 1540000) as new_value
on duplicate key update name      = new_value.name,
                        menu_id   = new_value.menu_id;

insert into t_s_operation(id, name, code, scope_id)
    values (1540101, '列表', 'system:api:key:list', 1540100),
           (1540102, '增加', 'system:api:key:add', 1540100),
           (1540103, '修改', 'system:api:key:modify', 1540100),
           (1540104, '状态', 'system:api:key:status', 1540100),
           (1540105, '密钥', 'system:api:key:secret', 1540100),
           (1540106, '删除', 'system:api:key:delete', 1540100),
           (1540201, '查询', 'system:api:account:query', 1540200),
           (1540202, '充值', 'system:api:account:topup', 1540200),
           (1540301, '查询', 'system:api:usage:query', 1540300),
           (1540401, '用量', 'system:api:stats:usage', 1540400),
           (1540402, '费用', 'system:api:stats:cost', 1540400),
           (1540403, 'Top', 'system:api:stats:top', 1540400),
           (1540501, '看板', 'system:api:monitor:dashboard', 1540500),
           (1540601, '查询', 'system:api:audit:query', 1540600),
           (1540701, '查询', 'system:api:registry:query', 1540700),
           (1540702, '价格', 'system:api:registry:price', 1540700),
           (1530105, 'API', 'system:settings:query:api', 1530100),
           (1530305, 'API', 'system:settings:modify:api', 1530300) as new_value
on duplicate key update name=new_value.name,
                        code=new_value.code,
                        scope_id=new_value.scope_id;
