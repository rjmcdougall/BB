var express = require('express');
var router = express.Router();
var util = require('util')
var format = require('util').format;
var bodyParser = require('body-parser');
var UserStore = require('./UserStore');

router.use(bodyParser.json());

router.use(async function (req, res, next) {

	if(!(req.path.endsWith('/DownloadDirectoryJSON') && req.path.startsWith('/boards/')))
	{

		var JWT = req.headers['authorization'].replace("Bearer ", "");

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
	}
	else
		next();

});

router.get('/', function (req, res, next) {
	res.status(400).send("Not Found");
});

router.get('/boards/', async function (req, res, next) {

	DownloadDirectoryDS = require('./DownloadDirectoryDS');
	try {
		var i = await DownloadDirectoryDS.listBoards(null);
		res.status(200).json(i);
	}
	catch (err) {
		res.status(500).json(err.message);
	}
});

router.get('/boards/:boardID', async function (req, res, next) {

	var boardID = req.params.boardID;

	DownloadDirectoryDS = require('./DownloadDirectoryDS');
	try {
		var i = await DownloadDirectoryDS.listBoards(boardID);
		res.status(200).json(i);
	}
	catch (err) {
		res.status(500).json(err.message);
	}
});

router.post('/boards/:boardID/profiles/:profileID', async function (req, res, next) {

	var boardID = req.params.boardID; 
	var profileID = req.params.profileID;
	var cloneFromBoardID = null;

	if(req.body.cloneFromBoardID !== "GLOBAL")
		cloneFromBoardID = req.body.cloneFromBoardID;

	cloneFromProfileID = req.body.cloneFromProfileID;

	DownloadDirectoryDS = require('./DownloadDirectoryDS');
	FileSystem = require('./FileSystem');

	var results = [];

	try {
		var profileExists = await DownloadDirectoryDS.profileExists(boardID, profileID);
		if (!profileExists) {
			results.push(await DownloadDirectoryDS.createProfile(boardID, profileID, false));
			
			if(cloneFromBoardID != "NONE" && cloneFromProfileID != "NONE"){
 				results.push(await FileSystem.copyProfileFiles(boardID, profileID, cloneFromBoardID, cloneFromProfileID));
			}
			results.push(await DownloadDirectoryDS.cloneBoardMedia (boardID, profileID, cloneFromBoardID, cloneFromProfileID, 'audio'));
			results.push(await DownloadDirectoryDS.cloneBoardMedia (boardID, profileID, cloneFromBoardID, cloneFromProfileID, 'video'));
		}
		else
			throw new Error("the profile already exists");
		res.status(200).json(results[0]);
	}
	catch (err) {
		res.status(500).json(err.message);
	}

});

router.post('/profiles/:profileID', async function (req, res, next) {

	var profileID = req.params.profileID;
	var cloneFromBoardID = null;

	if(req.body.cloneFromBoardID !== "GLOBAL")
		cloneFromBoardID = req.body.cloneFromBoardID;

	cloneFromProfileID = req.body.cloneFromProfileID;

	DownloadDirectoryDS = require('./DownloadDirectoryDS');
	FileSystem = require('./FileSystem');

	var results = [];

	try {
		var profileExists = await DownloadDirectoryDS.profileExists(null, profileID);
		if (!profileExists) {
			results.push(await DownloadDirectoryDS.createProfile(null, profileID, true));
			
			if(cloneFromBoardID != "NONE" && cloneFromProfileID != "NONE"){
 				results.push(await FileSystem.copyProfileFiles(null, profileID, cloneFromBoardID, cloneFromProfileID));
			}
			results.push(await DownloadDirectoryDS.cloneBoardMedia (null, profileID, cloneFromBoardID, cloneFromProfileID, 'audio'));
			results.push(await DownloadDirectoryDS.cloneBoardMedia (null, profileID, cloneFromBoardID, cloneFromProfileID, 'video'));
		}
		else
			throw new Error("the profile already exists");
		res.status(200).json(results[0]);
	}
	catch (err) {
		res.status(500).json(err.message);
	}
});

router.delete('/profiles/:profileID', async function (req, res, next) {

	var profileID = req.params.profileID;

	DownloadDirectoryDS = require('./DownloadDirectoryDS');
	FileSystem = require('./FileSystem');

	try {
		var profileExists = await DownloadDirectoryDS.profileExists(null, profileID);
		var i;
		if (profileExists) {
			i = await DownloadDirectoryDS.deleteMedia(null, profileID, "audio", null);
			i = await DownloadDirectoryDS.deleteMedia(null, profileID, "video", null);
			i = await DownloadDirectoryDS.deleteProfile(null, profileID);
			i = await FileSystem.deleteProfile(null, profileID);
		}
		else
			throw new Error("the profile " + profileID + " does not exist");
		res.status(200).json(i);
	}
	catch (err) {
		res.status(500).json(err.message);
	}
});

router.delete('/boards/:boardID/profiles/:profileID', async function (req, res, next) {

	var boardID = req.params.boardID;
	var profileID = req.params.profileID;

	DownloadDirectoryDS = require('./DownloadDirectoryDS');
	FileSystem = require('./FileSystem');

	try {
		var profileExists = await DownloadDirectoryDS.profileExists(boardID, profileID);
		var i;
		if (profileExists) {
			i = await DownloadDirectoryDS.deleteMedia(boardID, profileID, "audio", null);
			i = await DownloadDirectoryDS.deleteMedia(boardID, profileID, "video", null);
			i = await DownloadDirectoryDS.deleteProfile(boardID, profileID);
			i = await FileSystem.deleteProfile(boardID, profileID);
		}
		else
			throw new Error("the profile " + profileID + " does not exist");
		res.status(200).json(i);
	}
	catch (err) {
		res.status(500).json(err.message);
	}
});

router.get('/allProfiles/', async function (req, res, next) {

	DownloadDirectoryDS = require('./DownloadDirectoryDS');
	try {
		var i = await DownloadDirectoryDS.listProfiles(null);
		res.status(200).json(i);
	}
	catch (err) {
		res.status(500).json(err.message);
	}
});

router.get('/profiles/', async function (req, res, next) {

	DownloadDirectoryDS = require('./DownloadDirectoryDS');
	try {
		var i = await DownloadDirectoryDS.listGlobalProfiles();
		res.status(200).json(i);
	}
	catch (err) {
		res.status(500).json(err.message);
	}
});

router.get('/boards/:boardID/profiles', async function (req, res, next) {

	var boardID = req.params.boardID;

	DownloadDirectoryDS = require('./DownloadDirectoryDS');
	try {
		var i = await DownloadDirectoryDS.listProfiles(boardID);
		res.status(200).json(i);
	}
	catch (err) {
		res.status(500).json(err.message);
	}
});

router.get('/currentStatuses', async function (req, res, next) {

	try {
		var i = 1;

		BatteryQueries = require('./BatteryQueries')
		var results = [];

		results = await BatteryQueries.queryBatteryData();
		res.status(200).json(results);
	}
	catch (err) {
		res.status(500).json(err);
	}
});

router.get('/boards/:boardID/batteryHistory', async function (req, res, next) {

	BatteryQueries = require('./BatteryQueries')
	var results = [];

	try {
		results = await BatteryQueries.queryBatteryHistory(req.params.boardID);
		res.status(200).json(results);
	}
	catch (err) {
		res.status(500).json(err);
	}
});

router.get('/boards/:boardID/profiles/:profileID/listFiles', async function (req, res, next) {

	FileSystem = require('./FileSystem');

	var boardID = req.params.boardID;
	var profileID = req.params.profileID;
	var results = [];

	try {
		results.push(await FileSystem.listProfileFiles(boardID, profileID));
		res.status(200).json(results);
	}
	catch (err) {
		res.status(500).json(err);
	}

});

router.post('/boards/:boardID/activeProfile/:profileID/isGlobal/:isGlobal', async function (req, res, next) {

	DownloadDirectoryDS = require('./DownloadDirectoryDS');

	var boardID = req.params.boardID;
	var profileID = req.params.profileID;
	var isGlobal = req.params.isGlobal == "true";


	try {
		var boardExists = await DownloadDirectoryDS.boardExists(boardID);
		if (boardExists) {
			var result = await DownloadDirectoryDS.activateBoardProfile(boardID, profileID, isGlobal);
			res.status(200).json(result);
		}
		else {
			throw new Error("Board named " + boardID + " does not exist");
		}
	}
	catch (err) {
		res.status(500).json(err);
	}

});

router.get('/boards/:boardID/DownloadDirectoryJSON', async function (req, res, next) {
	DownloadDirectoryDS = require('./DownloadDirectoryDS');
	var boardID = req.params.boardID;
	var result = [];
	try {
		var boardExists = await DownloadDirectoryDS.boardExists(boardID);
		if (boardExists) {
			// get the default profile
			var profileID = await DownloadDirectoryDS.listBoards(boardID);

			// is the deault profile global? if so, null it out!
			if(profileID[0].isProfileGlobal)
				boardID = null;

			result = await DownloadDirectoryDS.DirectoryJSON(boardID, profileID[0].profile);
			res.status(200).json(result);
		}
		else {
			throw new Error("Board named " + boardID + " does not exist");
		}
	}
	catch (err) {
		res.status(500).json(err);
	}

});

router.get('/boards/:boardID/profiles/:profileID/DownloadDirectoryJSON', async function (req, res, next) {
	DownloadDirectoryDS = require('./DownloadDirectoryDS');
	var boardID = req.params.boardID;
	var profileID = req.params.profileID;
	var result = [];
	try {
		var boardExists = await DownloadDirectoryDS.boardExists(boardID);
		if (boardExists) {
			result = await DownloadDirectoryDS.DirectoryJSON(boardID, profileID);
			res.status(200).json(result);
		}
		else {
			throw new Error("Board named " + boardID + " does not exist");
		}
	}
	catch (err) {
		res.status(500).json(err);
	}

});

router.get('/profiles/:profileID/DownloadDirectoryJSON', async function (req, res, next) {
	DownloadDirectoryDS = require('./DownloadDirectoryDS');
	var profileID = req.params.profileID;
	var result = [];
	try {
		result = await DownloadDirectoryDS.DirectoryJSON(null, profileID);
		res.status(200).json(result);
	}
	catch (err) {
		res.status(500).json(err);
	}

});

router.delete('/profiles/:profileID/:mediaType/:mediaLocalName', async function (req, res, next) {

	var boardID = null;
	var profileID = req.params.profileID;
	var mediaType = req.params.mediaType;
	var mediaLocalName = req.params.mediaLocalName;

	FileSystem = require('./FileSystem');
	DownloadDirectoryDS = require('./DownloadDirectoryDS');

	try {
		var results = [];
		var mediaExists = await (DownloadDirectoryDS.mediaExists(boardID, profileID, mediaType, mediaLocalName))
		if (mediaExists) {
			results = await DownloadDirectoryDS.deleteMedia(boardID, profileID, mediaType, mediaLocalName);
			results = await FileSystem.deleteMedia(boardID, profileID, mediaLocalName);
		}
		else
			throw new Error(mediaType + " named " + mediaLocalName + " does not exist");
		res.status(200).json(results);

	}
	catch (err) {
		res.status(500).json(err.message);
	}
});

router.delete('/boards/:boardID/profiles/:profileID/:mediaType/:mediaLocalName', async function (req, res, next) {

	var boardID = req.params.boardID;
	var profileID = req.params.profileID;
	var mediaType = req.params.mediaType;
	var mediaLocalName = req.params.mediaLocalName;

	FileSystem = require('./FileSystem');
	DownloadDirectoryDS = require('./DownloadDirectoryDS');

	try {
		var results = [];
		var boardExists = await DownloadDirectoryDS.boardExists(boardID);
		if (boardExists) {
			var mediaExists = await (DownloadDirectoryDS.mediaExists(boardID, profileID, mediaType, mediaLocalName))
			if (mediaExists) {
				results = await DownloadDirectoryDS.deleteMedia(boardID, profileID, mediaType, mediaLocalName);
				results = await FileSystem.deleteMedia(boardID, profileID, mediaLocalName);
			}
			else
				throw new Error(mediaType + " named " + mediaLocalName + " does not exist");
			res.status(200).json(results);
		}
		else
			throw new Error("Board named " + boardID + " does not exist");
	}
	catch (err) {
		res.status(500).json(err.message);
	}
});

router.delete('/boards/:boardID', async function (req, res, next) {

	var boardID = req.params.boardID;

	FileSystem = require('./FileSystem');
	DownloadDirectoryDS = require('./DownloadDirectoryDS');

	try {
		var results = [];
		var boardExists = await DownloadDirectoryDS.boardExists(boardID);
		if (boardExists) {
			results.push(await DownloadDirectoryDS.deleteAllBoardMedia(boardID, "audio"));
			results.push(await DownloadDirectoryDS.deleteAllBoardMedia(boardID, "video"));
			results.push(await DownloadDirectoryDS.deleteProfile(boardID, null));
			results.push(await DownloadDirectoryDS.deleteBoard(boardID));
			results.push(await FileSystem.deleteBoard(boardID));
			res.status(200).json(results);
		}
		else
			throw new Error("Board named " + boardID + " does not exist");
	}
	catch (err) {
		res.status(500).json(err.message);
	}
});

router.get('/boards/AddBoard/:boardID', async function (req, res, next) {

	var newBoardID = req.params.boardID;

	FileSystem = require('./FileSystem');
	DownloadDirectoryDS = require('./DownloadDirectoryDS');

	try {
		var results = [];
		var boardExists = await DownloadDirectoryDS.boardExists(newBoardID);
		if (!boardExists) {
			results.push(await DownloadDirectoryDS.createNewBoard(newBoardID));
			results.push(await DownloadDirectoryDS.createNewBoardMedia(newBoardID, "video"));
			results.push(await DownloadDirectoryDS.createNewBoardMedia(newBoardID, 'audio'));
			results.push(await DownloadDirectoryDS.createProfile(newBoardID, "default", false))
			results.push(await FileSystem.createRootBoardFolder(newBoardID));
			res.status(200).json(results);
		}
		else
			throw new Error("Board named " + newBoardID + " already exists");
	}
	catch (err) {
		res.status(500).json(err.message);
	}
});

router.post('/profiles/:profileID/:mediaType/ReorderMedia', async function (req, res, next) {

	DownloadDirectoryDS = require('./DownloadDirectoryDS');

	var mediaArray = req.body.mediaArray;
	var boardID = null;
	var profileID = req.params.profileID;
	var mediaType = req.params.mediaType;
	var results = [];

	try {
		results = await DownloadDirectoryDS.reorderMedia(boardID, profileID, mediaType, mediaArray);
		res.status(200).json(results);
	}
	catch (err) {
		res.status(500).json(err);
	}

});

router.post('/boards/:boardID/profiles/:profileID/:mediaType/ReorderMedia', async function (req, res, next) {

	DownloadDirectoryDS = require('./DownloadDirectoryDS');

	var mediaArray = req.body.mediaArray;
	var boardID = req.params.boardID;
	var profileID = req.params.profileID;
	var mediaType = req.params.mediaType;
	var results = [];

	try {
		results = await DownloadDirectoryDS.reorderMedia(boardID, profileID, mediaType, mediaArray);
		res.status(200).json(results);
	}
	catch (err) {
		res.status(500).json(err);
	}

});

router.post('/profiles/:profileID/AddFileFromGDrive', async function (req, res, next) {

	var oAuthToken = req.body.oauthToken;

	var fileId = req.body.fileId;
	var profileID = req.params.profileID;
	var results = [];

	FileSystem = require('./FileSystem');

	try {
		results = await FileSystem.addGDriveFile(null, profileID, fileId, oAuthToken, "");
		res.status(200).json(results);
	}
	catch (err) {
		if (err.message.indexOf("already exists for profile") > -1)
			res.status(409).send(err.message)
		else
			res.status(500).json(err.message);
	}
});

router.post('/boards/:boardID/profiles/:profileID/AddFileFromGDrive', async function (req, res, next) {

	var oAuthToken = req.body.oauthToken;

	var fileId = req.body.fileId;
	var currentBoard = req.params.boardID;
	var profileID = req.params.profileID;
	var results = [];

	FileSystem = require('./FileSystem');

	try {
		results = await FileSystem.addGDriveFile(currentBoard, profileID, fileId, oAuthToken);
		res.status(200).json(results);
	}
	catch (err) {
		if (err.message.indexOf("already exists for board") > -1)
			res.status(409).send(err.message)
		else
			res.status(500).json(err.message);
	}
});

module.exports = router;
