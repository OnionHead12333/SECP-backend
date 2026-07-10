-- ============================================
-- 喝水提醒功能数据库建表脚本
-- 用于 MySQL 数据库 elder
-- ============================================

-- 1. 喝水提醒计划表
CREATE TABLE IF NOT EXISTS `water_reminders` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `elder_profile_id` BIGINT NOT NULL COMMENT '老人档案ID，关联 elder_profiles.id',
    `scheduled_at` DATETIME NOT NULL COMMENT '计划提醒时间',
    `status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '提醒状态: pending/confirmed/missed/snoozed',
    `confirmed_at` DATETIME NULL COMMENT '确认喝水时间',
    `snooze_until` DATETIME NULL COMMENT '稍后提醒截止时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_elder_date` (`elder_profile_id`, `scheduled_at`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='喝水提醒计划表';

-- 2. 提醒执行日志表（通用）
CREATE TABLE IF NOT EXISTS `reminder_execution_logs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `elder_profile_id` BIGINT NOT NULL COMMENT '老人档案ID',
    `reminder_id` BIGINT NOT NULL COMMENT '关联的提醒ID',
    `reminder_type` VARCHAR(32) NOT NULL DEFAULT 'water' COMMENT '提醒类型: water/medicine/exercise',
    `scheduled_at` DATETIME NOT NULL COMMENT '计划触发时间',
    `confirmed_at` DATETIME NULL COMMENT '实际确认时间',
    `snoozed_at` DATETIME NULL COMMENT '稍后操作时间',
    `status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '执行状态: pending/confirmed/missed/snoozed',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_elder_reminder` (`elder_profile_id`, `reminder_id`),
    INDEX `idx_type_date` (`reminder_type`, `scheduled_at`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提醒执行日志表';
