import BLEIDs from "./BLEIDs";

exports.fetchBoards = async function () {

	//const API = "http://www.fakeresponse.com/api/?sleep=5";
	const API = "https://www.burnerboard.com/boards/";
	try {
		var response = await fetch(API, {
			headers: {
				"Accept": "application/json",
				"Content-Type": "application/json",
			}
		});
		var boardsText = await response.text();
		var boardsJSON = await response.json();

		if(boardsText.length > 20) // make sure it isn't empty.
			return boardsJSON;
		else
			return null;

	}
	catch (error) {
		console.log(error);
		return null;
	}
};

exports.fetchLocations = async function (mediaState) {

	//const API = "http://192.168.1.66:3001/boards/locations/";
	const API = "https://www.burnerboard.com/boards/locations/";
	
	try {	
		var response = await fetch(API, {
			headers: {
				"Accept": "application/json",
				"Content-Type": "application/json",
			}
		});

		var apiLocations = await response.json();

		mediaState.apiLocations = apiLocations.map((board) => {
			return {
				title: board.board,
				boardId: board.board,
				latitude: board.lat,
				longitude: board.lon,
				dateTime:  board.time,
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