-- 示例：为某个老人档案插入三条提醒（把 @ELDER_ID 换成你库里真实的 elder_profiles.id）
-- 库名与 backend application.yml 一致，默认 elder
USE elder;

SET @E := 1;  -- 老人档案 ID

INSERT INTO water_reminders (
  elder_profile_id, title, daily_target_ml, per_intake_ml, interval_minutes,
  start_time, end_time, today_intake_ml, source_type, remind_time, repeat_rule, enabled, status, created_by
) VALUES (
  @E, '喝水提醒', 1500, 200, 60,
  '08:00:00', '22:00:00', 0, 'child_remote', NOW(), 'daily', 1, 'pending', 'child'
);

INSERT INTO medicine_reminders (
  elder_profile_id, title, medicine_name, dosage, frequency_rule, source_type,
  remind_time, repeat_rule, enabled, status, created_by
) VALUES (
  @E, '早间用药', '降压药', '1 片', 'once', 'child_remote',
  CONCAT(CURDATE(), ' 08:30:00'), 'daily', 1, 'pending', 'child'
);

INSERT INTO exercise_reminders (
  elder_profile_id, title, exercise_type, start_time, end_time, source_type,
  remind_time, repeat_rule, enabled, status, created_by
) VALUES (
  @E, '散步锻炼', '散步', '08:00:00', '18:00:00', 'child_remote',
  CONCAT(CURDATE(), ' 09:00:00'), 'daily', 1, 'pending', 'child'
);
