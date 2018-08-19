
import FileSystemConfig from "./FileSystemConfig";
import BBComAPIData from "./BBComAPIData";
import BLEIDs from "./BLEIDs";
import BLEBoardData from "./BLEBoardData";
import Geolocation from "react-native-geolocation-service";

var bEmptyUserPrefs = {
	isDevilsHand: false,
	isBurnerMode: false,
	wifiLocations: false,
	mapPoints: false,
	includeMeOnMap: false,
	man: {
		latitude: 40.7866,
		longitude: -119.20660000000001,
	}
};

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
			[null, { channelNo: 1, channelInfo: "loading..." }]
	},
	video: {
		channelNo: 1,
		maxChannel: 1,
		channels: [null, { channelNo: 1, channelInfo: "loading..." }]
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
	phoneLocation: {
		latitude: 0,
		longitude: 0,
		boardId: "my phone",
		title: "my phone",
		dateTime: Date.now()
	}
};

exports.blankUserPrefs = function () {
	return JSON.parse(JSON.stringify(bEmptyUserPrefs));
};

function mblankUserPrefs() {
	return JSON.parse(JSON.stringify(bEmptyUserPrefs));
}

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
		mediaState = getBoardsInternal(mediaState); // don't wait!

		return mediaState;
	}
	catch (error) {
		console.log("StateBuilder: " + BLEIDs.fixErrorMessage(error));
	}
};

async function getBoardsInternal(mediaState) {
	try {
		var boards = await module.exports.getBoards();
		mediaState.boards = boards;
	}
	catch (error) {
		console.log("StateBuilder: " + error);
	}
	return mediaState;
}

exports.getBoards = async function (isBurnerMode) {
	try {
		var boards = null;

		// no wifi in burner mode
		// if (!isBurnerMode)
		boards = await BBComAPIData.fetchBoards();

		console.log(boards);
		
		if (boards) {
			await FileSystemConfig.setBoards(boards);
		}
		else {
			boards = await FileSystemConfig.getBoards();
		}

		if (boards)
			return boards;
		else
			return mblankUserPrefs();
	}
	catch (error) {
		console.log("StateBuilder: Error: " + error);
	}
	return boards;
};

exports.getUserPrefs = async function () {
	try {
		var userPrefs = await FileSystemConfig.getUserPrefs();

		if (userPrefs) {
			if (userPrefs.mapPoints != null
				&& userPrefs.isBurnerMode != null
				&& userPrefs.isDevilsHand != null
				&& userPrefs.wifiLocations != null
				&& userPrefs.man != null
				&& userPrefs.includeMeOnMap != null)
				return userPrefs;
			else
				return mblankUserPrefs();
		}
		else {
			return mblankUserPrefs();
		}
	}
	catch (error) {
		console.log("StateBuilder: " + error);
	}
};

exports.setUserPrefs = async function (userPrefs) {
	console.log("SET user prefs")
	console.log(userPrefs)
	await FileSystemConfig.setUserPrefs(userPrefs);
};

exports.getPhoneLocation = async function (mediaState) {
	mediaState.phoneLocation = await checkPhoneLocation();
	return mediaState;
};

exports.getLocationForMan = async function () {
	return await checkPhoneLocation();
};

function checkPhoneLocation() {
	return new Promise(function (resolve, reject) {
		Geolocation.getCurrentPosition(
			(position) => {
				resolve({ latitude: position.coords.latitude, longitude: position.coords.longitude, boardId: "my phone", dateTime: Date.now(), title: "my phone" });
			},
			(error) => {
				reject(error);
			},
			{ enableHighAccuracy: true, timeout: 20000, maximumAge: 1000 }, );
	});
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

			if (existingBoard[0]) {
				if (existingBoard.dateTime < currentBoard.dateTime) {
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


exports.getRegionForCoordinates = function (points) {
	// points should be an array of { latitude: X, longitude: Y }
	let minX, maxX, minY, maxY;

	// init first point
	((point) => {
		minX = point.latitude;
		maxX = point.latitude;
		minY = point.longitude;
		maxY = point.longitude;
	})(points[0]);

	// calculate rect
	points.map((point) => {
		minX = Math.min(minX, point.latitude);
		maxX = Math.max(maxX, point.latitude);
		minY = Math.min(minY, point.longitude);
		maxY = Math.max(maxY, point.longitude);
	});

	const midX = (minX + maxX) / 2;
	const midY = (minY + maxY) / 2;
	const deltaX = Math.max(0.01, (maxX - minX) * 2);
	const deltaY = Math.max(0.01, (maxY - minY) * 2);

	return {
		latitude: midX,
		longitude: midY,
		latitudeDelta: deltaX,
		longitudeDelta: deltaY,
		hasBeenAutoGenerated: true,
	};
};