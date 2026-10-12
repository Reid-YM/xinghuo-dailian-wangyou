ALTER TABLE site_config
    ADD COLUMN home_service_mobile_limit INT NOT NULL DEFAULT 4 AFTER home_service_limit,
    ADD COLUMN home_service_desktop_columns INT NOT NULL DEFAULT 4 AFTER home_service_mobile_limit,
    ADD COLUMN home_service_mobile_columns INT NOT NULL DEFAULT 1 AFTER home_service_desktop_columns,
    ADD COLUMN service_card_density VARCHAR(16) NOT NULL DEFAULT 'COMFORTABLE' AFTER home_service_mobile_columns,
    ADD COLUMN service_card_mobile_density VARCHAR(16) NOT NULL DEFAULT 'COMPACT' AFTER service_card_density,
    ADD COLUMN home_contact_limit INT NOT NULL DEFAULT 6 AFTER contact_channels,
    ADD COLUMN home_contact_mobile_limit INT NOT NULL DEFAULT 2 AFTER home_contact_limit,
    ADD COLUMN trust_case_home_limit INT NOT NULL DEFAULT 4 AFTER trust_cases,
    ADD COLUMN trust_case_home_mobile_limit INT NOT NULL DEFAULT 2 AFTER trust_case_home_limit,
    ADD COLUMN trust_faq_home_limit INT NOT NULL DEFAULT 6 AFTER trust_faq,
    ADD COLUMN trust_faq_home_mobile_limit INT NOT NULL DEFAULT 4 AFTER trust_faq_home_limit;
