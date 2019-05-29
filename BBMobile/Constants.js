import { Platform } from "react-native";
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

//wait lengths may vary depending on BLE stability and the RN component
exports.LOCATION_CHECK_INTERVAL = function () {
	if (Platform.OS == "android")
		return 15000;
	else
		return 8000;
};

exports.CONNECT_SLEEP = function () {
	if (Platform.OS == "android")
		return 1000;
	else
		return 0;
};

exports.RETRIEVE_SERVICES_SLEEP = function () {
	if (Platform.OS == "android")
		return 1000;
	else
		return 0;
};

exports.SET_NOTIFICATIONS_SLEEP = function () {
	if (Platform.OS == "android")
		return 1000;
	else
		return 0;
};