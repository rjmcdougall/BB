
import FileSystemConfig from "./FileSystemConfig";
import BBComAPIData from "./BBComAPIData";
import BLEIDs from "./BLEIDs";
import BLEBoardData from "./BLEBoardData";

var bEmptyMediaState = {
	peripheral: {
		name: "loading...",
		id: "12345",
		connected: false,
	},
	audio: {
		channelNo: 1,
		maxChannel: 1,
		volume: 0,
		channels:
			[{ channelNo: 1, channelInfo: "loading..." }]
	},
	video: {
		channelNo: 1,
		maxChannel: 1,
		channels: [{ channelNo: 1, channelInfo: "loading..." }]
	},
	device: {
		deviceNo: 1,
		maxDevice: 1,
		devices: [{ deviceNo: 1, deviceInfo: "loading...", deviceLabel: "loading...", isPaired: false, }]
	},
	battery: 0,
	audioMaster: 0,
	APKUpdateDate: 0,
	APKVersion: 0,
	region: {
		latitude: 37.78825,
		longitude: -122.4324,
		latitudeDelta: 0.0922,
		longitudeDelta: 0.0922,
		hasBeenAutoGenerated: false,
	},
	locations: [],
	apiLocations: [],
	isError: false,
	logLines: [{ logLine: "", isError: false }],
	boards: [{ name: "none", address: 1234 }],
};

exports.blankMediaState = function () {
	return JSON.parse(JSON.stringify(bEmptyMediaState));
};

function mblankMediaState() {
	return JSON.parse(JSON.stringify(bEmptyMediaState));
}

exports.createMediaState = async function (peripheral) {
	try {
		var mediaState = mblankMediaState();
		mediaState.peripheral = peripheral;

		mediaState = BLEIDs.BLELogger(mediaState, "StateBuilder: Getting BLE Data for " + peripheral.name, false);
		mediaState = await BLEBoardData.refreshMediaState(mediaState);

		mediaState = BLEIDs.BLELogger(mediaState, "StateBuilder: Gettig Boards Data from API ", false);
		mediaState = getBoards(mediaState); // don't wait!

		return mediaState;
	}
	catch (error) {
		console.log("StateBuilder: " + BLEIDs.fixErrorMessage(error));
	}
};

async function getBoards(mediaState) {
	try {
		var boards = await BBComAPIData.fetchBoards();

		if (boards) {
			await FileSystemConfig.setBoards(boards);
			mediaState.boards = boards;
		}
		else {
			var fileBoards = await FileSystemConfig.getBoards();
			if (fileBoards)
				mediaState.boards = fileBoards;
		}
	}
	catch (error) {
		console.log(error);
	}
	return mediaState;
}

exports.getLocations = function (mediaState, showAPILocations) {

	if (showAPILocations) {
		var locations = [...mediaState.locations, ...mediaState.apiLocations];
		var afterLocations = Array();
		var currentBoard;
		var existingBoard;
 
		for (var i = 0; i < locations.length; i++) {
			currentBoard = locations[i];

			// if it  exists
			existingBoard = afterLocations.filter((item) => {
				return currentBoard.boardId == item.boardId;
			});

			if(existingBoard[0]) {
				if(existingBoard.dateTime < currentBoard.dateTime){
					// remove it and add the new one
					afterLocations = afterLocations.filter((board) => {
						return board.boardId != existingBoard[0].boardId;
					});
					afterLocations.push(currentBoard);
				}
			}
			else {
				// add it
				afterLocations.push(currentBoard);
			}
		}
		return afterLocations;
	}
	else {
		return mediaState.locations;
	}
};