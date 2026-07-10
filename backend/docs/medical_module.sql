-- 医疗单据归档 / 提醒日历（ddl-auto: none 时需手动执行）
-- MySQL 8+ / InnoDB

CREATE TABLE IF NOT EXISTS medical_archive_folders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    elder_profile_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_med_folder_elder (elder_profile_id)
);

CREATE TABLE IF NOT EXISTS medical_documents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    elder_profile_id BIGINT NOT NULL,
    uploaded_by_user_id BIGINT NOT NULL,
    folder_id BIGINT NULL,
    title VARCHAR(256) NULL,
    original_filename VARCHAR(512) NULL,
    stored_path VARCHAR(1024) NOT NULL,
    content_type VARCHAR(128) NULL,
    doc_category VARCHAR(64) NULL,
    routed_specialized_api VARCHAR(64) NULL,
    structured_route_source VARCHAR(32) NULL,
    full_text MEDIUMTEXT NULL,
    ocr_raw_json LONGTEXT NULL,
    classify_raw_json LONGTEXT NULL,
    specialized_raw_json LONGTEXT NULL,
    display_blocks_json LONGTEXT NULL,
    structured_fields_json LONGTEXT NULL,
    structured_error VARCHAR(512) NULL,
    extracted_fields_json LONGTEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_med_doc_elder_created (elder_profile_id, created_at),
    INDEX idx_med_doc_folder (folder_id),
    CONSTRAINT fk_med_doc_folder FOREIGN KEY (folder_id) REFERENCES medical_archive_folders (id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS medical_calendar_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    elder_profile_id BIGINT NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    title VARCHAR(256) NOT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NULL,
    notes VARCHAR(1024) NULL,
    source_document_id BIGINT NULL,
    created_by_user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_med_cal_elder_start (elder_profile_id, start_at),
    CONSTRAINT fk_med_cal_doc FOREIGN KEY (source_document_id) REFERENCES medical_documents (id) ON DELETE SET NULL
);
