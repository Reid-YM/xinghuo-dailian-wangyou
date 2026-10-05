-- Keep COD MW4 first, then DMZ, Warzone and War Dogs in the public service filters.
-- The first service of each category also controls the category filter order.

UPDATE services SET manual_priority = 40, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-dmz-missions';
UPDATE services SET manual_priority = 39, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'dmz-key-farming';
UPDATE services SET manual_priority = 38, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'dmz-faction-rep';
UPDATE services SET manual_priority = 37, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-dmz-weapon-unlock';

UPDATE services SET manual_priority = 35, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'warzone-ranked';
UPDATE services SET manual_priority = 34, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'warzone-loadout';
UPDATE services SET manual_priority = 33, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'warzone-event';
