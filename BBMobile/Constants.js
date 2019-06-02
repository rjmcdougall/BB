import { Platform } from "react-native";
import geoViewport from "@mapbox/geo-viewport";

//UUIDs
exports.bbUUID = "58fdc6ee-15d1-11e8-b642-0ed5f89f718b";
exports.UARTservice = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
exports.rxCharacteristic = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
exports.txCharacteristic = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
exports.CCCD = "00002902-0000-1000-8000-00805f9b34fb";

//connection states.
exports.DISCONNECTED = "Disconnected";
exports.CONNECTED = "Connected";
exports.CONNECTING = "Connecting";
exports.LOADED = "Loaded";

// The Screens
exports.DISCOVER = "Discover";
exports.MEDIA_MANAGEMENT = "Media Management";
exports.ADMINISTRATION = "Administration";
exports.DIAGNOSTIC = "Diagnostic";
exports.MAP = "Map";
exports.APP_MANAGEMENT = "App";
exports.MAP2 = "Map2";

//The Cache Items
exports.DEFAULT_PERIPHERAL = "DefaultPeripheral";
exports.BOARDS = "Boards";
exports.USER_PREFS = "UserPrefs";
exports.AUDIOPREFIX = "Audio_";
exports.VIDEOPREFIX = "Video_";
exports.BTDEVICESPREFIX = "BTDevice_";

exports.PLAYA_BOUNDS = function() {
	var bounds = geoViewport.bounds([-119.2066,40.7866], 14, [600, 400]);
	return [[bounds[0], bounds[1]], [bounds[2], bounds[3]]];
}

exports.MAN_LOCATION = [-119.2066,40.7866];


//wait lengths may vary depending on BLE stability and the RN component
exports.LOCATION_CHECK_INTERVAL = function () {
	if (Platform.OS == "android")
		return 8000;
	else
		return 8000;
};

exports.CONNECT_SLEEP = function () {
	if (Platform.OS == "android")
		return 0;
	else
		return 0;
};

exports.RETRIEVE_SERVICES_SLEEP = function () {
	if (Platform.OS == "android")
		return 0;
	else
		return 0;
};

exports.SET_NOTIFICATIONS_SLEEP = function () {
	if (Platform.OS == "android")
		return 1000;
	else
		return 0;
};