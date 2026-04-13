SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `drug_research_platform`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `drug_research_platform`;

CREATE TABLE IF NOT EXISTS `platform_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(80) NOT NULL,
  `email` VARCHAR(120) NOT NULL,
  `password_hash` VARCHAR(128) NOT NULL,
  `role` VARCHAR(40) NOT NULL,
  `status` VARCHAR(40) NOT NULL,
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_user_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `literature` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(300) NOT NULL,
  `source_type` VARCHAR(64) NOT NULL,
  `disease_area` VARCHAR(120) NOT NULL,
  `summary` TEXT NOT NULL,
  `keywords` VARCHAR(1000) NULL,
  `publication_date` DATE NULL,
  `storage_path` VARCHAR(300) NULL,
  `vector_sync_status` VARCHAR(40) NULL,
  `vector_sync_detail` VARCHAR(500) NULL,
  `vector_synced_chunk_count` INT NULL,
  `vector_synced_at` DATETIME NULL,
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_literature_title` (`title`),
  KEY `idx_literature_disease_area` (`disease_area`),
  KEY `idx_literature_publication_date` (`publication_date`),
  KEY `idx_literature_created_by` (`created_by`),
  CONSTRAINT `fk_literature_created_by`
    FOREIGN KEY (`created_by`) REFERENCES `platform_user` (`id`)
    ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `literature_chunk` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `literature_id` BIGINT NOT NULL,
  `chunk_index` INT NOT NULL,
  `content` TEXT NOT NULL,
  `embedding_json` MEDIUMTEXT NULL,
  `source_section` VARCHAR(32) NOT NULL,
  `chunk_label` VARCHAR(255) NULL,
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_literature_chunk_unique` (`literature_id`, `chunk_index`),
  KEY `idx_literature_chunk_literature_id` (`literature_id`),
  CONSTRAINT `fk_literature_chunk_literature`
    FOREIGN KEY (`literature_id`) REFERENCES `literature` (`id`)
    ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `study_indicator` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `literature_id` BIGINT NOT NULL,
  `indicator_name` VARCHAR(120) NOT NULL,
  `category` VARCHAR(80) NOT NULL,
  `time_window` VARCHAR(120) NULL,
  `cohort` VARCHAR(120) NULL,
  `observed_value` DECIMAL(12,4) NULL,
  `confidence_score` DECIMAL(5,2) NULL,
  `evidence_snippet` TEXT NOT NULL,
  `evidence_locator` TEXT NULL,
  `review_status` VARCHAR(40) NULL,
  `reviewer_note` VARCHAR(1000) NULL,
  `reviewed_at` DATETIME NULL,
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_study_indicator_literature_id` (`literature_id`),
  KEY `idx_study_indicator_name` (`indicator_name`),
  KEY `idx_study_indicator_category` (`category`),
  CONSTRAINT `fk_study_indicator_literature`
    FOREIGN KEY (`literature_id`) REFERENCES `literature` (`id`)
    ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `indicator_state` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `literature_id` BIGINT NOT NULL,
  `indicator_name` VARCHAR(120) NOT NULL,
  `stage_type` VARCHAR(40) NOT NULL,
  `state_order` INT NOT NULL,
  `state_label` VARCHAR(120) NOT NULL,
  `description` TEXT NOT NULL,
  `evidence_locator` TEXT NULL,
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_indicator_state_literature_id` (`literature_id`),
  KEY `idx_indicator_state_indicator_name` (`indicator_name`),
  CONSTRAINT `fk_indicator_state_literature`
    FOREIGN KEY (`literature_id`) REFERENCES `literature` (`id`)
    ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `state_transition` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `literature_id` BIGINT NOT NULL,
  `from_state_id` BIGINT NOT NULL,
  `to_state_id` BIGINT NOT NULL,
  `condition_text` TEXT NOT NULL,
  `transition_probability` DECIMAL(5,2) NULL,
  `evidence_locator` TEXT NULL,
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_state_transition_literature_id` (`literature_id`),
  KEY `idx_state_transition_from_state_id` (`from_state_id`),
  KEY `idx_state_transition_to_state_id` (`to_state_id`),
  CONSTRAINT `fk_state_transition_literature`
    FOREIGN KEY (`literature_id`) REFERENCES `literature` (`id`)
    ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_state_transition_from_state`
    FOREIGN KEY (`from_state_id`) REFERENCES `indicator_state` (`id`)
    ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_state_transition_to_state`
    FOREIGN KEY (`to_state_id`) REFERENCES `indicator_state` (`id`)
    ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `analysis_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `task_type` VARCHAR(60) NOT NULL,
  `status` VARCHAR(60) NOT NULL,
  `input_text` VARCHAR(1500) NOT NULL,
  `result_summary` TEXT NOT NULL,
  `citations` TEXT NULL,
  `context_literature_ids` VARCHAR(1000) NULL,
  `analysis_focus` VARCHAR(500) NULL,
  `evidence_json` LONGTEXT NULL,
  `indicators_json` LONGTEXT NULL,
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_analysis_task_type` (`task_type`),
  KEY `idx_analysis_task_status` (`status`),
  KEY `idx_analysis_task_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `operator_email` VARCHAR(120) NULL,
  `action_type` VARCHAR(80) NOT NULL,
  `resource_type` VARCHAR(120) NOT NULL,
  `resource_id` VARCHAR(255) NULL,
  `detail` VARCHAR(1000) NOT NULL,
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_operation_log_operator_email` (`operator_email`),
  KEY `idx_operation_log_action_type` (`action_type`),
  KEY `idx_operation_log_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `study_indicator` MODIFY COLUMN `evidence_locator` TEXT NULL;
ALTER TABLE `indicator_state` MODIFY COLUMN `evidence_locator` TEXT NULL;
ALTER TABLE `state_transition` MODIFY COLUMN `evidence_locator` TEXT NULL;
ALTER TABLE `analysis_task` MODIFY COLUMN `citations` TEXT NULL;
ALTER TABLE `analysis_task` ADD COLUMN IF NOT EXISTS `analysis_focus` VARCHAR(500) NULL;
ALTER TABLE `analysis_task` ADD COLUMN IF NOT EXISTS `evidence_json` LONGTEXT NULL;
ALTER TABLE `analysis_task` ADD COLUMN IF NOT EXISTS `indicators_json` LONGTEXT NULL;

INSERT INTO `platform_user` (`username`, `email`, `password_hash`, `role`, `status`)
SELECT 'research_admin', 'admin@aiforaso.local', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'ADMIN', 'ACTIVE'
WHERE NOT EXISTS (
  SELECT 1 FROM `platform_user` WHERE `email` = 'admin@aiforaso.local'
);

INSERT INTO `platform_user` (`username`, `email`, `password_hash`, `role`, `status`)
SELECT 'clinical_reviewer', 'reviewer@aiforaso.local', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'RESEARCHER', 'ACTIVE'
WHERE NOT EXISTS (
  SELECT 1 FROM `platform_user` WHERE `email` = 'reviewer@aiforaso.local'
);

UPDATE `platform_user`
SET `password_hash` = '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92'
WHERE `email` IN ('admin@aiforaso.local', 'reviewer@aiforaso.local')
  AND (`password_hash` IS NULL OR `password_hash` = '');

INSERT INTO `literature` (`title`, `source_type`, `disease_area`, `summary`, `keywords`, `publication_date`, `storage_path`)
SELECT
  'Efficacy and Safety of Bepirovirsen in Chronic Hepatitis B Infection',
  'PDF',
  'viral hepatitis',
  'Phase study discussing HBsAg reduction, HBV DNA dynamics, and treatment response patterns in ASO-supported chronic hepatitis B research.',
  'HBsAg,HBV DNA,ASO,hepatitis B',
  '2024-01-10',
  '数据示例/Efficacy and Safety of Bepirovirsen in Chronic Hepatitis B Infection.pdf'
WHERE NOT EXISTS (
  SELECT 1 FROM `literature`
  WHERE `title` = 'Efficacy and Safety of Bepirovirsen in Chronic Hepatitis B Infection'
);

INSERT INTO `literature` (`title`, `source_type`, `disease_area`, `summary`, `keywords`, `publication_date`, `storage_path`)
SELECT
  'Long-term open-label vebicorvir for chronic HBV infection',
  'PDF',
  'viral hepatitis',
  'Long-term safety follow-up highlighting ALT, AST, virologic rebound, and off-treatment response in nucleotide or oligonucleotide combination research.',
  'ALT,AST,HBV,off-treatment response',
  '2024-06-25',
  '数据示例/Long-term open-label vebicorvir for chronic HBV infection Safety and off-treatment responses.pdf'
WHERE NOT EXISTS (
  SELECT 1 FROM `literature`
  WHERE `title` = 'Long-term open-label vebicorvir for chronic HBV infection'
);

INSERT INTO `literature` (`title`, `source_type`, `disease_area`, `summary`, `keywords`, `publication_date`, `storage_path`)
SELECT
  'Safety, pharmacodynamics, and antiviral activity of selgantolimod',
  'PDF',
  'infectious disease',
  'Immunology-oriented evidence covering antiviral activity, pharmacodynamic changes, HBeAg, and tolerability in viremic patients.',
  'pharmacodynamics,HBeAg,antiviral activity,safety',
  '2023-11-18',
  '数据示例/Safety, pharmacodynamics, and antiviral activity of selgantolimod in viremic patients with chronic hepatitis B virus infection.pdf'
WHERE NOT EXISTS (
  SELECT 1 FROM `literature`
  WHERE `title` = 'Safety, pharmacodynamics, and antiviral activity of selgantolimod'
);

SET FOREIGN_KEY_CHECKS = 1;
