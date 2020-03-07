var express = require("express");
 
const app = express();
const PORT = 5555;
const DownloadDirectoryDS = require("./DownloadDirectoryDS");
const BatteryQueries = require("./BatteryQueries");

app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

app.get("/", async function (req, res, next) {
	console.log(req.protocol + "://"+ req.get('Host') + req.url);
	try {
        var i = await DownloadDirectoryDS.listBoards(null);
		res.status(200).json(i);
	}
	catch (err) {
		res.status(500).json(err.message);
	}
});

app.get("/locations/", async function (req, res, next) {
	console.log(req.protocol + "://"+ req.get('Host') + req.url);
	try {
		var i = await BatteryQueries.queryBoardLocations();
		res.status(200).json(i);
	}
	catch (err) {
		res.status(500).json(err.message);
	}
});

app.get("/CreateBoard/:deviceID", async function (req, res, next) {
	console.log(req.protocol + "://"+ req.get('Host') + req.url);
	var deviceID = req.params.deviceID;

	const DownloadDirectoryDS = require("./DownloadDirectoryDS");
	 
	try {
		var i = await DownloadDirectoryDS.createNewBoard(deviceID);
		res.status(200).json(i);
	}
	catch (err) {
		res.status(500).json(err.message);
	}
});

app.get("/:boardID/DownloadDirectoryJSON", async function (req, res, next) {
	console.log(req.protocol + "://"+ req.get('Host') + req.url);

	var boardID = req.params.boardID;
	var result = [];
	try {
		var boardExists = await DownloadDirectoryDS.boardExists(boardID);
		if (boardExists) {
			// get the default profile
			var profileID = await DownloadDirectoryDS.listBoards(boardID);

			// is the deault profile global? if so, null it out!
			if (profileID[0].isProfileGlobal)
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

/**
 * Returns a list of Google APIs and the link to the API's docs.
 * @param {Express.Request} req The API request.
 * @param {Express.Response} res The API response.
 */
listGoogleAPIs = async (req, res) => {
	let sum = 0;
	for (let i = 0; i < 10; ++i) {
	  sum += i;
	}
	res.send({ sum });
  };

module.exports = {
	app,
	listGoogleAPIs
};
