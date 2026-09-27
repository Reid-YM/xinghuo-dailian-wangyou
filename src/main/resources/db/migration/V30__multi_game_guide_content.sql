CREATE TABLE IF NOT EXISTS game_guides (
    id BIGINT NOT NULL AUTO_INCREMENT,
    game_code VARCHAR(32) NOT NULL,
    slug VARCHAR(140) NOT NULL,
    title VARCHAR(180) NOT NULL,
    subtitle VARCHAR(255) NULL,
    summary TEXT NOT NULL,
    cover_image VARCHAR(500) NULL,
    difficulty VARCHAR(24) NOT NULL DEFAULT 'BEGINNER',
    estimated_minutes INT NOT NULL DEFAULT 30,
    language VARCHAR(16) NOT NULL DEFAULT 'zh-CN',
    author_name VARCHAR(80) NOT NULL DEFAULT '星火攻略组',
    source_name VARCHAR(120) NULL,
    source_url VARCHAR(500) NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    is_featured TINYINT(1) NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 100,
    tags_json JSON NOT NULL,
    seo_title VARCHAR(160) NULL,
    seo_description VARCHAR(300) NULL,
    canonical_url VARCHAR(500) NULL,
    view_count BIGINT NOT NULL DEFAULT 0,
    published_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_game_guides_slug (slug),
    UNIQUE KEY uk_game_guides_game_slug (game_code, slug),
    KEY idx_game_guides_public (status, is_featured, sort_order, published_at),
    KEY idx_game_guides_game (game_code, status, updated_at),
    CONSTRAINT fk_game_guides_game FOREIGN KEY (game_code) REFERENCES game_catalog (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS guide_sections (
    id BIGINT NOT NULL AUTO_INCREMENT,
    guide_id BIGINT NOT NULL,
    section_key VARCHAR(80) NOT NULL,
    heading VARCHAR(180) NOT NULL,
    body_markdown MEDIUMTEXT NOT NULL,
    image_url VARCHAR(500) NULL,
    image_alt VARCHAR(255) NULL,
    sort_order INT NOT NULL DEFAULT 10,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_guide_sections_key (guide_id, section_key),
    KEY idx_guide_sections_order (guide_id, sort_order, id),
    CONSTRAINT fk_guide_sections_guide FOREIGN KEY (guide_id) REFERENCES game_guides (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS guide_products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    guide_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    label VARCHAR(160) NULL,
    description VARCHAR(500) NULL,
    image_url VARCHAR(500) NULL,
    is_featured TINYINT(1) NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 10,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_guide_products_service (guide_id, service_id),
    KEY idx_guide_products_order (guide_id, is_featured, sort_order, id),
    CONSTRAINT fk_guide_products_guide FOREIGN KEY (guide_id) REFERENCES game_guides (id) ON DELETE CASCADE,
    CONSTRAINT fk_guide_products_service FOREIGN KEY (service_id) REFERENCES services (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS guide_faqs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    guide_id BIGINT NOT NULL,
    question VARCHAR(240) NOT NULL,
    answer TEXT NOT NULL,
    sort_order INT NOT NULL DEFAULT 10,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_guide_faqs_order (guide_id, sort_order, id),
    CONSTRAINT fk_guide_faqs_guide FOREIGN KEY (guide_id) REFERENCES game_guides (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS guide_relations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    guide_id BIGINT NOT NULL,
    related_guide_id BIGINT NOT NULL,
    sort_order INT NOT NULL DEFAULT 10,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_guide_relations_pair (guide_id, related_guide_id),
    KEY idx_guide_relations_order (guide_id, sort_order, id),
    CONSTRAINT fk_guide_relations_guide FOREIGN KEY (guide_id) REFERENCES game_guides (id) ON DELETE CASCADE,
    CONSTRAINT fk_guide_relations_related FOREIGN KEY (related_guide_id) REFERENCES game_guides (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS guide_market_assets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    game_code VARCHAR(32) NOT NULL,
    service_id BIGINT NULL,
    asset_type VARCHAR(32) NOT NULL DEFAULT 'COVER',
    source_name VARCHAR(120) NOT NULL,
    source_url VARCHAR(700) NOT NULL,
    source_page_url VARCHAR(1000) NULL,
    local_path VARCHAR(500) NOT NULL,
    checksum_sha256 CHAR(64) NOT NULL,
    width INT NULL,
    height INT NULL,
    fetched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uk_guide_market_asset_source (source_url),
    KEY idx_guide_market_assets_game (game_code, service_id, is_active),
    CONSTRAINT fk_guide_market_assets_game FOREIGN KEY (game_code) REFERENCES game_catalog (code),
    CONSTRAINT fk_guide_market_assets_service FOREIGN KEY (service_id) REFERENCES services (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;