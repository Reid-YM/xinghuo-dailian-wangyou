-- Add permissions for newly migrated modules. Do not broaden existing operator compatibility.
INSERT INTO xw_permissions (code, name, module_code, built_in, created_at) VALUES
('customer:view', '网站会员查看', 'customer', TRUE, NOW(6)),
('customer:manage', '网站会员管理', 'customer', TRUE, NOW(6)),
('feedback:view', '测试反馈查看', 'feedback', TRUE, NOW(6)),
('feedback:manage', '测试反馈管理', 'feedback', TRUE, NOW(6));

-- Only the existing management safety role receives these new capabilities by default.
INSERT INTO xw_role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW(6) FROM xw_rbac_roles r CROSS JOIN xw_permissions p
WHERE r.code = 'XW_MANAGEMENT'
  AND p.code IN ('customer:view', 'customer:manage', 'feedback:view', 'feedback:manage');
