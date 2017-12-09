var express = require('express');
var router = express.Router();

router.use('/media', require('./media'));

/* GET home page. */
router.get('/', function (req, res, next) {
	res.render('index', { title: 'burnerboard.com' });
});

/* GET battery results. */
router.get('/allBoards', function (req, res, next) {

	BatteryQueries = require('./BatteryQueries')
	BatteryQueries.queryBatteryData(function(err, batteryData){
		if(err){
			res.status(500).send("Error");
		}
		else{
			res.status(200).json(batteryData)
		}
	});

});

router.get('/boards/:boardID/directoryJSONURL', function (req, res, next) {
	DownloadDirectory = require('./media/DownloadDirectory')
	res.status(200).send(DownloadDirectory.getDirectoryJSONPath(req.params.boardID));
});

/* GET battery results. */
router.get('/boards/:boardID/batteryHistory', function (req, res, next) {

	BatteryQueries = require('./BatteryQueries')
	BatteryQueries.queryBatteryHistory(req.params.boardID, function(err, batteryHistory){
		if(err){
			res.status(500).send("Error");
		}
		else{
			res.status(200).json(batteryHistory)
		}
	});
});







// Imports the Google Cloud client library
const BigQuery = require('@google-cloud/bigquery');
const projectId = "burner-board";

// Instantiates a client
const bigquery = BigQuery({
	projectId: projectId
});
 
module.exports = router;
