import { Platform } from "react-native";
import geoViewport from "@mapbox/geo-viewport";

//lots more debug info shipped to stdout
exports.debug = false;

//sime features are android specific.  the monitor is android only.'
exports.IS_ANDROID = Platform.OS === "android";
exports.IS_IOS = Platform.OS === "ios";
exports.HAS_ANDROID_VERSION = Platform.Version >= 23;

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

//Batter Thresholds
exports.BATTERY_RED = 20;
exports.BATTERY_YELLOW = 30;

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

//Map Configuration
exports.PLAYA_BOUNDS = function() {
	var bounds = geoViewport.bounds([-119.2066,40.7866], 14, [600, 400]);
	return [[bounds[0], bounds[1]], [bounds[2], bounds[3]]];
};
exports.MAN_LOCATION = [-119.2066,40.7866];

//Diagnostic Screen Configuration
exports.MAX_DIAGNOSTIC_LINES = 100;

//wait lengths may vary depending on BLE stability and the RN component
exports.LOCATION_CHECK_INTERVAL = function (isMonitorMode) {
	if (module.exports.IS_ANDROID){
		if(isMonitorMode)
			return 1000;
		else
			return 8000;
	}
	else
		return 8000;
};

exports.CONNECT_SLEEP = function () {
	if (module.exports.IS_ANDROID)
		return 0;
	else
		return 0;
};

exports.RETRIEVE_SERVICES_SLEEP = function () {
	if (module.exports.IS_ANDROID)
		return 0;
	else
		return 0;
};

exports.SET_NOTIFICATIONS_SLEEP = function () {
	if (module.exports.IS_ANDROID)
		return 1000;
	else
		return 0;
};

exports.BLE_DATA_FETCH_TIMEOUT = function () {
	if (module.exports.IS_ANDROID)
		return 5000;
	else
		return 5000;
};

exports.FS_CACHE_HEADER = "BBM_";
exports.BLE_TIMEOUT = 10000;