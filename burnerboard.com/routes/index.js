var express = require('express');
var router = express.Router();
var util = require('util')
var format = require('util').format;
var Multer = require('multer');
var bodyParser = require('body-parser');
 
router.use(bodyParser.json());

const upload = Multer({
	storage: Multer.memoryStorage(),
	limits: {
		fileSize: 20 * 1024 * 1024
	}
});

/* GET home page. */
router.get('/', function (req, res, next) {
	res.render('index', { title: 'burnerboard.com' });
});

/* GET battery results. */
router.get('/boards', function (req, res, next) {

	var boardNames = [
		"akula",
		"artemis",
		"candy",
		"monaco",
		"pegasus",
		"ratchet",
		"squeeze",
		"vega"
	]

	res.status(200).json(boardNames)

});

/* GET battery results. */
router.get('/boards/currentStatuses', function (req, res, next) {

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
	DownloadDirectory = require('./DownloadDirectory')
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

router.get('/boards/:boardID/upload', (req, res, next) => {
	res.render('uploadForm', { title: 'burnerboard.com', boardID: req.params.boardID });
});

router.post('/boards/:boardID/upload', upload.single('file'), (req, res, next) => {
	if (!req.file) {
		res.status(400).send('No file uploaded.');
		return;
	}

	FileSystem = require('./FileSystem');

	FileSystem.addFile(req.params.boardID, req.file, req.body.speechCue, function (err, savedFile) {
		if (!err) {
			res.status(200).json(savedFile);
		}
		else {
			res.status(500).send("ERROR");
		}
	})
});

router.get('/boards/:boardID/listFiles', function (req, res, next) {
	FileSystem = require('./FileSystem');

	FileSystem.listFiles(req.params.boardID, function (err, fileList) {
		if (err) {
			res.status(500).send("ERROR");
		}
		else {
			res.status(200).json(fileList);
		}
	});
});

router.get('/boards/:boardID/DownloadDirectoryJSON', function (req, res, next) {
	DownloadDirectory = require('./DownloadDirectory')

	DownloadDirectory.DirectoryJSON(req.params.boardID
		, function (err, data) {
			if (!err) {
				res.status(200).send(JSON.stringify(data));
			}
			else {
				res.send(err);
			}
		}
	);
});

router.post('/boards/:boardID/DownloadDirectoryJSONAudio', function (req, res, next) {
	DownloadDirectory = require('./DownloadDirectory')
 
	var audioArray = req.body.audioArray;

	DownloadDirectory.reorderAudio(req.params.boardID, audioArray, function (err, data) {
		if (!err) {
			res.status(200).send(JSON.stringify(data));
		}
		else {
			res.send(err);
		}
	}
	);
});

router.post('/boards/:boardID/AddFileFromGDrive', function (req, res, next) {

//	var oAuthToken = 'ya29.Glw0BTczgRoygNAePtuBdxMqUMYgzwMCy6yr2LvZ4VQfHV8vlgd_nYB6HvXkXvC-QpLvdLdEAKFF9fMEkFHLzumj0clizXBsOpAQT7dTKqEV1kodOo0RKlRK_4IrPA';
var oAuthToken = req.body.oauthToken;

//	var fileId = '1aSXoRfidHo33nlVRDha9xdrKaPKeQJCS';
	var fileId = req.body.fileId;
	var currentBoard = req.body.currentBoard;
//	var currentBoard = 'vega';

	FileSystem = require('./FileSystem');

	FileSystem.addGDriveFile(currentBoard, fileId, oAuthToken, "", function (err, savedFile) {
		if (!err) {
			res.status(200).json(savedFile);
		}
		else {
			res.status(500).json(err);
		}
	}) ;
 


});

router.post('/boards/:boardID/DownloadDirectoryJSONVideo', function (req, res, next) {
	DownloadDirectory = require('./DownloadDirectory')
 
	var videoArray = req.body.videoArray;

	DownloadDirectory.reorderVideo(req.params.boardID, videoArray, function (err, data) {
		if (!err) {
			res.status(200).send(JSON.stringify(data));
		}
		else {
			res.send(err);
		}
	}
	);
});

/* warning: this will not maintain the length of mp3 files or other metadata 
IT DOES NOT WORK!!!*/
router.get('/boards/:boardID/genJSONFromFiles', function (req, res, next) {
	
		DownloadDirectory = require('./DownloadDirectory');
		DownloadDirectory.generateNewDirectoryJSON();
		res.status(200).send("OK");
	
	});

module.exports = router;
