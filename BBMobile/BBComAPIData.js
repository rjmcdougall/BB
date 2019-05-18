import BLEIDs from "./BLEIDs";

exports.fetchBoards = async function () {

	//const API = "http://www.fakeresponse.com/api/?sleep=5";
	//const API = "https://us-central1-burner-board.cloudfunctions.net/boards";
	const API = "https://www.burnerboard.com/boards/";
	try {
		var response = await advFetch(API, {
			headers: {
				"Accept": "application/json",
				"Content-Type": "application/json",
			}
		});

		var boardsJSON = await response.json();
		var boardsText = await JSON.stringify(boardsJSON);

		if (boardsText.length > 20) // make sure it isn't empty.
			return boardsJSON;
		else
			return null;

	}
	catch (error) {
		console.log(error);
		return null;
	}
};

async function advFetch (url, headers) {
	const TIMEOUT = 2000;
	let didTimeOut = false;

	return new Promise(function (resolve, reject) {
		const timeout = setTimeout(() => {
			didTimeOut = true;
			reject(new Error('Request timed out'));
		}, TIMEOUT);

		fetch(url, headers).then(function (response) {
			clearTimeout(timeout);
			if (!didTimeOut) {
				resolve(response);
			}
		})
			.catch(function (err) {
				if (didTimeOut) {
					return;
				}
				reject(err);
			});
	})
}

exports.fetchLocations = async function (mediaState) {

	//const API = "http://192.168.1.66:3001/boards/locations/";
	const API = "https://www.burnerboard.com/boards/locations/";

	try {
		var response = await advFetch(API, {
			headers: {
				"Accept": "application/json",
				"Content-Type": "application/json",
			}
		});

		var apiLocations = await response.json();

		mediaState.apiLocations = apiLocations.map((board) => {
			return {
				board: board.board,
				latitude: board.lat,
				longitude: board.lon,
				dateTime: board.time,
			};
		});
		mediaState = BLEIDs.BLELogger(mediaState, "API: Locations Fetch Found " + mediaState.apiLocations.length + " boards", false);

		return mediaState;
	}
	catch (error) {
		mediaState = BLEIDs.BLELogger(mediaState, "API: Locations: " + error, true);
		return mediaState;
	}
};