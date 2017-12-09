var express = require('express');
var router = express.Router();
 
router.get('/:boardID/directoryJSONURL', function (req, res, next) {
	DownloadDirectory = require('../media/DownloadDirectory')
	res.status(200).send(DownloadDirectory.getDirectoryJSONPath(req.params.boardID));
});

/* GET battery results. */
router.get('/:boardID/batteryHistory', function (req, res, next) {

	BatteryQueries = require('./BatteryQueries');

	BatteryQueries.queryBatteryHistory(req.params.boardID, function (err, batteryHistory) {
		if (!err) {
			res.status(200).json(batteryHistory);
		}
		else {
			res.status(500).send(err);
		}
	});
});

/* GET battery results. */
router.get('/', function (req, res, next) {
	
		BatteryQueries = require('./BatteryQueries');
		
		BatteryQueries.queryBatteryData(req, res, function (err, allBoardsData) {
			if (!err) {
				res.status(200).json(allBoardsData);
			}
			else {
				res.status(500).send(err);
			}
		});
	
	});


module.exports = router;
