UPDATE sy_database_driver
SET name = 'hive_4.1.0',
    file_name = 'hive-jdbc-4.1.0-standalone.jar',
    last_modified_by = 'zhiqingyun',
    last_modified_date_time = CURRENT_TIMESTAMP,
    version_number = version_number + 1
WHERE id = 'hive_3.1.3'
  AND db_type = 'HIVE'
  AND driver_type = 'SYSTEM_DRIVER';
