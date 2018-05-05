const constants = require("./Constants");
const BigQuery = require("@google-cloud/bigquery");
const bigquery = BigQuery({
	projectId: constants.PROJECT_ID
});

exports.queryBatteryData = async function () {

	const options = {
		query: this.sqlBatteryLevel,
		useLegacySql: false
	};

	return new Promise((resolve, reject) => {
		bigquery
			.query(options)
			.then((results) => {
				resolve(results[0]);
			})
			.catch((err) => {
				reject(err);
			});
	});
};

exports.queryBatteryHistory = async function (boardID) {

	const options = {
		query: this.sqlBatteryHistory.replace("?", boardID),
		useLegacySql: false // Use standard SQL syntax for queries.
	};

	return new Promise((resolve, reject) => {
		bigquery
			.query(options)
			.then((results) => {
				resolve(results[0]);
			})
			.catch((err) => {
				reject(err);
			});
	});
};

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
TimeBucket ASC
`;