ALTER TABLE site_config
  ADD COLUMN trust_cases TEXT NULL AFTER trust_tags,
  ADD COLUMN trust_faq TEXT NULL AFTER trust_cases;

UPDATE site_config
SET
  trust_cases = CONCAT_WS('\n',
    '武器升级\tMW4 武器 1→满级配件\t约 50–90 分钟 · ¥35–60\t按当前等级抵扣，完成后可在业务记录查看进度说明。',
    '活动任务\t赛季限时任务清单\t约 3–6 小时 · 按任务报价\t先核对剩余任务与截止时间，再确认是否开工。',
    '迷彩挑战\t剩余挑战补齐\t当天可交付 · 按截图估价\t金/钻石/暗物质按剩余项目确认，网页不收密码。',
    '排位/段位\t当前段到目标段\t按段差报价 · 异常即停\t开工前确认目标、平台与可操作时间，异常及时沟通。'
  ),
  trust_faq = CONCAT_WS('\n',
    '怎么查进度？\t提交后会生成业务编号，在「业务记录」输入即可查看当前状态和客服备注。',
    '要不要给密码？\t网页、查询页、截图都不要发密码、验证码或令牌。具体上号方式加微信后单独确认。',
    '怎么付款？\t确认报价后走淘宝或闲鱼担保，网页不收钱。',
    '能取消吗？\t未开工可取消；已开工按完成进度核对后退剩余部分。',
    '支持哪些平台？\tSteam、Battle.net、PlayStation、Xbox、Switch 2 等平台按具体游戏确认。',
    '排位会不会掉？\t异常立即停单并沟通，不硬冲。排位类服务会先确认当前段位、目标段位和风险。',
    '截图怎么发？\t询价页可上传任务截图，也可以复制业务编号后加微信补发。截图里不要包含密码或验证码。',
    '多久回复？\t客服时间通常为 10:00–24:00，高峰期会按业务编号顺序处理。'
  )
WHERE id = 1
  AND (trust_cases IS NULL OR trust_cases = '')
  AND (trust_faq IS NULL OR trust_faq = '');
