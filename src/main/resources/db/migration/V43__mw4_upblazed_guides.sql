-- MW4 guide expansion based on UPBLAZED MW4 guide directory.
-- All article copy is rewritten for 星火网游代练平台. Source pages are credited through source_name/source_url.
-- The bot lobby guide from the source directory is intentionally excluded.


-- MW4 魔爪能量饮料兑换码：如何获取、兑换及所有奖励

INSERT INTO game_guides (game_code, slug, title, subtitle, summary, cover_image, difficulty, estimated_minutes, language, author_name, source_name, source_url, status, is_featured, sort_order, tags_json, seo_title, seo_description, canonical_url, view_count, published_at, created_at, updated_at, version) VALUES ('CODMW4', 'mw4-monster-energy-codes', 'MW4 魔爪能量饮料兑换码：如何获取、兑换及所有奖励', '魔爪联名活动规则、六个奖励与兑换步骤', '整理 MW4 魔爪能量饮料联动的兑换规则、六个奖励、活动时间与双倍经验上限，帮助你在活动结束前按正确顺序兑换。', '/assets/covers/service/codmw4-3.jpg', 'BEGINNER', 7, 'zh-CN', '星火攻略组', 'UPBLAZED', 'https://upblazed.com/guides/mw4-monster-energy-promotion-whats-next', 'PUBLISHED', 1, 100, JSON_ARRAY('促销', '兑换码', '双倍经验', 'MW4', '奖励'), 'MW4 魔爪能量饮料兑换码与奖励攻略 | 星火网游代练平台', 'MW4 魔爪能量饮料兑换码怎么获取、怎么兑换，六个奖励分别是什么，双倍经验上限和活动截止时间一次看懂。', 'https://wydlgf.cn/guides/mw4-monster-energy-codes', 0, NOW(), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON DUPLICATE KEY UPDATE title=VALUES(title), subtitle=VALUES(subtitle), summary=VALUES(summary), cover_image=VALUES(cover_image), difficulty=VALUES(difficulty), estimated_minutes=VALUES(estimated_minutes), language=VALUES(language), author_name=VALUES(author_name), source_name=VALUES(source_name), source_url=VALUES(source_url), status=VALUES(status), is_featured=VALUES(is_featured), sort_order=VALUES(sort_order), tags_json=VALUES(tags_json), seo_title=VALUES(seo_title), seo_description=VALUES(seo_description), canonical_url=VALUES(canonical_url), published_at=COALESCE(published_at, VALUES(published_at)), updated_at=CURRENT_TIMESTAMP, version=version+1;
SET @g1 := (SELECT id FROM game_guides WHERE slug = 'mw4-monster-energy-codes' LIMIT 1);
DELETE FROM guide_sections WHERE guide_id = @g1;
INSERT INTO guide_sections (guide_id, section_key, heading, body_markdown, image_url, image_alt, sort_order) VALUES (@g1, 'rules', '兑换码能拿到什么', '每个有效兑换码提供 **1 点积分 + 15 分钟双倍经验**。积分用于解锁奖励，前 6 点各对应一个奖励，之后继续累计双倍经验。

- 双倍经验每天最多累积 1 小时
- 单账号总上限约 40 小时，通常对应 160 个码
- 第 161 个码关联额外神秘物品，具体发放时间以活动公告为准', NULL, NULL, 10), (@g1, 'rewards', '六个奖励与兑换顺序', '奖励按积分顺序发放，不能跳级领取。前六点依次对应：

1. **Tagged** 大型贴花
2. **Green Light** 武器蓝图
3. **OG Green** 特战兵皮肤
4. **Canned** 武器蓝图
5. **Zero Chill** 特战兵皮肤
6. 隐藏神秘奖励，后续公布

如果只想要某一件外观，先确认它对应的积分档位，再决定要收集多少个码。', NULL, NULL, 20), (@g1, 'steps', '兑换步骤', '按下面的顺序操作，避免在错误的页面反复试码：

1. 打开官方活动页，输入罐底拉环下的码，或扫描罐身二维码
2. 活动页会返回一个 Call of Duty 兑换码
3. 登录 Activision 账号，在官方兑换页提交该码
4. 外观奖励进入 MW4 账号，双倍经验按规则累计

每个码只能成功兑换一次。公开流传的码通常会被第一个使用者占用，不要把它当成稳定来源。', NULL, NULL, 30), (@g1, 'window', '活动时间与地区限制', '活动从 2026 年 9 月 1 日开始，持续到 2027 年 3 月 31 日，兑换码有效期到 2027 年 4 月 30 日。

- 参与包装为 16 盎司的 Monster Energy Original、Zero Sugar 和 Zero Ultra
- 并非所有国家和地区都能在货架上买到活动包装
- 活动开始后，双倍经验可在部分现有使命召唤作品中先使用，未用完的部分可带入 MW4 多人模式

购买前先确认包装、地区与有效期，避免买到已经过期的码。', NULL, NULL, 40), (@g1, 'use-xp', '双倍经验怎么用更划算', '双倍经验本身不产生进度，关键是用在目标明确的地方：

- 账号等级和武器等级：越早用，越早解锁配件和配装空间
- 战斗通行证：赛季末补进度时价值最高
- 活动任务：和限时任务叠加时，能减少重复对局

活动码适合在正式开局或赛季冲刺阶段使用。平时先保留，等到有双倍经验活动或目标集中时再消耗。', NULL, NULL, 50), (@g1, 'safety', '注意事项', '官方兑换只在活动页和使命召唤兑换页完成，不需要提供账号密码、验证码或令牌。

- 不要从来路不明的页面提交登录信息
- 罐装码和 QR 码都只应通过官方页面兑换
- 活动日期与奖励以 Activision、Monster Energy 公告为准
- 站内只提供活动任务代练与咨询，不代替官方兑换', NULL, NULL, 60);
DELETE FROM guide_faqs WHERE guide_id = @g1;
INSERT INTO guide_faqs (guide_id, question, answer, sort_order) VALUES (@g1, '一个魔爪兑换码给什么？', '每个码提供 1 点积分和 15 分钟双倍经验，前 6 点依次解锁 6 个奖励。', 10), (@g1, '兑换码在包装的什么位置？', '多数在拉环下方，部分包装使用二维码，参与规格为 16 盎司的 Original、Zero Sugar 和 Zero Ultra。', 20), (@g1, '双倍经验可以留到 MW4 再用吗？', '可以。活动开始后可在部分现有使命召唤作品中使用，未用完的双倍经验可带入 MW4 多人模式。', 30), (@g1, '活动什么时候结束？', '活动持续到 2027 年 3 月 31 日，兑换码有效期到 2027 年 4 月 30 日。', 40);
DELETE FROM guide_products WHERE guide_id = @g1;
INSERT INTO guide_products (guide_id, service_id, label, description, image_url, is_featured, sort_order) VALUES (@g1, 3, '当前活动与限时任务', NULL, NULL, 1, 10), (@g1, 4, '其他任务 / 人工询价', NULL, NULL, 0, 20);

-- MW4 最佳设置：控制器、画面、音频和视野

INSERT INTO game_guides (game_code, slug, title, subtitle, summary, cover_image, difficulty, estimated_minutes, language, author_name, source_name, source_url, status, is_featured, sort_order, tags_json, seo_title, seo_description, canonical_url, view_count, published_at, created_at, updated_at, version) VALUES ('CODMW4', 'mw4-best-settings', 'MW4 最佳设置：控制器、画面、音频和视野', '手柄、鼠标键盘、画面、音频与 FOV 基线', '从手柄灵敏度、鼠标 DPI、PC 画面选项到音频和视野，给出一套可以先照着用、再按个人手感微调的 MW4 设置基线。', '/assets/covers/service/codmw4-4.jpg', 'BEGINNER', 8, 'zh-CN', '星火攻略组', 'UPBLAZED', 'https://upblazed.com/guides/mw4-best-settings', 'PUBLISHED', 1, 110, JSON_ARRAY('设置', '手柄', '画面', '音频', 'FOV'), 'MW4 最佳设置推荐：手柄、画面、音频与 FOV | 星火网游代练平台', 'MW4 设置怎么调？整理手柄灵敏度、瞄准曲线、鼠标 DPI、PC 画面帧数选项、音频混音和视野范围，给出可直接使用的基线。', 'https://wydlgf.cn/guides/mw4-best-settings', 0, NOW(), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON DUPLICATE KEY UPDATE title=VALUES(title), subtitle=VALUES(subtitle), summary=VALUES(summary), cover_image=VALUES(cover_image), difficulty=VALUES(difficulty), estimated_minutes=VALUES(estimated_minutes), language=VALUES(language), author_name=VALUES(author_name), source_name=VALUES(source_name), source_url=VALUES(source_url), status=VALUES(status), is_featured=VALUES(is_featured), sort_order=VALUES(sort_order), tags_json=VALUES(tags_json), seo_title=VALUES(seo_title), seo_description=VALUES(seo_description), canonical_url=VALUES(canonical_url), published_at=COALESCE(published_at, VALUES(published_at)), updated_at=CURRENT_TIMESTAMP, version=version+1;
SET @g2 := (SELECT id FROM game_guides WHERE slug = 'mw4-best-settings' LIMIT 1);
DELETE FROM guide_sections WHERE guide_id = @g2;
INSERT INTO guide_sections (guide_id, section_key, heading, body_markdown, image_url, image_alt, sort_order) VALUES (@g2, 'controller', '手柄设置', '手柄的核心是稳定跟枪和可重复的微调，建议先从一个中间值开始：

- 水平与垂直灵敏度：6 到 8，先试 7
- ADS 灵敏度倍率：1.00，如果开镜后抖动再降到 0.90
- 瞄准响应曲线：Dynamic 适合多数玩家，追求线性手感可换 Linear
- 摇杆死区：在漂移出现前尽量调低，最大死区保持较高
- 瞄准辅助：先从 Standard 开始，再根据跟枪习惯测试其他类型

新灵敏度至少用两次完整游戏再判断，频繁改动会让肌肉记忆一直不稳定。', NULL, NULL, 10), (@g2, 'mnk', '鼠标和键盘设置', '鼠标键盘看的是转身距离和一致性：

- 800 DPI 搭配游戏内 6 到 8，是常见起点
- 以一次舒适滑鼠垫滑动能转身约 180 度为准，再微调
- ADS 鼠标灵敏度使用 Relative，倍率 1.00
- 鼠标轮询率 1000 Hz
- 滑铲、飞扑、战术冲刺和蹲伏绑在能同时按到的位置

移动键位比额外宏更重要，先把能稳定操作的按键固定下来。', NULL, NULL, 20), (@g2, 'graphics', 'PC 画面设置：优先稳定帧数', '画面设置优先保证帧数稳定，再考虑观感：

- DLSS 或 FSR 设为 Quality，帧数不足时再降到 Balanced
- Nvidia Reflex 开启，并使用 Boost
- 光线追踪全部关闭，这是最明显的帧数开关
- 阴影质量低，关闭世界和武器动态模糊、景深、胶片颗粒
- 渲染分辨率保持 100，帧数不足时可先降到 85
- 显存目标约 80%，纹理质量按显存余量选择

稳定高帧比偶尔的高画质更有助于对枪。', NULL, NULL, 30), (@g2, 'audio-fov', '音频与视野', '音频负责信息，视野负责感知范围：

- 音频混音选 Headphones 或 Boost High，脚步声和换弹声会更靠前
- 音乐音量调低，避免盖住枪声和脚步
- FOV 建议 105 到 120，110 是中间值
- ADS 视野选 Independent，开镜时保持宽视野
- 武器视野按习惯选 Wide 或 Default

视野越大画面信息越多，但目标也会变小，需要在训练场里实际感受。', NULL, NULL, 40), (@g2, 'console', '主机与最终调整', 'PS5 和 Xbox Series X 如果显示设备支持，开启 120Hz 或性能模式。多人模式里帧数通常比分辨率更重要。

设置没有一套适合所有人。先用上面的值打两局，再只调整最影响手感的一两项；同时改灵敏度、视野和死区，很难判断是哪一项起了作用。', NULL, NULL, 50);
DELETE FROM guide_faqs WHERE guide_id = @g2;
INSERT INTO guide_faqs (guide_id, question, answer, sort_order) VALUES (@g2, 'MW4 手柄灵敏度多少合适？', '水平、垂直都在 6 到 8 之间是常见范围，先试 7，再按是否过冲或转身慢微调。', 10), (@g2, 'MW4 最佳 FOV 是多少？', '建议在 105 到 120 之间，110 是折中值；ADS 视野选 Independent。', 20), (@g2, '怎么设置更容易听到脚步？', '音频混音选 Headphones 或 Boost High，并把音乐音量调低。', 30), (@g2, 'PC 画面最影响帧数的选项是什么？', '光线追踪和多层阴影影响最大，优先关闭光线追踪并把阴影质量调低。', 40);
DELETE FROM guide_products WHERE guide_id = @g2;
INSERT INTO guide_products (guide_id, service_id, label, description, image_url, is_featured, sort_order) VALUES (@g2, 8, '排位 / 段位', NULL, NULL, 1, 10), (@g2, 1, '武器升级', NULL, NULL, 0, 20);

-- 如何在《使命召唤：现代战争4》中快速升级武器（以及何时应该直接购买）

INSERT INTO game_guides (game_code, slug, title, subtitle, summary, cover_image, difficulty, estimated_minutes, language, author_name, source_name, source_url, status, is_featured, sort_order, tags_json, seo_title, seo_description, canonical_url, view_count, published_at, created_at, updated_at, version) VALUES ('CODMW4', 'mw4-weapon-leveling-fast', '如何在《使命召唤：现代战争4》中快速升级武器（以及何时应该直接购买）', '武器经验来源、双倍经验窗口与购买边界', '武器等级决定配件和配装上限。这篇攻略解释武器经验从哪里来、如何叠倍率，以及什么情况下找人代肝比自己慢慢打更合适。', '/assets/covers/service/codmw4-1.jpg', 'BEGINNER', 7, 'zh-CN', '星火攻略组', 'UPBLAZED', 'https://upblazed.com/guides/how-to-level-up-weapons-fast-in-cod', 'PUBLISHED', 0, 120, JSON_ARRAY('武器升级', '配件解锁', '双倍经验', '效率'), 'MW4 武器快速升级攻略与代肝建议 | 星火网游代练平台', 'MW4 武器怎么升级最快，武器经验来源、双倍经验叠加、代肝适合什么情况，以及哪些玩家应该自己练枪。', 'https://wydlgf.cn/guides/mw4-weapon-leveling-fast', 0, NOW(), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON DUPLICATE KEY UPDATE title=VALUES(title), subtitle=VALUES(subtitle), summary=VALUES(summary), cover_image=VALUES(cover_image), difficulty=VALUES(difficulty), estimated_minutes=VALUES(estimated_minutes), language=VALUES(language), author_name=VALUES(author_name), source_name=VALUES(source_name), source_url=VALUES(source_url), status=VALUES(status), is_featured=VALUES(is_featured), sort_order=VALUES(sort_order), tags_json=VALUES(tags_json), seo_title=VALUES(seo_title), seo_description=VALUES(seo_description), canonical_url=VALUES(canonical_url), published_at=COALESCE(published_at, VALUES(published_at)), updated_at=CURRENT_TIMESTAMP, version=version+1;
SET @g3 := (SELECT id FROM game_guides WHERE slug = 'mw4-weapon-leveling-fast' LIMIT 1);
DELETE FROM guide_sections WHERE guide_id = @g3;
INSERT INTO guide_sections (guide_id, section_key, heading, body_markdown, image_url, image_alt, sort_order) VALUES (@g3, 'why', '武器等级为什么是门槛', '每把武器从基础版开始，瞄具、枪管、握把和完整配装都锁在等级后面。没有等级，很多打法根本组不出来。

武器升级不是可做可不做的支线，而是进入正式配装的前置条件。先确定要用的两三把主武器，再集中资源升级，比平均分配更快形成可用配装。', NULL, NULL, 10), (@g3, 'xp-source', '武器经验来自参与度', '只盯击杀会低估经验效率。占点、助攻、连杀和完成目标同样会提供武器经验，节奏快的目标模式通常比慢节奏模式收益更高。

- 目标模式：持续交战，经验来源稳定
- 击杀确认：击杀与拾取都能推进节奏
- 长时间架枪：击杀数可能不低，但每分钟经验偏低

想快速升级，优先选择能持续把你送回交战区的模式。', NULL, NULL, 20), (@g3, 'multipliers', '叠加倍率', '最有效的做法是等双倍武器经验窗口再消耗武器经验代币，而不是在普通时间单独使用。

- 双倍经验活动开启时集中升级主武器
- 代币留到活动叠加时使用
- 同一把武器连续打完，减少切换成本
- 优先升能通用的主武器，再处理特定模式武器

倍率叠加的核心是集中投入，而不是每天少量分散。', NULL, NULL, 30), (@g3, 'buy-or-not', '什么时候值得找人代肝', '适合代肝的情况是：等级只是挡住你使用完整配装，或者你在为排位、迷彩任务提前准备武器。

不适合代肝的情况也很明确：如果你享受一边升级一边熟悉后坐力和节奏，或者只差一把枪的最后一两个配件，自己打一个晚上更划算。

站内武器升级服务按目标等级和当前进度报价，下单前请说明平台、武器、目标等级和可操作时间。网页不收账号密码、验证码或令牌，具体交付方式先和客服确认。', NULL, NULL, 40), (@g3, 'checklist', '下单前准备', '为了让报价准确，先整理这些信息：

- 平台与区服
- 武器名称和当前等级
- 目标等级或目标配件
- 是否需要在某个时间窗口前完成
- 是否同时需要迷彩、账号等级或活动任务

信息越完整，越容易判断工作量，也越不容易出现重复报价。', NULL, NULL, 50);
DELETE FROM guide_faqs WHERE guide_id = @g3;
INSERT INTO guide_faqs (guide_id, question, answer, sort_order) VALUES (@g3, '快速升级武器最关键的是什么？', '选择高节奏目标模式，并把武器经验代币留到双倍武器经验窗口集中使用。', 10), (@g3, '只靠击杀能升级武器吗？', '能，但占点、助攻和连杀等参与行为同样提供经验，单纯架枪的效率通常更低。', 20), (@g3, '什么时候适合直接购买升级服务？', '当目标是一整套配装、排位准备或迷彩前置，而升级本身只是重复劳动时更合适。', 30), (@g3, '自己练枪的玩家应该买吗？', '如果你享受熟悉武器的过程，或者只差少量配件，自己打更合适。', 40);
DELETE FROM guide_products WHERE guide_id = @g3;
INSERT INTO guide_products (guide_id, service_id, label, description, image_url, is_featured, sort_order) VALUES (@g3, 1, '武器升级', NULL, NULL, 1, 10), (@g3, 2, '迷彩任务', NULL, NULL, 0, 20);

-- 杀戮连锁指南（MW4）：最佳枪械、装备搭配及地图解读

INSERT INTO game_guides (game_code, slug, title, subtitle, summary, cover_image, difficulty, estimated_minutes, language, author_name, source_name, source_url, status, is_featured, sort_order, tags_json, seo_title, seo_description, canonical_url, view_count, published_at, created_at, updated_at, version) VALUES ('CODMW4', 'mw4-kill-block-guide', '杀戮连锁指南（MW4）：最佳枪械、装备搭配及地图解读', 'Kill Block 模块地图、枪械区间与三套配装思路', 'Kill Block 会不断重组布局，固定背点位不再可靠。这篇攻略解释模块机制、不同距离的枪械选择、三套配装模板和轮转原则。', '/assets/covers/service/codmw4-6.jpg', 'INTERMEDIATE', 9, 'zh-CN', '星火攻略组', 'UPBLAZED', 'https://upblazed.com/guides/mw4-kill-block-map-guide-best-loadouts', 'PUBLISHED', 0, 130, JSON_ARRAY('地图', 'Kill Block', '配置', '枪械', '轮转'), 'MW4 Kill Block 杀戮连锁地图与配装攻略 | 星火网游代练平台', 'MW4 Kill Block 地图怎么打，模块重组机制、最佳枪械区间、三套配装模板、卡点与轮转思路一次整理。', 'https://wydlgf.cn/guides/mw4-kill-block-guide', 0, NOW(), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON DUPLICATE KEY UPDATE title=VALUES(title), subtitle=VALUES(subtitle), summary=VALUES(summary), cover_image=VALUES(cover_image), difficulty=VALUES(difficulty), estimated_minutes=VALUES(estimated_minutes), language=VALUES(language), author_name=VALUES(author_name), source_name=VALUES(source_name), source_url=VALUES(source_url), status=VALUES(status), is_featured=VALUES(is_featured), sort_order=VALUES(sort_order), tags_json=VALUES(tags_json), seo_title=VALUES(seo_title), seo_description=VALUES(seo_description), canonical_url=VALUES(canonical_url), published_at=COALESCE(published_at, VALUES(published_at)), updated_at=CURRENT_TIMESTAMP, version=version+1;
SET @g4 := (SELECT id FROM game_guides WHERE slug = 'mw4-kill-block-guide' LIMIT 1);
DELETE FROM guide_sections WHERE guide_id = @g4;
INSERT INTO guide_sections (guide_id, section_key, heading, body_markdown, image_url, image_alt, sort_order) VALUES (@g4, 'layout', '地图机制：一图多变', 'Kill Block 位于 West Bridge 高级军事训练设施，由两个 End Slab 和一个 Central Slab 组成。每局开始前，三块模块会重组为一个 Combo，首发版本包含 500 种以上组合。

三块模块连接后，整体规模接近小型到中型地图，交战频率高。构图可能偏向战壕近战，也可能出现屋顶和长距离对枪，因此不能用一张固定地图的思路去打。', NULL, NULL, 10), (@g4, 'read', '不能背固定点位，要读模块名', 'Kill Block 没有永久固定报点，因为地形本身会变化。可学习的部分是单个 Slab：记住每块模块的通道、掩体和接战距离，开局时观察模块名称，就能更快判断这一局的结构。

- 开局先看模块组合，再决定第一波走位
- 每局重新确认通道和卡点，不要自动驾驶到上一局的位置
- 将每个 Slab 当成独立小地图来学习
- 模块连接处通常是天然瓶颈，进入前先预瞄

地图奖励读信息的人，不奖励只靠记忆的人。', NULL, NULL, 20), (@g4, 'guns', '按距离选择枪械', '与其找一把绝对最强的枪，不如按这局可能出现的交战距离准备：

- 长通道、屋顶和开阔区：中低后坐力突击步枪或战术步枪
- 战壕、室内和近身转角：冲锋枪或霰弹枪
- 不确定组合时的默认选择：均衡突击步枪或中距离冲锋枪
- 副武器：高伤害手枪在处理换弹和被贴脸时更稳

默认配装要能覆盖中距离，极端距离的专用枪留给副配置。', NULL, NULL, 30), (@g4, 'templates', '三套配装思路', '在正式配装数值稳定前，先按角色搭建：

1. **全能组**：均衡突击步枪，后坐力优先；副武器选高伤害手枪，特长偏向移动或 ADS 速度
2. **突击组**：机动冲锋枪，强化冲刺转开火、腰射和 ADS 速度；副武器可带霰弹枪
3. **架点组**：低后坐力步枪或战术步枪，负责长线和屋顶；副武器带冲锋枪，防止战局突然转入近战

附件名称和数值会随版本调整，先固定角色分工，再根据版本补齐具体配件。', NULL, NULL, 40), (@g4, 'rotation', '轮转与常见错误', 'Kill Block 的胜负很大程度取决于是否愿意重新侦察：

- 中央模块适合争取地图控制，两端模块更适合围绕目标点打
- 模块连接处是高风险瓶颈，穿过前先确认信息
- 每次架枪都检查后方，上一局安全的路线这一局可能已经打开
- 不要在开局前几秒冲刺到记忆里的架点
- 不要每局都用同一种极端配装

最常见的错误是用旧地图的习惯打新组合。慢两秒确认结构，通常比多跑十秒更有价值。', NULL, NULL, 50);
DELETE FROM guide_faqs WHERE guide_id = @g4;
INSERT INTO guide_faqs (guide_id, question, answer, sort_order) VALUES (@g4, 'Kill Block 是什么地图？', '它是 MW4 的模块化多人地图，由三个 Slab 重组出 500 种以上组合，每局结构可能变化。', 10), (@g4, 'Kill Block 最佳枪械是哪把？', '没有固定唯一答案。开阔组合优先步枪和战术步枪，近战组合优先冲锋枪和霰弹枪，默认配装选全能型。', 20), (@g4, 'Kill Block 可以背报点吗？', '不能完全背固定报点，但可以学习每块 Slab 的通道和掩体，再根据开局模块名称调整。', 30), (@g4, 'Kill Block 最容易犯的错误是什么？', '按上一局的点位自动驾驶、使用过度专用的配装，以及忽略模块重组后的侧翼变化。', 40);
DELETE FROM guide_products WHERE guide_id = @g4;
INSERT INTO guide_products (guide_id, service_id, label, description, image_url, is_featured, sort_order) VALUES (@g4, 1, '武器升级', NULL, NULL, 1, 10), (@g4, 8, '排位 / 段位', NULL, NULL, 0, 20);

-- MW4 精通迷彩：所有多人游戏、战区和 DMZ 迷彩

INSERT INTO game_guides (game_code, slug, title, subtitle, summary, cover_image, difficulty, estimated_minutes, language, author_name, source_name, source_url, status, is_featured, sort_order, tags_json, seo_title, seo_description, canonical_url, view_count, published_at, created_at, updated_at, version) VALUES ('CODMW4', 'mw4-mastery-camos', 'MW4 精通迷彩：所有多人游戏、战区和 DMZ 迷彩', '九款精通迷彩、Pyroclast 系列与当前确认信息', 'MW4 的迷彩进度横跨多人、战区和 DMZ，共有九款精通迷彩和一套 Pyroclast 系列。这篇攻略整理名单、模式归属和尚未确认的部分。', '/assets/covers/service/codmw4-2.jpg', 'INTERMEDIATE', 8, 'zh-CN', '星火攻略组', 'UPBLAZED', 'https://upblazed.com/guides/mw4-mastery-camos', 'PUBLISHED', 1, 140, JSON_ARRAY('迷彩', '精通迷彩', '多人', '战区', 'DMZ', 'Pyroclast'), 'MW4 精通迷彩名单与解锁攻略 | 星火网游代练平台', 'MW4 精通迷彩有哪些？九款多人、战区和 DMZ 迷彩名单、Pyroclast 系列、当前已确认与未确认信息一次整理。', 'https://wydlgf.cn/guides/mw4-mastery-camos', 0, NOW(), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON DUPLICATE KEY UPDATE title=VALUES(title), subtitle=VALUES(subtitle), summary=VALUES(summary), cover_image=VALUES(cover_image), difficulty=VALUES(difficulty), estimated_minutes=VALUES(estimated_minutes), language=VALUES(language), author_name=VALUES(author_name), source_name=VALUES(source_name), source_url=VALUES(source_url), status=VALUES(status), is_featured=VALUES(is_featured), sort_order=VALUES(sort_order), tags_json=VALUES(tags_json), seo_title=VALUES(seo_title), seo_description=VALUES(seo_description), canonical_url=VALUES(canonical_url), published_at=COALESCE(published_at, VALUES(published_at)), updated_at=CURRENT_TIMESTAMP, version=version+1;
SET @g5 := (SELECT id FROM game_guides WHERE slug = 'mw4-mastery-camos' LIMIT 1);
DELETE FROM guide_sections WHERE guide_id = @g5;
INSERT INTO guide_sections (guide_id, section_key, heading, body_markdown, image_url, image_alt, sort_order) VALUES (@g5, 'structure', '迷彩进度结构', 'MW4 使用共享迷彩进度，多人、战区和 DMZ 的武器进度互相连接，之后开放精通迷彩。

- 精通迷彩共九款，每个模式三款
- 另有四款 Pyroclast 迷彩，属于独立系列
- 迷彩名称来自测试阶段展示，具体挑战尚未全部公布
- 每款迷彩的上下层级和最终顶阶还未完全确认

先按模式准备武器等级，再等官方公布具体挑战，会比盲目刷更有效。', NULL, NULL, 10), (@g5, 'mp-wz', '多人模式与战区迷彩', '多人模式展示的三款：

- **Mercurial Drift**：MW4 新迷彩
- **Polyatomic Reforged**：MW2 经典名称的 MW4 重制版
- **Orion Reforged**：MW2 经典名称的 MW4 重制版

战区展示的三款：

- **Parallax**：MW4 新战区迷彩
- **Damascus Reforged**：MW 2019 经典名称，本次归在战区
- **Empyros**：MW4 新战区迷彩

重制名称不代表沿用旧解锁条件，MW4 会使用自己的迷彩系统。', NULL, NULL, 20), (@g5, 'dmz', 'DMZ 与 Pyroclast 系列', 'DMZ 在 MW4 中作为完整模式回归，并拥有独立迷彩：

- **Chiral**
- **Ripple Tide**
- **Helio**

Pyroclast 系列包括 **Pyroclast**、**Pyroclast Verge**、**Pyroclast Scatter** 和 **Pyroclast Cascade**，属于声望相关的独立路线，不是九款精通迷彩的一部分。具体是武器声望还是账号声望解锁，目前尚未公布。', NULL, NULL, 30), (@g5, 'confirmed', '目前确认与未确认', '已经确认的内容：

- 九款精通迷彩按多人、战区、DMZ 各三款划分
- Pyroclast 是独立的四款系列
- 首发共有 33 把武器，其中 22 把已公布名称
- 测试阶段展示了迷彩名称，但无法完成完整迷彩进度
- 测试进度不会继承，正式迷彩从 2026 年 10 月 23 日游戏上线后开始

尚未确认的内容：每款迷彩的具体挑战、各模式的最顶阶迷彩，以及 Pyroclast 的声望解锁条件。', NULL, NULL, 40), (@g5, 'prepare', '上线前的准备建议', '迷彩挑战公布前，可以先做这些准备：

- 集中升级计划使用的武器，减少上线后的等待时间
- 确认自己要打多人、战区还是 DMZ，提前熟悉模式节奏
- 关注官方公告，不要照搬旧作的迷彩条件
- 如果想节省时间，可先咨询站内迷彩任务服务，等挑战公布后再确认工作量

本站迷彩任务按剩余挑战和目标模式报价。网页不收账号密码、验证码或令牌，交付方式请先与客服确认。', NULL, NULL, 50);
DELETE FROM guide_faqs WHERE guide_id = @g5;
INSERT INTO guide_faqs (guide_id, question, answer, sort_order) VALUES (@g5, 'MW4 一共有多少款精通迷彩？', '九款精通迷彩，每个模式三款，另有四款 Pyroclast 声望系列迷彩。', 10), (@g5, '每款迷彩的具体解锁条件公布了吗？', '尚未全部公布。测试阶段只展示了名称，具体挑战和层级仍待官方确认。', 20), (@g5, 'Damascus Reforged 属于哪个模式？', '本次展示在战区，不是多人模式。', 30), (@g5, '测试阶段的迷彩进度会继承吗？', '不会，正式迷彩进度从游戏上线后开始。', 40);
DELETE FROM guide_products WHERE guide_id = @g5;
INSERT INTO guide_products (guide_id, service_id, label, description, image_url, is_featured, sort_order) VALUES (@g5, 2, '迷彩任务', NULL, NULL, 1, 10), (@g5, 1, '武器升级', NULL, NULL, 0, 20);

-- MW4 排位赛：目前已确认的内容（以及尚未确认的内容）

INSERT INTO game_guides (game_code, slug, title, subtitle, summary, cover_image, difficulty, estimated_minutes, language, author_name, source_name, source_url, status, is_featured, sort_order, tags_json, seo_title, seo_description, canonical_url, view_count, published_at, created_at, updated_at, version) VALUES ('CODMW4', 'mw4-ranked-play', 'MW4 排位赛：目前已确认的内容（以及尚未确认的内容）', '已确认的多人内容、排位现状与上线前准备', 'MW4 已公布发售平台、首发地图和多人机制，但排位系统仍没有官方细节。这篇攻略区分已确认信息与合理推测，避免被网络传言带偏。', '/assets/covers/service/codmw4-6.jpg', 'BEGINNER', 6, 'zh-CN', '星火攻略组', 'UPBLAZED', 'https://upblazed.com/guides/mw4-ranked-system-what-we-know', 'PUBLISHED', 0, 150, JSON_ARRAY('排位', 'SR', '赛季', '多人'), 'MW4 排位赛已确认内容与上线前准备 | 星火网游代练平台', 'MW4 排位赛有哪些确认信息？发售平台、首发地图、多人机制、排位系统现状以及上线前可以准备什么一次说明。', 'https://wydlgf.cn/guides/mw4-ranked-play', 0, NOW(), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON DUPLICATE KEY UPDATE title=VALUES(title), subtitle=VALUES(subtitle), summary=VALUES(summary), cover_image=VALUES(cover_image), difficulty=VALUES(difficulty), estimated_minutes=VALUES(estimated_minutes), language=VALUES(language), author_name=VALUES(author_name), source_name=VALUES(source_name), source_url=VALUES(source_url), status=VALUES(status), is_featured=VALUES(is_featured), sort_order=VALUES(sort_order), tags_json=VALUES(tags_json), seo_title=VALUES(seo_title), seo_description=VALUES(seo_description), canonical_url=VALUES(canonical_url), published_at=COALESCE(published_at, VALUES(published_at)), updated_at=CURRENT_TIMESTAMP, version=version+1;
SET @g6 := (SELECT id FROM game_guides WHERE slug = 'mw4-ranked-play' LIMIT 1);
DELETE FROM guide_sections WHERE guide_id = @g6;
INSERT INTO guide_sections (guide_id, section_key, heading, body_markdown, image_url, image_alt, sort_order) VALUES (@g6, 'confirmed', '已确认的发售信息', 'MW4 在 2026 年 10 月 23 日发售，登陆 PS5、Xbox Series X|S、PC，并首次登陆 Nintendo Switch 2，不支持 PS4 和 Xbox One。

- 开发方：Infinity Ward
- 首发模式：战役、多人、DMZ
- 背景设定：韩国
- 数字预购可获得 2026 年 10 月 16 日开始的战役抢先体验', NULL, NULL, 10), (@g6, 'multiplayer', '已确认的多人内容', '首发多人内容比预期更完整：

- 12 张全新 6v6 核心地图
- 独立的 Gunfight 地图
- 支持步兵与载具的大战场地图
- Kill Block 模块化地图，包含 500 种以上组合
- 取消腰射扩散，重做移动、Create-a-Class 和剧场模式
- 不回归 Omnimovement

这些是官方已经公布的内容，可以作为上线前练习重点。', NULL, NULL, 20), (@g6, 'ranked-status', '排位系统现状', '目前 Activision 没有公布 MW4 排位系统的具体细节，包括 SR 规则、段位层级、定级结构和奖励。任何直接给出完整段位表的内容，都应先核对官方来源。

根据近十年的使命召唤排位模式，可以合理预期：定级阶段、SR 阶梯、赛季重置和中段位长期爬分。但预期不等于官方信息，正式规则公布后应以官方为准。', NULL, NULL, 30), (@g6, 'prepare', '上线前能准备什么', '排位规则未定，不代表只能等：

- 预购数字版，提前体验战役并在首发期熟悉枪械手感
- 先把计划使用的武器升到可配装等级
- 用最佳设置和 Kill Block 指南熟悉基础操作与地图思路
- 想提前安排排位服务时，先说明平台、目标与时间窗口，等官方公布规则后再确认具体方案

站内排位服务按目标段位和当前进度确认工作量，网页不收集账号密码、验证码或令牌。', NULL, NULL, 40);
DELETE FROM guide_faqs WHERE guide_id = @g6;
INSERT INTO guide_faqs (guide_id, question, answer, sort_order) VALUES (@g6, 'MW4 什么时候发售？', '2026 年 10 月 23 日，登陆 PS5、Xbox Series X|S、PC 和 Nintendo Switch 2。', 10), (@g6, 'MW4 排位系统公布了吗？', '尚未公布。SR、段位、定级和奖励等细节仍没有官方信息。', 20), (@g6, '上线前可以准备什么？', '预购数字版、升级常用武器、熟悉设置和地图，并等官方公布排位规则后再确认代练方案。', 30), (@g6, '首发有多少张多人地图？', '12 张全新 6v6 核心地图，另有 Gunfight 和大战场地图。', 40);
DELETE FROM guide_products WHERE guide_id = @g6;
INSERT INTO guide_products (guide_id, service_id, label, description, image_url, is_featured, sort_order) VALUES (@g6, 8, '排位 / 段位', NULL, NULL, 1, 10), (@g6, 1, '武器升级', NULL, NULL, 0, 20);

-- K/D 值提升机制：每局最低值，而不是平均值

INSERT INTO game_guides (game_code, slug, title, subtitle, summary, cover_image, difficulty, estimated_minutes, language, author_name, source_name, source_url, status, is_featured, sort_order, tags_json, seo_title, seo_description, canonical_url, view_count, published_at, created_at, updated_at, version) VALUES ('CODMW4', 'mw4-kd-explained', 'K/D 值提升机制：每局最低值，而不是平均值', '最低 K/D、判定方式与真实提升边界', 'K/D 提升常被误解成把生涯平均值刷到某个数字。实际服务通常按每局最低值计算。这篇攻略解释判定方式、模式范围和现实限制。', '/assets/covers/service/codmw4-6.jpg', 'INTERMEDIATE', 7, 'zh-CN', '星火攻略组', 'UPBLAZED', 'https://upblazed.com/guides/kd-boosting-explained', 'PUBLISHED', 0, 160, JSON_ARRAY('K/D', '数据', '对局', '排位'), 'MW4 K/D 提升机制：每局最低值怎么计算 | 星火网游代练平台', 'MW4 K/D 提升按每局最低值还是生涯平均值？判定方式、模式范围、漏单补偿和生涯数据变化的现实限制一次讲清。', 'https://wydlgf.cn/guides/mw4-kd-explained', 0, NOW(), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON DUPLICATE KEY UPDATE title=VALUES(title), subtitle=VALUES(subtitle), summary=VALUES(summary), cover_image=VALUES(cover_image), difficulty=VALUES(difficulty), estimated_minutes=VALUES(estimated_minutes), language=VALUES(language), author_name=VALUES(author_name), source_name=VALUES(source_name), source_url=VALUES(source_url), status=VALUES(status), is_featured=VALUES(is_featured), sort_order=VALUES(sort_order), tags_json=VALUES(tags_json), seo_title=VALUES(seo_title), seo_description=VALUES(seo_description), canonical_url=VALUES(canonical_url), published_at=COALESCE(published_at, VALUES(published_at)), updated_at=CURRENT_TIMESTAMP, version=version+1;
SET @g7 := (SELECT id FROM game_guides WHERE slug = 'mw4-kd-explained' LIMIT 1);
DELETE FROM guide_sections WHERE guide_id = @g7;
INSERT INTO guide_sections (guide_id, section_key, heading, body_markdown, image_url, image_alt, sort_order) VALUES (@g7, 'floor', '最低值不是平均值', 'K/D 提升通常由指定目标决定每局最低值，例如 2.0、3.0、4.0 或 5.0。每局结束都必须达到或超过该值，而不是几局平均值达标。

- 一场高数据对局不能抵消一场低数据对局
- 超过最低值的部分仍然计入结果
- 未达到最低值的对局通常需要额外补打
- 目标越高，单局难度和报价通常越高', NULL, NULL, 10), (@g7, 'measure', '如何判定一局', '判定依据是游戏结算面板显示的击杀与死亡数据，而不是第三方截图。连杀奖励产生的击杀按游戏结算方式计入。

服务过程中应提供每局结算截图，让用户自己核对，而不是只给最终总结。对局记录不会被删除；未达标对局会保留在对局历史里，补打是在原有对局之外额外进行。', NULL, NULL, 20), (@g7, 'modes', '可用模式范围', 'MW4 正式发售前，K/D 服务只能基于核心多人模式。

- MW4 核心多人：短局、快复活，但匹配会持续调整对手强度
- 战区 Resurgence：地图较小、复活快、交战密度高
- 战区大逃杀：单局时间长、变量多，保持最低值更难，成本通常更高
- 排位和夺金模式通常不属于 K/D 服务范围

不同模式的工作量和风险不同，不能用同一价格简单换算。', NULL, NULL, 30), (@g7, 'lifetime', '生涯 K/D 能提升多少', '生涯 K/D 是所有历史对局的平均值。新账号对局少，变化更快；有几千局历史的老账号，同样局数带来的变化会小很多。

因此，服务通常按每局最低值计算，而不是承诺把生涯 K/D 提升到固定数字。下单前提供当前 K/D 和对局数，才能得到相对可靠的局数估算。', NULL, NULL, 40), (@g7, 'risk', '风险与边界', 'K/D 提升通常需要代打，也就是由他人登录账号操作。任何代打都存在账号安全、平台规则和处罚风险，没有商家能承诺绝对安全。

本站不在网页收集账号密码、验证码或令牌。是否采用代打、如何交付、账号风险由用户自行判断。如果只想让数据立刻明显变化，或者你本来就会大量自己打，短单可能并不划算，先咨询再决定。', NULL, NULL, 50);
DELETE FROM guide_faqs WHERE guide_id = @g7;
INSERT INTO guide_faqs (guide_id, question, answer, sort_order) VALUES (@g7, '最低 K/D 是什么意思？', '每局结束都必须达到指定数值，例如 3.0 表示每局至少三击杀一死亡，而不是看多局平均。', 10), (@g7, '未达到最低值怎么办？', '通常补打一局，未达标对局仍保留在对局历史里。', 20), (@g7, '哪些模式可以提升 K/D？', 'MW4 核心多人模式；战区可选 Resurgence 或大逃杀，排位和夺金通常不在范围内。', 30), (@g7, '生涯 K/D 会提升多少？', '取决于账号已有对局数量。新账号变化快，老账号需要更多局数才有明显变化。', 40);
DELETE FROM guide_products WHERE guide_id = @g7;
INSERT INTO guide_products (guide_id, service_id, label, description, image_url, is_featured, sort_order) VALUES (@g7, 8, '排位 / 段位', NULL, NULL, 1, 10), (@g7, 4, '其他任务 / 人工询价', NULL, NULL, 0, 20);

-- 如何在《使命召唤：现代战争4》中获得核弹（30 连杀）

INSERT INTO game_guides (game_code, slug, title, subtitle, summary, cover_image, difficulty, estimated_minutes, language, author_name, source_name, source_url, status, is_featured, sort_order, tags_json, seo_title, seo_description, canonical_url, view_count, published_at, created_at, updated_at, version) VALUES ('CODMW4', 'mw4-nuke-guide', '如何在《使命召唤：现代战争4》中获得核弹（30 连杀）', '30 连杀条件、模式选择与三阶段打法', '核弹要求单局 30 连杀，死亡一次就重置。这篇攻略拆解计入方式、适合模式、配装思路和 1 到 30 杀的节奏安排。', '/assets/covers/service/codmw4-6.jpg', 'ADVANCED', 9, 'zh-CN', '星火攻略组', 'UPBLAZED', 'https://upblazed.com/guides/how-to-get-a-nuke', 'PUBLISHED', 1, 170, JSON_ARRAY('核弹', '30连杀', '连杀奖励', '多人'), 'MW4 核弹怎么获得：30 连杀攻略与模式推荐 | 星火网游代练平台', 'MW4 核弹需要 30 连杀，死亡一次重置。整理计入规则、最佳模式、配装思路、三阶段打法和常见失败原因。', 'https://wydlgf.cn/guides/mw4-nuke-guide', 0, NOW(), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON DUPLICATE KEY UPDATE title=VALUES(title), subtitle=VALUES(subtitle), summary=VALUES(summary), cover_image=VALUES(cover_image), difficulty=VALUES(difficulty), estimated_minutes=VALUES(estimated_minutes), language=VALUES(language), author_name=VALUES(author_name), source_name=VALUES(source_name), source_url=VALUES(source_url), status=VALUES(status), is_featured=VALUES(is_featured), sort_order=VALUES(sort_order), tags_json=VALUES(tags_json), seo_title=VALUES(seo_title), seo_description=VALUES(seo_description), canonical_url=VALUES(canonical_url), published_at=COALESCE(published_at, VALUES(published_at)), updated_at=CURRENT_TIMESTAMP, version=version+1;
SET @g8 := (SELECT id FROM game_guides WHERE slug = 'mw4-nuke-guide' LIMIT 1);
DELETE FROM guide_sections WHERE guide_id = @g8;
INSERT INTO guide_sections (guide_id, section_key, heading, body_markdown, image_url, image_alt, sort_order) VALUES (@g8, 'rules', '核弹条件', 'MW4 多人核弹需要单局连续击杀 30 人，中途死亡一次计数归零。

- 枪械、投掷物和战地升级产生的击杀计入
- 其他连杀奖励产生的击杀不计入核弹进度
- UAV 等侦察型连杀奖励虽然不加计数，但能帮助判断下一个交战点
- 完整连杀奖励、特长和可用模式仍以正式上线后的官方列表为准', NULL, NULL, 10), (@g8, 'modes', '模式选择', '核弹需要稳定的击杀流量，同时不能被迫频繁移动：

- 团队死斗和击杀确认：小地图、复活快、交火密度高
- 占领模式：守住已有据点，让对手主动进入你的火力范围
- 热点模式：前 15 杀节奏很好，后段多角度压力较大
- 避免枪战和大战场：前者人数不足以凑齐 30 杀，后者地图过大，交火不可控', NULL, NULL, 20), (@g8, 'loadout', '配装思路', '正式版本数值未稳定前，先按角色配装，不要照抄未经验证的配件表：

- 主武器：能覆盖中远距离的稳定步枪或长枪管冲锋枪
- 副武器：值得信任的高伤害手枪，处理换弹时被贴脸的情况
- 投掷物：致命装备清架点，战术装备处理不敢直接冲的拐角
- 特长：优先反侦察和生存能力，而不是单纯堆移动速度

核弹不是单点爆发，而是 30 次稳定决策的结果。', NULL, NULL, 30), (@g8, 'stages', '1 到 30 杀的节奏', '把整局拆成三个阶段：

1. **1 到 10 杀**：正常进攻，主动接好打的交火，尽快建立连杀奖励
2. **11 到 20 杀**：放慢节奏，利用掩体接战，在墙后换弹，提前预瞄通道
3. **21 到 30 杀**：选一到两个入口的架点，让对手来找你，只在复活点翻转时移动

有 UAV 时先放 UAV 再进入后段。不要追一个残血目标跑进未知区域，能避开的 1 对 2 尽量避开。', NULL, NULL, 40), (@g8, 'mistakes', '为什么多数核弹死在 20 杀之后', '失败往往不是枪法问题，而是进度接近目标后开始加速：

- 为了快点结束而穿越开阔地
- 觉得连杀安全，忽略侧翼和后方
- 在空地换弹，不用副武器过渡
- 不追踪复活点，把敌人刷在身后
- 不复盘击杀回放，重复同一个被击杀角度

越接近 30 杀，越要降低移动欲望。战区核弹是另一套机制，需要连续赢下多局并完成冠军任务，不能和多人 30 连杀混为一谈。', NULL, NULL, 50);
DELETE FROM guide_faqs WHERE guide_id = @g8;
INSERT INTO guide_faqs (guide_id, question, answer, sort_order) VALUES (@g8, 'MW4 核弹需要多少连杀？', '单局 30 连杀，死亡一次计数归零。', 10), (@g8, '连杀奖励的击杀计入核弹吗？', '其他连杀奖励的击杀不计入，只有枪械、投掷物和战地升级等击杀计入。', 20), (@g8, '哪个模式最适合打核弹？', '小地图团队死斗和击杀确认最适合，占领模式适合守点打法。', 30), (@g8, '战区核弹和多人核弹一样吗？', '不一样。战区核弹属于冠军任务，需要连续多局胜利并完成组装和引爆流程。', 40);
DELETE FROM guide_products WHERE guide_id = @g8;
INSERT INTO guide_products (guide_id, service_id, label, description, image_url, is_featured, sort_order) VALUES (@g8, 8, '排位 / 段位', NULL, NULL, 1, 10), (@g8, 4, '其他任务 / 人工询价', NULL, NULL, 0, 20);

-- 使命召唤账号等级详解：军衔、声望以及人们为何要刷等级

INSERT INTO game_guides (game_code, slug, title, subtitle, summary, cover_image, difficulty, estimated_minutes, language, author_name, source_name, source_url, status, is_featured, sort_order, tags_json, seo_title, seo_description, canonical_url, view_count, published_at, created_at, updated_at, version) VALUES ('CODMW4', 'cod-account-leveling', '使命召唤账号等级详解：军衔、声望以及人们为何要刷等级', '账号等级解锁内容、声望机制与效率建议', '账号等级决定基础武器、装备、特长和配装槽位的开放速度，声望则是在满级后重置显示等级换取奖励。这篇攻略解释两者区别和升级时机。', '/assets/covers/service/codmw4-1.jpg', 'BEGINNER', 6, 'zh-CN', '星火攻略组', 'UPBLAZED', 'https://upblazed.com/guides/cod-account-leveling-explained', 'PUBLISHED', 0, 180, JSON_ARRAY('账号等级', '声望', '军衔', '解锁'), '使命召唤 MW4 账号等级与声望机制攻略 | 星火网游代练平台', 'MW4 账号等级解锁什么、声望是什么、为什么新账号升级更急迫，以及双倍经验和效率升级的实用建议。', 'https://wydlgf.cn/guides/cod-account-leveling', 0, NOW(), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON DUPLICATE KEY UPDATE title=VALUES(title), subtitle=VALUES(subtitle), summary=VALUES(summary), cover_image=VALUES(cover_image), difficulty=VALUES(difficulty), estimated_minutes=VALUES(estimated_minutes), language=VALUES(language), author_name=VALUES(author_name), source_name=VALUES(source_name), source_url=VALUES(source_url), status=VALUES(status), is_featured=VALUES(is_featured), sort_order=VALUES(sort_order), tags_json=VALUES(tags_json), seo_title=VALUES(seo_title), seo_description=VALUES(seo_description), canonical_url=VALUES(canonical_url), published_at=COALESCE(published_at, VALUES(published_at)), updated_at=CURRENT_TIMESTAMP, version=version+1;
SET @g9 := (SELECT id FROM game_guides WHERE slug = 'cod-account-leveling' LIMIT 1);
DELETE FROM guide_sections WHERE guide_id = @g9;
INSERT INTO guide_sections (guide_id, section_key, heading, body_markdown, image_url, image_alt, sort_order) VALUES (@g9, 'unlocks', '账号等级解锁什么', '账号等级来自总经验，无论胜负每局都会累积。等级提升会逐步开放：

- 基础武器与装备
- 特长和战地升级
- Create-a-Class 配装槽位
- 后续赛季的等级相关奖励

低等级账号只能用一小部分工具，恰好也是最需要完整配装的阶段。提前升级可以缩短这段受限期。', NULL, NULL, 10), (@g9, 'prestige', '声望是什么', '账号达到等级上限后，可以进入声望阶段。声望会重置显示等级，但不会拿走已经获得的核心内容，并通常提供新的徽章、奖励和长期目标。

简单说，账号等级是解锁工具，声望是展示投入时间的长期进度。两者相关，但不是同一件事。', NULL, NULL, 20), (@g9, 'when', '什么情况值得推进等级', '适合集中升级的情况：

- 新账号或回归账号缺少常用武器、特长和配装槽位
- 想尽快进入排位、战区或迷彩任务的前置准备
- 想要声望奖励，但不想用几十个小时重复对局

如果你是刚开始学习地图、枪法和移动的新玩家，早期对局也是建立基础的过程，直接跳过等级并不会自动提升技术。', NULL, NULL, 30), (@g9, 'efficiency', '效率升级建议', '账号经验来自每一局，但不同模式的单位时间经验不同：

- 高节奏目标模式通常比慢节奏模式更快
- 双倍经验活动期间集中游玩，效率最高
- 魔爪活动等双倍经验来源可提前累积
- 先完成每日和周常挑战，再按目标模式推进
- 固定两到三套配装，减少反复调整时间

账号等级只代表可用工具更多，不代表技术水平同步提升。', NULL, NULL, 40), (@g9, 'service', '下单前准备', '如果想安排账号等级服务，先整理：

- 当前等级和目标等级
- 平台、区服和常用模式
- 是否同时需要武器等级或活动任务
- 期望完成时间

站内相关服务按目标进度报价。网页不收账号密码、验证码或令牌，具体交付方式请在咨询时确认。', NULL, NULL, 50);
DELETE FROM guide_faqs WHERE guide_id = @g9;
INSERT INTO guide_faqs (guide_id, question, answer, sort_order) VALUES (@g9, '账号等级解锁什么？', '基础武器、装备、特长、战地升级和 Create-a-Class 配装槽位等。', 10), (@g9, '账号等级和声望有什么区别？', '等级是总经验进度，声望是满级后的长期重置与奖励系统，不会拿走已获得的核心内容。', 20), (@g9, '升级账号会让人变强吗？', '等级提升会增加可用工具，但不会自动提升地图理解、枪法和移动技术。', 30), (@g9, '怎样升级效率更高？', '打高节奏目标模式，把双倍经验留到集中游玩时使用，并优先完成每日和周常挑战。', 40);
DELETE FROM guide_products WHERE guide_id = @g9;
INSERT INTO guide_products (guide_id, service_id, label, description, image_url, is_featured, sort_order) VALUES (@g9, 4, '其他任务 / 人工询价', NULL, NULL, 1, 10), (@g9, 1, '武器升级', NULL, NULL, 0, 20);

-- 战斗通行证加速真的值得吗？一份诚实的分析

INSERT INTO game_guides (game_code, slug, title, subtitle, summary, cover_image, difficulty, estimated_minutes, language, author_name, source_name, source_url, status, is_featured, sort_order, tags_json, seo_title, seo_description, canonical_url, view_count, published_at, created_at, updated_at, version) VALUES ('CODMW4', 'mw4-battle-pass-value', '战斗通行证加速真的值得吗？一份诚实的分析', '通行证机制、价值误区与加速时机', '战斗通行证加速本质上是买时间，而不是买奖励。这篇攻略解释通行证到期规则、价值计算和什么时间点才值得补进度。', '/assets/covers/service/codmw4-3.jpg', 'BEGINNER', 6, 'zh-CN', '星火攻略组', 'UPBLAZED', 'https://upblazed.com/guides/is-a-battle-pass-boost-worth-it', 'PUBLISHED', 0, 190, JSON_ARRAY('战斗通行证', '赛季', '等级', '奖励'), 'MW4 战斗通行证加速值得吗？赛季末补进度攻略 | 星火网游代练平台', '战斗通行证加速值不值得买？通行证到期规则、价值误区、赛季末补进度时机和效率建议一次说明。', 'https://wydlgf.cn/guides/mw4-battle-pass-value', 0, NOW(), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON DUPLICATE KEY UPDATE title=VALUES(title), subtitle=VALUES(subtitle), summary=VALUES(summary), cover_image=VALUES(cover_image), difficulty=VALUES(difficulty), estimated_minutes=VALUES(estimated_minutes), language=VALUES(language), author_name=VALUES(author_name), source_name=VALUES(source_name), source_url=VALUES(source_url), status=VALUES(status), is_featured=VALUES(is_featured), sort_order=VALUES(sort_order), tags_json=VALUES(tags_json), seo_title=VALUES(seo_title), seo_description=VALUES(seo_description), canonical_url=VALUES(canonical_url), published_at=COALESCE(published_at, VALUES(published_at)), updated_at=CURRENT_TIMESTAMP, version=version+1;
SET @g10 := (SELECT id FROM game_guides WHERE slug = 'mw4-battle-pass-value' LIMIT 1);
DELETE FROM guide_sections WHERE guide_id = @g10;
INSERT INTO guide_sections (guide_id, section_key, heading, body_markdown, image_url, image_alt, sort_order) VALUES (@g10, 'how', '通行证机制', '战斗通行证是一条约一个赛季的奖励轨道，通过游戏获得通行证经验并逐级解锁。购买后，达到对应等级即可领取武器蓝图、外观和部分返还货币。

通行证最大的限制是时间。赛季结束后，未达到的等级会关闭，已经购买但没解锁的奖励无法补领。', NULL, NULL, 10), (@g10, 'value', '价值误区', '很多人说通行证完成会返还货币，相当于免费，但这个计算只在完整完成时成立。

- 完成整条通行证，返还货币才有意义
- 只完成一半，不是拿一半价值，而是错过另一半已付费奖励
- 赛季结束时未解锁的等级不会保留

所以加速不是买奖励，而是买在截止日期前拿到奖励的时间。', NULL, NULL, 20), (@g10, 'worth', '什么时候值得加速', '值得考虑的情况通常是赛季接近结束、剩余等级很多、但现实时间不够：

- 距离赛季结束只剩几天或一周
- 还差大量等级，正常游玩无法完成
- 已经购买通行证，奖励即将过期
- 工作或学习导致无法在截止日前游玩

如果你只是想提前拿到某个外观，先确认剩余等级和时间，再决定是否值得。', NULL, NULL, 30), (@g10, 'skip', '什么时候应该跳过', '以下情况通常不需要加速：

- 赛季刚开始，还有数周时间
- 每天都能稳定游玩，正常进度足够完成
- 通行证里大部分奖励你并不使用
- 升级本身是你享受赛季目标的一部分

单纯为了把界面清空而补进度，通常是花钱整理菜单，没有实际收益。', NULL, NULL, 40), (@g10, 'service', '站内咨询方式', '如果要咨询通行证进度服务，先说明：

- 当前等级和目标等级
- 赛季剩余天数
- 平台与区服
- 是否还需要活动任务或账号等级

站内当前活动服务可以承接赛季和限时任务。网页不收账号密码、验证码或令牌，交付方式请先与客服确认。', NULL, NULL, 50);
DELETE FROM guide_faqs WHERE guide_id = @g10;
INSERT INTO guide_faqs (guide_id, question, answer, sort_order) VALUES (@g10, '战斗通行证加速值得买吗？', '取决于剩余时间。赛季接近结束且等级差距大时更值得，赛季刚开或你能稳定游玩时不建议。', 10), (@g10, '通行证没打完会怎样？', '赛季结束后未达到的等级会关闭，未解锁的已购奖励无法再领取。', 20), (@g10, '通行证会返还货币吗？', '完整完成时通常会返还部分货币，但只有达到对应等级才能领取。', 30), (@g10, '什么时候应该跳过加速？', '赛季时间充足、你能稳定游玩、或者大部分奖励你并不需要时。', 40);
DELETE FROM guide_products WHERE guide_id = @g10;
INSERT INTO guide_products (guide_id, service_id, label, description, image_url, is_featured, sort_order) VALUES (@g10, 3, '当前活动与限时任务', NULL, NULL, 1, 10), (@g10, 4, '其他任务 / 人工询价', NULL, NULL, 0, 20);

-- MW4 新西兰技巧：如何提前玩《使命召唤：现代战争4》

INSERT INTO game_guides (game_code, slug, title, subtitle, summary, cover_image, difficulty, estimated_minutes, language, author_name, source_name, source_url, status, is_featured, sort_order, tags_json, seo_title, seo_description, canonical_url, view_count, published_at, created_at, updated_at, version) VALUES ('CODMW4', 'mw4-new-zealand-trick', 'MW4 新西兰技巧：如何提前玩《使命召唤：现代战争4》', '地区切换原理、平台步骤与风险说明', '新西兰技巧通过切换主机和 Activision 账号地区，让本地零点解锁更早发生。这篇攻略整理适用平台、具体步骤、Steam 和战网的差异以及风险。', '/assets/covers/service/codmw4-4.jpg', 'INTERMEDIATE', 9, 'zh-CN', '星火攻略组', 'UPBLAZED', 'https://upblazed.com/guides/how-to-play-mw4-early-new-zealand-trick', 'PUBLISHED', 0, 200, JSON_ARRAY('新西兰', '提前游玩', '发售', '地区设置'), 'MW4 新西兰技巧：提前游玩方法与风险 | 星火网游代练平台', 'MW4 新西兰技巧怎么用？适用平台、Activision 地区设置、PS5、Xbox、Xbox PC 步骤，以及 Steam 战网和锁区风险说明。', 'https://wydlgf.cn/guides/mw4-new-zealand-trick', 0, NOW(), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON DUPLICATE KEY UPDATE title=VALUES(title), subtitle=VALUES(subtitle), summary=VALUES(summary), cover_image=VALUES(cover_image), difficulty=VALUES(difficulty), estimated_minutes=VALUES(estimated_minutes), language=VALUES(language), author_name=VALUES(author_name), source_name=VALUES(source_name), source_url=VALUES(source_url), status=VALUES(status), is_featured=VALUES(is_featured), sort_order=VALUES(sort_order), tags_json=VALUES(tags_json), seo_title=VALUES(seo_title), seo_description=VALUES(seo_description), canonical_url=VALUES(canonical_url), published_at=COALESCE(published_at, VALUES(published_at)), updated_at=CURRENT_TIMESTAMP, version=version+1;
SET @g11 := (SELECT id FROM game_guides WHERE slug = 'mw4-new-zealand-trick' LIMIT 1);
DELETE FROM guide_sections WHERE guide_id = @g11;
INSERT INTO guide_sections (guide_id, section_key, heading, body_markdown, image_url, image_alt, sort_order) VALUES (@g11, 'principle', '原理', '新西兰靠近国际日期变更线，是最早进入午夜的地区之一。如果游戏按当地时间零点解锁，新西兰会比欧洲和北美更早开服。

新西兰技巧的本质是把主机地区和 Activision 账号地址切换到新西兰，让解锁时间跟随新西兰时钟。它不涉及修改文件、注入程序或绕过安全机制，只是修改平台本身提供的地区设置。', NULL, NULL, 10), (@g11, 'apply', '是否适用于 MW4', '根据 MW3、黑色行动 6 和黑色行动 7 的发售方式，以下平台有较大概率适用：

- PS5：按本地零点滚动解锁
- Xbox Series X|S：按本地零点滚动解锁
- PC Xbox 应用：使用 Game Pass 或微软商店版本时可能按本地零点解锁
- Steam 和 Battle.net：过去采用全球统一时间，切换地区通常无效
- Nintendo Switch 2：目前没有可靠案例证明技巧有效

Activision 尚未公布 MW4 的具体解锁时间，以上判断基于前作模式，不是官方承诺。', NULL, NULL, 20), (@g11, 'risk', '风险说明', '这不是作弊，但也不是零风险操作。2022 年 Infinity Ward 曾提醒，提前切换主机地区的玩家可能遇到连接问题，并可能被限制到本地区正式解锁后才能进入游戏。

- 可能出现连接异常或临时锁区
- 是否处罚由 Activision 决定，没有商家能承诺绝对安全
- 如果被锁到本地区时间，切回地区等待即可
- 战役抢先体验是否跟随地区时间，目前尚未明确', NULL, NULL, 30), (@g11, 'steps', '操作步骤', '先修改 Activision 账号地址，再按平台调整主机或系统地区：

1. 登录 Activision 官网，进入 Profile 的 Basic Info
2. 编辑地址，将国家改为新西兰并保存
3. PS5：部分玩家还需要新西兰区 PSN 账号，前作表现并不完全一致
4. Xbox：设置、系统、语言和位置、位置、新西兰，然后重启主机
5. PC Xbox 应用：Windows 设置、时间和语言、语言和区域、国家或地区、新西兰，然后重启
6. Steam 和 Battle.net：切换地区通常不能提前解锁

改完地区后先预下载 MW4，避免解锁时才开始下载。', NULL, NULL, 40), (@g11, 'time', '能提前多久', '提前量取决于你所在地区：

- 中欧大约提前半天
- 美国西海岸接近一整天
- 其他地区按与新西兰的时差递减

抢先进入后的首批对局通常强度较低，适合升级武器、完成早期迷彩和熟悉地图。先用最佳设置调整手感，再安排升级目标。', NULL, NULL, 50);
DELETE FROM guide_faqs WHERE guide_id = @g11;
INSERT INTO guide_faqs (guide_id, question, answer, sort_order) VALUES (@g11, 'MW4 新西兰技巧有效吗？', '如果 MW4 延续前作的本地零点解锁方式，PS5、Xbox 和 Xbox PC 版可能有效；Steam 和战网通常无效。', 10), (@g11, '新西兰技巧会被封号吗？', '它不是作弊，但只有 Activision 能决定处罚。2022 年官方曾提醒可能出现连接问题和临时锁区。', 20), (@g11, 'Steam 能用新西兰技巧吗？', '通常不能。前作 Steam 和战网采用全球统一解锁时间，切换地区没有可提前的空间。', 30), (@g11, '需要新西兰区账号吗？', 'PS5 部分玩家需要新西兰区 PSN 账号；Xbox 和 Xbox PC 通常改系统地区加 Activision 地址即可。', 40), (@g11, '能提前多久进入？', '中欧约半天，美国西海岸接近一天，具体取决于时区和实际解锁方式。', 50);
DELETE FROM guide_products WHERE guide_id = @g11;
INSERT INTO guide_products (guide_id, service_id, label, description, image_url, is_featured, sort_order) VALUES (@g11, 4, '其他任务 / 人工询价', NULL, NULL, 1, 10), (@g11, 1, '武器升级', NULL, NULL, 0, 20);

-- Cross-links between the new MW4 guides
DELETE FROM guide_relations WHERE guide_id IN (@g1, @g2, @g3, @g4, @g5, @g6, @g7, @g8, @g9, @g10, @g11);
INSERT INTO guide_relations (guide_id, related_guide_id, sort_order) VALUES (@g1, @g2, 10), (@g1, @g9, 20), (@g1, @g10, 30), (@g2, @g4, 10), (@g2, @g8, 20), (@g2, @g6, 30), (@g3, @g2, 10), (@g3, @g5, 20), (@g3, @g9, 30), (@g4, @g2, 10), (@g4, @g6, 20), (@g4, @g3, 30), (@g5, @g3, 10), (@g5, @g6, 20), (@g5, @g1, 30), (@g6, @g2, 10), (@g6, @g4, 20), (@g6, @g7, 30), (@g7, @g6, 10), (@g7, @g8, 20), (@g7, @g2, 30), (@g8, @g7, 10), (@g8, @g2, 20), (@g8, @g4, 30), (@g9, @g3, 10), (@g9, @g10, 20), (@g9, @g1, 30), (@g10, @g9, 10), (@g10, @g1, 20), (@g10, @g6, 30), (@g11, @g2, 10), (@g11, @g3, 20), (@g11, @g6, 30);
