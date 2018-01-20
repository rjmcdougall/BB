var express = require('express');
var router = express.Router();
var util = require('util')
var format = require('util').format;
var Multer = require('multer');
var bodyParser = require('body-parser');
var UserStore = require('./UserStore');

router.use(bodyParser.json());

router.use(async function (req, res, next) {

	var JWT = req.headers['x-access-token'];

	if (JWT) {
		try {
			var i = await UserStore.verifyJWT(JWT);
			next();
		}
		catch (err) {
			res.status(403).send(err.message.substr(0, 30) + "... Please Try Again.");
		}
	}
	else res.status(403).json({
		success: false,
		message: 'No token provided.'
	});
});

const upload = Multer({
	storage: Multer.memoryStorage(),
	limits: {
		fileSize: 20 * 1024 * 1024
	}
});

/* GET home page. */
router.get('/', function (req, res, next) {
	res.status(400).send("Not Found");
});

router.get('/boards', async function (req, res, next) {

	DownloadDirectoryDS = require('./DownloadDirectoryDS');
	try{
		var i = await DownloadDirectoryDS.listBoards();
		res.status(200).json(i);
	}
	catch(err){
		res.status(500).json(err.message);
	}
 

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

router.delete('/boards/:boardID/:mediaType/:mediaLocalName', async function (req, res, next) {

	var boardID = req.params.boardID;
	var mediaType = req.params.mediaType;	
	var mediaLocalName = req.params.mediaLocalName;

	FileSystem = require('./FileSystem');
	DownloadDirectoryDS = require('./DownloadDirectoryDS');

	try{
		var results = [];
		var boardExists = await DownloadDirectoryDS.boardExists(boardID);
		if(boardExists){
			var mediaExists = await(DownloadDirectoryDS.mediaExists(boardID, mediaType, mediaLocalName)) 
			if(mediaExists){
				results.push(await DownloadDirectoryDS.deleteMedia(boardID, mediaType, mediaLocalName));  
				results.push(await FileSystem.deleteMedia(boardID, mediaLocalName)); // implement me
			}
			else
				throw new Error(mediaType + " named " + mediaLocalName + " does not exist");
			res.status(200).json(results);				}
		else
			throw new Error("Board named " + boardID + " does not exist");
	}
	catch(err){
		res.status(500).json(err.message);
	}
});

router.get('/boards/AddBoard/:boardID', async function (req, res, next) {

	var newBoardID = req.params.boardID;

	FileSystem = require('./FileSystem');
	DownloadDirectoryDS = require('./DownloadDirectoryDS');

	try{
		var results = [];
		var boardExists = await DownloadDirectoryDS.boardExists(newBoardID);
		if(!boardExists){
			results.push(await DownloadDirectoryDS.createNewBoard(newBoardID));
			results.push(await DownloadDirectoryDS.createNewBoardMedia(newBoardID, 'video'));
			results.push(await DownloadDirectoryDS.createNewBoardMedia(newBoardID, 'audio'));
			results.push(await FileSystem.createRootBoardFolder(newBoardID));
			res.status(200).json(results);				}
		else
			throw new Error("Board named " + newBoardID + " already exists");
	}
	catch(err){
		res.status(500).json(err.message);
	}
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
