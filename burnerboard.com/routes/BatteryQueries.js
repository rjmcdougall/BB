const constants = require("./Constants");
const {BigQuery} = require("@google-cloud/bigquery");
const bigquery = new BigQuery({
	projectId: constants.PROJECT_ID
});

exports.queryBatteryData = async function () {

	const options = {
		query: this.sqlBatteryLevel,
		useLegacySql: false
	};

	try {
		var results = await bigquery.query(options);
		return results[0];
	}
	catch (error) {
		return error;
	}
};

exports.queryBatteryHistory = async function (boardID) {
	try {
		const options = {
			query: this.sqlBatteryHistory.replace("?", boardID),
			useLegacySql: false // Use standard SQL syntax for queries.
		};

		var results = await bigquery.query(options);
		return results[0];
	}
	catch (error) {
		return error;
	}
};

exports.queryBoardLocations = async function () {

	const options = {
		query: sqlBoardLoactions,
		useLegacySql: false // Use standard SQL syntax for queries.
	};

	try {
		var results = await bigquery.query(options);
		console.log(results);
		return results[0].map((item) => {
			return {
				lat: item.lat,
				lon: item.lon,
				board: item.board,
				time: item.time_stamp.value,
			};
		});

	}
	catch (error) {
		throw new Error(error);
	}

};

var sqlBoardLoactions = `SELECT
lat,
lon,
board,
time_stamp
FROM (
SELECT
  e.lat AS lat,
  e.lon AS lon,
  e.board_name AS board,
  e.time_stamp AS time_stamp,
  ROW_NUMBER() OVER (PARTITION BY e.board_name ORDER BY e.time_stamp DESC) AS seqnum
FROM
  \`burner-board.telemetry.events\` e
JOIN (
  SELECT
	board_name,
	MAX(time_stamp) AS time_stamp
  FROM
	\`burner-board.telemetry.events\` j
  GROUP BY
	board_name ) j
ON
  j.board_name = e.board_name
WHERE
  j.time_stamp = e.time_stamp ) t
WHERE
seqnum < 2;`;

exports.sqlBatteryLevel = `SELECT
board_name,
local_time AS last_seen,
is_online,
ROUND(battery_level) AS battery_level
FROM (
SELECT
  board_name,
  local_time,
  is_online,
  battery_level,
  row_number
FROM (
  SELECT
    FORMAT_DATETIME('%D %X',
      MAX(DATETIME(message_timestamp,
          "US/Pacific"))) AS local_time,
    TIMESTAMP_DIFF(CURRENT_TIMESTAMP(), message_timestamp, HOUR) < 3 AS is_online,
    board_name,
    ROW_NUMBER() OVER (PARTITION BY board_name ORDER BY message_timestamp DESC) AS row_number,
    battery_level
  FROM
    \`burner-board.telemetry.battery_Data\`
  WHERE
    CAST(voltage AS FLOAT64) > 30.0
    AND DATE(message_timestamp, "US/Pacific") BETWEEN DATE_ADD(CURRENT_DATE(), INTERVAL -15 DAY)
    AND CURRENT_DATE()
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

