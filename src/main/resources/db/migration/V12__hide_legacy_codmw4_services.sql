UPDATE services
SET is_active = FALSE,
    status = 'OFFLINE',
    updated_at = UTC_TIMESTAMP(6),
    version = version + 1
WHERE game_code = 'CODMW4'
  AND slug NOT IN ('weapon-leveling','camo-task','dmz-task','event-task','rank-boost','other-task');
