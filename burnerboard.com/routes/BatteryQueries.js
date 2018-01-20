const constants = require('./Constants');


// Imports the Google Cloud client library
const BigQuery = require('@google-cloud/bigquery');
 
// Instantiates a client
const bigquery = BigQuery({
	projectId: constants.PROJECT_ID
});
 
exports.queryBatteryData = function(callback) {
  
    BatteryQueries = require('./BatteryQueries');
   
    const options = {
      query: BatteryQueries.sqlBatteryLevel,
      useLegacySql: false  
    };
  
    // Runs the query
    bigquery
      .query(options)
      .then((results) => {
        callback(null,results[0]);
      })
      .catch((err) => {
        callback(err,null);
      });
  }

exports.queryBatteryHistory = function(boardID,callback) {

    BatteryQueries = require('./BatteryQueries');
  
    const options = {
      query: BatteryQueries.sqlBatteryHistory.replace('?', boardID),
      useLegacySql: false // Use standard SQL syntax for queries.
    };
  
    // Runs the query
    bigquery
      .query(options)
      .then((results) => {
        callback(null,results[0]);
      })
      .catch((err) => {
        callback(err, null);
      });
  }

exports.sqlBatteryLevel = `SELECT 
board_name,
local_time as last_seen,
is_online,
ROUND(battery_level) as battery_level
FROM (
SELECT
  board_name,
  local_time,
  is_online,
  battery_level,
  row_number
FROM (
  SELECT
  FORMAT_DATETIME('%D %X',MAX(DATETIME(message_timestamp,
        "US/Pacific"))) AS local_time,
    TIMESTAMP_DIFF(CURRENT_TIMESTAMP(), message_timestamp, HOUR) < 3 as is_online,
    board_name,
    ROW_NUMBER() OVER (PARTITION BY board_name ORDER BY message_timestamp DESC) AS row_number,
    battery_level
  FROM
    \`burner-board.telemetry.battery_Data\`
  WHERE
      CAST(voltage AS FLOAT64) > 30.0
  GROUP BY
    board_name,
    message_timestamp,
    battery_level)
WHERE
  row_number = 1 )
GROUP BY
board_name,
local_time,
is_online,
battery_level,
row_number
ORDER BY
board_name ASC
`;

exports.sqlBatteryHistory = `#standardSQL
SELECT
board_name,
ROUND(AVG(battery_level)) AS BatteryLevel,
FORMAT_DATETIME('%D %X',DATETIME(CAST(DATETIME(DATE(message_timestamp),
      TIME(EXTRACT(HOUR
        FROM
          message_timestamp),
        CAST(FLOOR(EXTRACT(MINUTE
            FROM (message_timestamp))/15)*15 AS INT64),
        0) ) AS TIMESTAMP),
  "US/Pacific")) AS TimeBucket
FROM
\`burner-board.telemetry.battery_Data\`
WHERE
(board_name = "?")
AND message_timestamp IS NOT NULL
AND TIMESTAMP_DIFF(CURRENT_TIMESTAMP(), message_timestamp, HOUR) < (7 * 24)
GROUP BY
board_name,
TimeBucket
ORDER BY
TimeBucket DESC
`