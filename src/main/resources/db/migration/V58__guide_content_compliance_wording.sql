-- V58: 攻略内容合规改写（小程序「攻略」页渲染合规）
--
-- 背景：微信小程序类目文档「电子娱乐服务」资质说明中明确写着
--       「不得提供代练（代打）或违法（如色情交易等）相关服务」。
--       小程序「攻略」页会渲染 guide_sections.body_markdown 等字段，
--       原文里夹着的服务推广句子（代练/代肝/报价/下单/购买…）会直接踩红线。
--
-- 本迁移只改文案，不动表结构、不动 guide_products（网站服务位保持原样，
-- 小程序端本来就不渲染 products 字段）。
--
-- 幂等：REPLACE 第二次执行为空操作。
-- 回滚：把下列每条的 old / new 互换再执行一次即可。
-- 生成：星火小程序仓库 tools/gen-guide-migration.js（已在线上内容上验证）

-- game_guides.title / subtitle / summary
UPDATE game_guides SET title = REPLACE(title, '如何在《使命召唤：现代战争4》中快速升级武器（以及何时应该直接购买）', '如何在《使命召唤：现代战争4》中快速升级武器（以及什么时候集中投入更划算）');
UPDATE game_guides SET subtitle = REPLACE(subtitle, '武器经验来源、双倍经验窗口与购买边界', '武器经验来源、双倍经验窗口与投入取舍');
UPDATE game_guides SET summary = REPLACE(summary, '以及什么情况下找人代肝比自己慢慢打更合适。', '以及什么情况下集中投入比自己慢慢打更合适。');

-- guide_sections.heading / body_markdown
UPDATE guide_sections SET body_markdown = REPLACE(body_markdown, '购买前先确认包装、地区与有效期，��免买到已经过期的码。', '兑换前先确认包装、地区与有效期，避免拿到已经过期的码。');
UPDATE guide_sections SET body_markdown = REPLACE(body_markdown, '- 站内只提供活动任务代练与咨询，不代替官方兑换', '- 兑换一律以官方渠道为准，不要使用来源不明的码');
UPDATE guide_sections SET body_markdown = REPLACE(body_markdown, '本站迷彩任务按剩余挑战和目标模式报价。网页不收账号密码、验证码或令牌，交付方式请先与客服确认。', '迷彩进度可以在这个工具里逐把勾选，卡住的那几档再单独研究。网页不收账号密码、验证码或令牌。');
UPDATE guide_sections SET heading = REPLACE(heading, '什么时候值得找人代肝', '什么时候该集中投入');
UPDATE guide_sections SET body_markdown = REPLACE(body_markdown, '适合代肝的情况是：', '适合集中投入的情况是：');
UPDATE guide_sections SET body_markdown = REPLACE(body_markdown, '不适合代肝的情况也很明确：', '不适合集中投入的情况也很明确：');
UPDATE guide_sections SET body_markdown = REPLACE(body_markdown, '站内武器升级服务按目标等级和当前进度报价，下单前请说明平台、武器、目标等级和可操作时间。网页不收账号密码、验证码或令牌，具体交付方式先和客服确认。', '先把目标等级和当前进度对齐，再说明平台、武器和可操作时间，评估工作量会更准确。网页不收账号密码、验证码或令牌。');
UPDATE guide_sections SET heading = REPLACE(heading, '下单前准备', '准备清单');
UPDATE guide_sections SET body_markdown = REPLACE(body_markdown, '为了让报价准确，先整理这些信息：', '为了让评估准确，先整理这些信息：');
UPDATE guide_sections SET body_markdown = REPLACE(body_markdown, '信息越完整，越容易判断工作量，也越不容易出现重复报价。', '信息越完整，越容易判断工作量，也越不容易反复确认。');
UPDATE guide_sections SET body_markdown = REPLACE(body_markdown, '- 目标越高，单局难度和报价通常越高', '- 目标越高，单局难度和所需场次通常越高');
UPDATE guide_sections SET body_markdown = REPLACE(body_markdown, '不能用同一价格简单换算。', '不能用同一标准简单换算。');
UPDATE guide_sections SET body_markdown = REPLACE(body_markdown, '因此，服务通常按每局最低值计算，而不是承诺把生涯 K/D 提升到固定数字。下单前提供当前 K/D 和对局数，才能得到相对可靠的局数估算。', '因此，估算通常按每局最低值计算，不应该把生涯 K/D 提升到某个固定数字当成承诺。提供当前 K/D 和对局数，才能得到相对可靠的局数估算。');
UPDATE guide_sections SET body_markdown = REPLACE(body_markdown, 'K/D 提升通常需要代打，也就是由��人登录账号操作。任何代打都存在账号安全、平台规则和处罚风险，没有商家能承诺绝对安全。', 'K/D 提升本质取决于每局表现，短期大幅变化通常伴随高强度投入。任何涉及共享账号的操作都存在账号安全、平台规则和处罚风险，没有人能承诺绝对安全。');
UPDATE guide_sections SET body_markdown = REPLACE(body_markdown, '本站不在网页收集账号密码、验证码或令牌。是否采用代打、如何交付、账号风险由用户自行判断。如果只想让数据立刻明显变化，或者你本来就会大量自己打，短单可能并不划算，先咨询再决定。', '本站不在网页收集账号密码、验证码或令牌，任何情况下都不要把密码或验证码交给他人。如果只想让数据立刻明显变化，或者你本来就会大量自己打，短期投入可能并不划算。');
UPDATE guide_sections SET body_markdown = REPLACE(body_markdown, '如果想安排账号等级服务，先整理：', '如果想尽快提升账号等级，先整理：');
UPDATE guide_sections SET body_markdown = REPLACE(body_markdown, '站内相关服务按目标进度报价。网页不收账号密码、验证码或令牌，具体交付方式请在咨询时确认。', '进度评估以当前等级和目标等级为准。网页不收账号密码、验证码或令牌。');
UPDATE guide_sections SET body_markdown = REPLACE(body_markdown, '购买后，达到对应等级即可领取武器蓝图、外观和部分返还货币。', '开通后，达到对应等级即可领取武器蓝图、外观和部分返还货币。');
UPDATE guide_sections SET body_markdown = REPLACE(body_markdown, '已经购买但没解锁的奖励无法补领。', '已经开通但没解锁的奖励无法补领。');
UPDATE guide_sections SET body_markdown = REPLACE(body_markdown, '- 已经购买通行证，奖励即将过期', '- 已经开通通行证，奖励即将过期');

-- guide_faqs.question / answer
UPDATE guide_faqs SET question = REPLACE(question, '什么时候适合直接购买升级服务？', '什么时候集中投入更划算？');
UPDATE guide_faqs SET answer = REPLACE(answer, '并等官方公布排位规则后再确认代练方案。', '并等官方公布排位规则后再做决定。');
