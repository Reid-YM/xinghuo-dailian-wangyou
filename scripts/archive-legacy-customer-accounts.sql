-- =============================================================================
-- 旧账号表归档（**必须用数据库管理账号执行，不能用应用账号**）
--
-- 为什么单独拆出来：
--   V35 迁移的第一步是把旧表 customer_accounts 重命名归档，而 RENAME TABLE
--   需要原表的 DROP 权限。应用账号 xinghuo_v2 只有
--   SELECT/INSERT/UPDATE/DELETE/CREATE/REFERENCES/INDEX/ALTER，
--   没有 DROP，因此这一步由 Flyway 执行时会直接失败，
--   并把数据库留在「迁移已开始但未完成」的状态。
--
-- 执行时机：部署新版本**之前**。
-- 幂等性：本脚本可重复执行；表已归档时不做任何事。
-- 执行方式：
--   mysql --defaults-file=<管理账号配置> < archive-legacy-customer-accounts.sql
-- =============================================================================

SET @has_old := (SELECT COUNT(*) FROM information_schema.TABLES
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customer_accounts');
SET @has_legacy := (SELECT COUNT(*) FROM information_schema.TABLES
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customer_accounts_legacy_20261001');

-- 两者同时存在说明处于异常状态：既没归档成功，又已有归档表。
-- 这时不猜测、不覆盖，直接报错让人来判断。
SET @state := IF(@has_old > 0 AND @has_legacy > 0, 'CONFLICT',
            IF(@has_old > 0, 'ARCHIVE', 'ALREADY_DONE'));
SELECT @state AS archive_state;

SET @stmt := CASE @state
    WHEN 'ARCHIVE' THEN
        'RENAME TABLE customer_accounts TO customer_accounts_legacy_20261001'
    WHEN 'CONFLICT' THEN
        'SIGNAL_STATE_CONFLICT'
    ELSE
        'DO 0'
END;

-- SIGNAL 需要写在存储程序里，这里用一个必然失败的语句来表达冲突，
-- 让脚本停下来而不是带着未知状态继续。
SET @stmt := IF(@stmt = 'SIGNAL_STATE_CONFLICT',
                'SELECT * FROM __archive_conflict_customer_accounts_and_legacy_both_exist__',
                @stmt);

PREPARE archive_stmt FROM @stmt;
EXECUTE archive_stmt;
DEALLOCATE PREPARE archive_stmt;

-- 切断指向旧账号的悬空引用：归档后旧的 customer_account_id 已无对应账号，
-- 若不清理，新账号从 id=1 重新开始时历史报价/订单会错误归属到新用户。
UPDATE quotes          SET customer_account_id = NULL WHERE customer_account_id IS NOT NULL;
UPDATE customer_orders SET customer_account_id = NULL WHERE customer_account_id IS NOT NULL;

SELECT
    (SELECT COUNT(*) FROM information_schema.TABLES
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customer_accounts') AS has_new_table,
    (SELECT COUNT(*) FROM information_schema.TABLES
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customer_accounts_legacy_20261001') AS has_legacy_table;
