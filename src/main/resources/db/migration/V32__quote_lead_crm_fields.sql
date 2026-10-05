ALTER TABLE quotes
  ADD COLUMN lead_score INT NULL,
  ADD COLUMN lead_intent_level VARCHAR(20) NULL,
  ADD COLUMN next_action VARCHAR(240) NULL,
  ADD COLUMN follow_up_note TEXT NULL,
  ADD COLUMN ai_suggestion TEXT NULL,
  ADD COLUMN last_contacted_at DATETIME(6) NULL;
