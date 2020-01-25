import Constants from "./Constants";

var bEmptyUserPrefs = {
	isDevilsHand: false,
	isBurnerMode: false,
	wifiLocations: false,
	mapPoints: false,
	isMonitor: false,
	locationHistoryMinutes: "1",
	offlineMapPercentage: 0,
};

var bMap = {
	center: Constants.MAN_LOCATION,
	zoom: 13,
	userLocation: Constants.MAN_LOCATION,
};

var bEmptyLogLines = [{ logLine: "", isError: false }];

var bBoardData = [{ name: "none", address: 1234 }]; //    { "color": "coral", "address": 42424, "isProfileGlobal": true, "profile": "Small-Testing","name": "BLUE DASH M2",  "type": "tester"},

var bLocations = [];

var bPeripheral = {
	name: "loading...",
	id: "12345",
	connectionStatus: Constants.DISCONNECTED,
};
var bAudio = [{ localName: "loading..." }];
var bVideo = [{ localName: "loading..." }];
var bDevices = [{ name: "loading...", address: "loading...", isPaired: false, }];
var bWifi = [];

var bBoardState = {
	acn: 9999, // audio channel number
	vcn: 9999,
	v: -1, // volume
	b: 0, // battery level
	am: 0, // audio master
	apkd: 0, // apk updated date
	apkv: 0, // apk version number
	ip: "0.0.0.0", // ip address
	g: false, // GTFO
	bm: false, //block master
	s: "", // SSID
	c: "zzzz", // configured ssid
	p: "", // configured password
	r: false, //crisis mode
};

exports.blankWifi = function() {
	return JSON.parse(JSON.stringify(bWifi));
};
exports.blankBoardState = function() {
	return JSON.parse(JSON.stringify(bBoardState));
};
exports.blankDevices = function() {
	return JSON.parse(JSON.stringify(bDevices));
};
exports.blankAudio = function() {
	return JSON.parse(JSON.stringify(bAudio));
};
exports.blankVideo = function() {
	return JSON.parse(JSON.stringify(bVideo));
};

exports.blankPeripheral = function() {
	return JSON.parse(JSON.stringify(bPeripheral));
};

exports.blankBoardData = function() {
	return JSON.parse(JSON.stringify(bBoardData));
};

exports.blankLocations = function() {
	return JSON.parse(JSON.stringify(bLocations));
};

exports.blankMap = function() {
	return JSON.parse(JSON.stringify(bMap));
};

exports.blankLogLines = function () {
	return JSON.parse(JSON.stringify(bEmptyLogLines));
};
exports.blankUserPrefs = function () {
	return JSON.parse(JSON.stringify(bEmptyUserPrefs));
};

exports.boardColor = function (item, boardData) {

	var color = "whitesmoke";

	var foundBoard = boardData.filter((board) => {
		if (board.name)
			return board.name == item;
		else if (board.bootName)
			return board.bootName == item;
		else
			return false;
	});
 
	if (foundBoard[0]) {
		if (foundBoard[0].color) {
			color = foundBoard[0].color;
		}
	}
	return color;
};

