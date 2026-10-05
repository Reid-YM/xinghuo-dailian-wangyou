-- Assign unique cover images to the MW4 guide set.
-- The four newly supplied images are used once each; the remaining guides use distinct existing MW4 covers.

UPDATE game_guides SET cover_image = '/assets/covers/service/codmw4-3.jpg', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-monster-energy-codes';
UPDATE game_guides SET cover_image = '/assets/covers/guides/mw4-korea-village.png', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-best-settings';
UPDATE game_guides SET cover_image = '/assets/covers/service/codmw4-1.jpg', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-weapon-leveling-fast';
UPDATE game_guides SET cover_image = '/assets/covers/guides/mw4-dmz-interior.jpg', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-kill-block-guide';
UPDATE game_guides SET cover_image = '/assets/covers/service/codmw4-2.jpg', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-mastery-camos';
UPDATE game_guides SET cover_image = '/assets/covers/guides/mw4-dmz-tune-in.jpg', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-ranked-play';
UPDATE game_guides SET cover_image = '/assets/covers/service/codmw4-6.jpg', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-kd-explained';
UPDATE game_guides SET cover_image = '/assets/covers/guides/mw4-forest-squad.jpg', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-nuke-guide';
UPDATE game_guides SET cover_image = '/assets/covers/service/codmw4-4.jpg', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'cod-account-leveling';
UPDATE game_guides SET cover_image = '/assets/covers/service/codmw4-5.jpg', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-battle-pass-value';
UPDATE game_guides SET cover_image = '/assets/covers/game/codmw4.jpg', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-new-zealand-trick';
