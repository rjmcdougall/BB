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

// router.get('/boards/:boardID/upload', (req, res, next) => {
// 	res.render('uploadForm', { title: 'burnerboard.com', boardID: req.params.boardID });
// });

// router.post('/boards/:boardID/upload', upload.single('file'), (req, res, next) => {
// 	if (!req.file) {
// 		res.status(400).send('No file uploaded.');
// 		return;
// 	}

// 	FileSystem = require('./FileSystem');

// 	FileSystem.addFile(req.params.boardID, req.file, req.body.speechCue, function (err, savedFile) {
// 		if (!err) {
// 			res.status(200).json(savedFile);
// 		}
// 		else {
// 			res.status(500).send("ERROR");
// 		}
// 	})
// });

router.get('/boards/:boardID/listFiles', function (req, res, next) {

	FileSystem = require('./FileSystem');

	FileSystem.listFiles(req.params.boardID)
		.then(result => {
			res.status(200).json(result);
		})
		.catch(function (err) {
			res.status(500).json(err);
		});
});

router.get('/boards/TestDatastore', function (req, res, next) {
	DownloadDirectoryDS = require('./DownloadDirectoryDS');

	var boardID = 'vega';
	// var fileName = 'BurnerBoardMedia/vega/AvenerMix2551.mp3';
	// var fileSize = 48786286;
	// var fileLength = 1220;
	// var speechCue = "Tunnels";

	// DownloadDirectoryDS.addMedia(boardID, 'video', fileName, fileSize, fileLength, speechCue)
	// 	.then(result => {
	// 		res.status(200).send(result);
	// 	})
	// 	.catch(function(err){
	// 		res.status(500).send(err.message);
	// 	})

	// var mediaType = 'audio';
	// DownloadDirectoryDS.listMedia (boardID, mediaType)
	// 	.then(result => {
	// 		res.status(200).json(result);
	// 	})
	// 	.catch(function(err){
	// 		res.status(500).json(err);
	// 	});

	//var mediaType = 'audio';
	// DownloadDirectoryDS.DirectoryJSON(boardID)
	// 	.then(result => {
	// 		res.status(200).json(result);
	// 	})
	// 	.catch(function (err) {
	// 		res.status(500).json(err);
	// 	});
	
	var mediaType = 'video';
	var mediaArray = [
		
		'AvenerMix2551.mp3',
		'AvenerMix251.mp3',
		'AvenerMix21.mp3'
	]
	DownloadDirectoryDS.reorderMedia(boardID, mediaType, mediaArray)
		.then(result => {
			res.status(200).json(result);
		})
		.catch(function (err) {
			res.status(500).json(err);
		});
});

router.get('/boards/:boardID/DownloadDirectoryJSON', function (req, res, next) {
	DownloadDirectoryDS = require('./DownloadDirectoryDS');

	DownloadDirectoryDS.DirectoryJSON (req.params.boardID) 
		.then(result => {
			res.status(200).json(result);
		})
		.catch(function (err) {
			res.status(500).json(err);
		});
 
});

router.get('/boards/AddBoard/:boardID', function (req, res, next) {

	var newBoardID = req.params.boardID;

	// FileSystem = require('./FileSystem');

	// FileSystem.createRootBoardFolder(newBoardID)
	// 	.then(result => {
	// 		res.status(200).json(result);
	// 	})
	// 	.catch(function (err) {
	// 		res.status(500).json(err);
	// 	});

	DownloadDirectoryDS = require('./DownloadDirectoryDS');
	DownloadDirectoryDS.createNewBoard(newBoardID)
		.then(result => {
			res.status(200).json(result);
		})
		.catch(function (err) {
			res.status(500).json(err);
		});
 
});
 
router.post('/boards/:boardID/ReorderMedia', function (req, res, next) {

	DownloadDirectoryDS = require('./DownloadDirectoryDS');
 
	var mediaArray = req.body.mediaArray;
	var mediaType = req.body.mediaType;

	DownloadDirectoryDS.reorderMedia(req.params.boardID, mediaType, mediaArray) 
	.then(result => {
		res.status(200).json(result);
	})
	.catch(function (err) {
		res.status(500).json(err);
	});
 
});

router.post('/boards/:boardID/AddFileFromGDrive', function (req, res, next) {

//	var oAuthToken = 'ya29.Glw0BTczgRoygNAePtuBdxMqUMYgzwMCy6yr2LvZ4VQfHV8vlgd_nYB6HvXkXvC-QpLvdLdEAKFF9fMEkFHLzumj0clizXBsOpAQT7dTKqEV1kodOo0RKlRK_4IrPA';
var oAuthToken = req.body.oauthToken;

//	var fileId = '1aSXoRfidHo33nlVRDha9xdrKaPKeQJCS';
	var fileId = req.body.fileId;
	var currentBoard = req.body.currentBoard;
//	var currentBoard = 'vega';

	FileSystem = require('./FileSystem');

	FileSystem.addGDriveFile(currentBoard, fileId, oAuthToken, "")
		.then(result => {
			res.status(200).json(result);
		})
		.catch(function (err) {
			if(err.message.indexOf("already exists for board") > -1)  
				res.status(409).send(err.message)
			else
				res.status(500).json(err.message);
		});
 


});
 
module.exports = router;
