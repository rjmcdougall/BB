
import FileSystemConfig from "./FileSystemConfig";
import BBComAPIData from "./BBComAPIData";
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
	connectedPeripheral: {
		name: "loading...",
		id: "12345",
		connState: "DISCONNECTED",
	},
	//audio: [null, { channelNo: 1, channelInfo: "loading..." }],
	audio: [{ localName: "loading..." }],
	video: [{ localName: "loading..." }],
	devices: [{ name: "loading...", address: "loading...", isPaired: false, }],
	state: {
		audioChannelNo: 9999,
		videoChannelNo: 9999,
		volume: 0,
		battery: 0,
		audioMaster: 0,
		APKUpdateDate: 0,
		APKVersion: 0,
		IPAddress: "0.0.0.0",
		GTFO: false,
	},
	region: {
		latitude: 37.78825,
		longitude: -122.4324,
		latitudeDelta: 0.0922,
		longitudeDelta: 0.0922,
		hasBeenAutoGenerated: false,
	},
	locations: [], // [{"board":"unknown","latitude":37.476222,"longitude":-122.1551087,"address":41,"lastHeard":811071,"lastHeardDate":1555174780789,"sigStrength":-53}]
	apiLocations: [], //[{board: "sexy", latitude: 37.759305, longitude: -122.450425, dateTime: "2019-04-02T04:50:31.488000"}]
	isError: false,
	logLines: [{ logLine: "", isError: false }],
	boards: [{ name: "none", address: 1234 }], //    { "color": "coral", "address": 42424, "isProfileGlobal": true, "profile": "Small-Testing","name": "BLUE DASH M2",  "type": "tester"},
	phoneLocation: {
		latitude: 0,
		longitude: 0,
		board: "my phone",
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
 
exports.getBoards = async function () {
	try {
		var boards = null;

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
			return null;
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
	console.log("SET user prefs");
	console.log(userPrefs);
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
				resolve({ latitude: position.coords.latitude, longitude: position.coords.longitude, board: "my phone", dateTime: Date.now()});
			},
			(error) => {
				reject(error);
			},
			{ enableHighAccuracy: true, timeout: 20000, maximumAge: 1000 });
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
				return currentBoard.board == item.board;
			});

			if (existingBoard[0]) {
				if (existingBoard.dateTime < currentBoard.dateTime) {
					// remove it and add the new one
					afterLocations = afterLocations.filter((board) => {
						return board.board != existingBoard[0].board;
					});
					afterLocations.push(currentBoard);
				}
			}
			else {
				// add it
				afterLocations.push(currentBoard);
			}
		}
		console.log("after locations:");
		console.log(afterLocations);
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
