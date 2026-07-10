-- EC800M backend minimal loop migration with family-level hardware SOS.
-- Additive only: no existing table/column is deleted, renamed, or repurposed.
-- Run ALTER statements only after checking the target column/index does not already exist.

CREATE TABLE IF NOT EXISTS families (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '家庭ID',
  family_name VARCHAR(100) DEFAULT NULL COMMENT '家庭名称，例如张爷爷家',
  home_address VARCHAR(255) DEFAULT NULL COMMENT '家庭地址',
  home_latitude DECIMAL(10, 7) DEFAULT NULL COMMENT '家庭纬度，可选',
  home_longitude DECIMAL(10, 7) DEFAULT NULL COMMENT '家庭经度，可选',
  created_by_child_id BIGINT DEFAULT NULL COMMENT '创建该家庭的子女用户ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_families_created_by_child_id (created_by_child_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭表';

CREATE TABLE IF NOT EXISTS family_members (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '家庭成员ID',
  family_id BIGINT NOT NULL COMMENT '家庭ID',
  elder_profile_id BIGINT NOT NULL COMMENT '老人档案ID',
  member_role VARCHAR(50) DEFAULT NULL COMMENT '成员角色，例如 grandfather/grandmother',
  is_primary BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否主要老人，用于兼容旧SOS查询链路',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_family_member (family_id, elder_profile_id),
  KEY idx_family_members_family_id (family_id),
  KEY idx_family_members_elder_profile_id (elder_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭成员表';

CREATE TABLE IF NOT EXISTS iot_devices (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '设备ID',
  device_id VARCHAR(64) NOT NULL COMMENT '设备唯一编号，例如 ec800m-001',
  secret_hash VARCHAR(255) NOT NULL COMMENT '设备上报密钥哈希值，不保存明文',
  family_id BIGINT NOT NULL COMMENT '绑定家庭ID',
  route_elder_profile_id BIGINT DEFAULT NULL COMMENT '兼容旧查询链路用的主要老人档案ID，不代表硬件SOS实际触发者',
  install_area VARCHAR(100) DEFAULT NULL COMMENT '安装区域，例如卧室床头区域',
  status VARCHAR(20) NOT NULL DEFAULT 'offline' COMMENT '设备状态 online/offline',
  signal_strength INT DEFAULT NULL COMMENT '最近一次信号强度',
  last_seen_at DATETIME DEFAULT NULL COMMENT '服务端最近收到上报时间',
  last_heartbeat_at DATETIME DEFAULT NULL COMMENT '最近心跳时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_iot_devices_device_id (device_id),
  KEY idx_iot_devices_family_id (family_id),
  KEY idx_iot_devices_route_elder_profile_id (route_elder_profile_id),
  KEY idx_iot_devices_status (status),
  KEY idx_iot_devices_last_seen_at (last_seen_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物联网设备表';

ALTER TABLE emergency_alerts
  ADD COLUMN family_id BIGINT DEFAULT NULL COMMENT '家庭ID，硬件SOS优先绑定家庭',
  ADD COLUMN source VARCHAR(32) DEFAULT NULL COMMENT '告警来源：APP/HARDWARE_DEVICE/RULE_ENGINE',
  ADD COLUMN device_id VARCHAR(64) DEFAULT NULL COMMENT '硬件设备编号',
  ADD COLUMN area VARCHAR(100) DEFAULT NULL COMMENT '硬件触发区域',
  ADD COLUMN hardware_message VARCHAR(255) DEFAULT NULL COMMENT '硬件SOS消息';

ALTER TABLE emergency_alerts
  ADD INDEX idx_emergency_alerts_family_id (family_id),
  ADD INDEX idx_emergency_alerts_source (source),
  ADD INDEX idx_emergency_alerts_device_id (device_id);

-- If an earlier iot_devices table was already created with elder_profile_id,
-- keep that old column and only add the new family columns/indexes after checking.
-- ALTER TABLE iot_devices ADD COLUMN family_id BIGINT DEFAULT NULL COMMENT '绑定家庭ID';
-- ALTER TABLE iot_devices ADD COLUMN route_elder_profile_id BIGINT DEFAULT NULL COMMENT '兼容旧查询链路用的主要老人档案ID，不代表硬件SOS实际触发者';
-- ALTER TABLE iot_devices ADD INDEX idx_iot_devices_family_id (family_id);
-- ALTER TABLE iot_devices ADD INDEX idx_iot_devices_route_elder_profile_id (route_elder_profile_id);

-- Test seed. Confirm real child_user_id and elder_profile_id values before running.
-- The hash below matches raw secret: demo-secret
-- INSERT INTO families (family_name, home_address, created_by_child_id)
-- VALUES ('张爷爷家', '北京市海淀区示例地址', 1);
-- INSERT INTO family_members (family_id, elder_profile_id, member_role, is_primary)
-- VALUES
-- (1, 1, 'grandfather', TRUE),
-- (1, 2, 'grandmother', FALSE);
-- INSERT INTO iot_devices (device_id, secret_hash, family_id, route_elder_profile_id, install_area, status)
-- VALUES ('ec800m-001', '$2a$10$T2RVpjx/WVuEzFxXItmewOEbqP6saet/eOCkwteajUy2ggAudkBAK', 1, 1, '卧室床头区域', 'offline');
