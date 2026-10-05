-- Chinese product names for the MW4 camo catalog.

UPDATE services SET name = 'MW4 全迷彩代练', short_desc = '多人、战区和 DMZ 精通迷彩整枪进度推进', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'camo-task';
UPDATE services SET name = 'MW4 迷彩层级代练', short_desc = '精通迷彩前置层级，按武器当前进度补差', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-tier-camo';
UPDATE services SET name = 'MW4 流光漂移迷彩', short_desc = '多人模式流光漂移精通迷彩，指定武器推进', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-mercurial-drift-camo';
UPDATE services SET name = 'MW4 多原子重制迷彩', short_desc = '多人模式多原子重制精通迷彩，指定武器推进', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-polyatomic-reforged-camo';
UPDATE services SET name = 'MW4 猎户座重制迷彩', short_desc = '多人模式猎户座重制精通迷彩，指定武器推进', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-orion-reforged-camo';
UPDATE services SET name = 'MW4 视差迷彩', short_desc = '战区视差精通迷彩，指定武器推进', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-parallax-camo';
UPDATE services SET name = 'MW4 大马士革重制迷彩', short_desc = '战区大马士革重制精通迷彩，指定武器推进', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-damascus-reforged-camo';
UPDATE services SET name = 'MW4 恩派罗斯迷彩', short_desc = '战区恩派罗斯精通迷彩，指定武器推进', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-empyros-camo';
UPDATE services SET name = 'MW4 手性迷彩', short_desc = 'DMZ 手性精通迷彩，指定武器推进', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-chiral-camo';
UPDATE services SET name = 'MW4 涟漪之潮迷彩', short_desc = 'DMZ 涟漪之潮精通迷彩，指定武器推进', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-ripple-tide-camo';
UPDATE services SET name = 'MW4 赫利俄斯迷彩', short_desc = 'DMZ 赫利俄斯精通迷彩，按剩余挑战推进', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-helio-camo';
UPDATE services SET name = 'MW4 火山碎屑迷彩', short_desc = '火山碎屑声望迷彩第一阶，武器满级后推进', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-pyroclast-camo';
UPDATE services SET name = 'MW4 火山碎屑·边缘迷彩', short_desc = '火山碎屑·边缘声望迷彩，含前置阶段', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-pyroclast-verge-camo';
UPDATE services SET name = 'MW4 火山碎屑·散射迷彩', short_desc = '火山碎屑·散射声望迷彩，含前置阶段', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-pyroclast-scatter-camo';
UPDATE services SET name = 'MW4 火山碎屑·瀑流迷彩', short_desc = '火山碎屑·瀑流声望迷彩，含全部前置阶段', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-pyroclast-cascade-camo';
