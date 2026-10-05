-- V22 keeps the home hero price visible after the storefront copy refresh.

UPDATE site_config
SET starting_price_mode='MANUAL',
    starting_price=20.00,
    show_starting_price=TRUE,
    updated_at=UTC_TIMESTAMP(6)
WHERE id=1;
