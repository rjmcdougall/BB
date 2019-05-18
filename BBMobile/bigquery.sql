  SELECT board_name, ROUND(AVG(battery_level)) AS BatteryLevel, 
  FORMAT_DATETIME('%D %X',DATETIME(CAST(DATETIME(DATE(message_timestamp), 
  TIME(EXTRACT(HOUR FROM message_timestamp), 
  CAST(FLOOR(EXTRACT(MINUTE FROM (message_timestamp))/15)*15 AS INT64), 0) ) AS TIMESTAMP), "US/Pacific")) AS TimeBucket 
  FROM `burner-board.telemetry.battery_Data` WHERE (board_name = "vega") AND message_timestamp IS NOT NULL AND TIMESTAMP_DIFF(CURRENT_TIMESTAMP(), message_timestamp, HOUR) < (7 * 24) GROUP BY board_name, TimeBucket ORDER BY TimeBucket ASC
 