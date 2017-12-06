var express = require('express');
var router = express.Router();

router.use('/media', require('./media'));

/* GET home page. */
router.get('/', function (req, res, next) {
  res.render('index', { title: 'burnerboard.com' });
});

/* GET battery results. */
router.get('/allBoards', function (req, res, next) {

  queryBatteryData(req, res, next);

});

router.get('/boards/:boardID/directoryJSONURL', function (req, res, next) {
  DownloadDirectory = require('./media/DownloadDirectory')
  res.status(200).send(DownloadDirectory.getDirectoryJSONPath(req.params.boardID));
});

/* GET battery results. */
router.get('/boards/:boardID/batteryHistory', function (req, res, next) {

  queryBatteryHistory(req, res, next);

});

function queryBatteryHistory(req, res, next) {

  console.log(req.params.boardID);

  BatteryQueries = require('./BatteryQueries');

   const options = {
    query: BatteryQueries.sqlBatteryHistory.replace('?', req.params.boardID),
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

  BatteryQueries = require('./BatteryQueries');
  
  // Query options list: https://cloud.google.com/bigquery/docs/reference/v2/jobs/query
  const options = {
    query: BatteryQueries.sqlBatteryLevel,
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



// Imports the Google Cloud client library
const BigQuery = require('@google-cloud/bigquery');
const projectId = "burner-board";

// Instantiates a client
const bigquery = BigQuery({
  projectId: projectId
});









module.exports = router;
