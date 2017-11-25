var express = require('express');
var router = express.Router();


/* GET home page. */
router.get('/', function (req, res, next) {
  res.render('index', { title: 'burnerboard.com' });
});

/* GET battery results. */
router.get('/batterydata', function (req, res, next) {

  queryBatteryData(req, res, next);

});

/* GET battery results. */
router.get('/boards/:boardId/batteryHistory', function (req, res, next) {

  queryBatteryHistory(req, res, next);

});

function queryBatteryHistory(req, res, next) {

  console.log(req.params.boardId);

  // Query options list: https://cloud.google.com/bigquery/docs/reference/v2/jobs/query
  const options = {
    query: sqlBatteryHistory.replace('?', req.params.boardId),
    useLegacySql: false // Use standard SQL syntax for queries.
  };

  // Runs the query
  bigquery
    .query(options)
    .then((results) => {
      const rows = results[0];
      //printResult(rows);
      res.render('tableHistory', { Datarows: rows })
    })
    .catch((err) => {
      console.error('ERROR:', err);
    });
}

function queryBatteryData(req, res, next) {

  // Query options list: https://cloud.google.com/bigquery/docs/reference/v2/jobs/query
  const options = {
    query: sqlBatteryLevel,
    useLegacySql: false // Use standard SQL syntax for queries.
  };

  // Runs the query
  bigquery
    .query(options)
    .then((results) => {
      const rows = results[0];
      //printResult(rows);
      res.render('table', { Datarows: rows })
    })
    .catch((err) => {
      console.error('ERROR:', err);
    });
}

const sqlBatteryLevel = `SELECT 
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

const sqlBatteryHistory = `#standardSQL
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

// Imports the Google Cloud client library
const BigQuery = require('@google-cloud/bigquery');
const projectId = "burner-board";

// Instantiates a client
const bigquery = BigQuery({
  projectId: projectId
});









module.exports = router;
