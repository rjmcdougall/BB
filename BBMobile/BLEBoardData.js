
import BleManager from "react-native-ble-manager";
import BLEIDs from "./BLEIDs";
import { bin } from "charenc";
import StateBuilder from "./StateBuilder";
import { stringToBytes } from 'convert-string';

exports.createMediaState = async function (peripheral) {
	try {
		var mediaState = StateBuilder.blankMediaState();
		mediaState.connectedPeripheral = peripheral;
		mediaState = BLEIDs.BLELogger(mediaState, "BLE: createMediaState Getting BLE Data for " + peripheral.name, false);
		return await this.refreshMediaState(mediaState);
	}
	catch (error) {
		//console.log("BLE: " + BLEIDs.fixErrorMessage(error));
		console.log("BLE: " + error);
	}
};

exports.refreshMediaState = async function (mediaState) {

	mediaState = BLEIDs.BLELogger(mediaState, "BLE: refreshMediaState Getting state ", false);
	if (mediaState.connectedPeripheral) {
		try {
			mediaState = BLEIDs.BLELogger(mediaState, "BLE: Getting state ", false);
			if (await sendCommand(mediaState, "getall", "") == false) {
				return mediaState;
			}
			mediaState = BLEIDs.BLELogger(mediaState, "BLE: RefreshMediaState Complete: ", false);
			return mediaState;
		}
		catch (error) {
			mediaState = BLEIDs.BLELogger(mediaState, "BLE: Refresh Media Error: " + error, true);
			return mediaState;
		}
	}
	else {
		return mediaState;
	}
	return mediaState;
};


// Upload the JSON from the brain to the local mediaState
exports.updateMediaState = function (mediaState, newMedia) {
	console.log("BLE: new update from brain");
	if (newMedia.boards) {
		console.log("BLE: updated boards: " + JSON.stringify(newMedia.boards));
		mediaState.boards = newMedia.boards;
	}
	if (newMedia.video) {
		console.log("BLE: updated video: " + JSON.stringify(newMedia.video));
		mediaState.video = newMedia.video;
	}
	if (newMedia.audio) {
		console.log("BLE: updated audio: " + JSON.stringify(newMedia.audio));
		mediaState.audio = newMedia.audio;
	}
	if (newMedia.state) {
		console.log("BLE: updated state: " + JSON.stringify(newMedia.state));
		mediaState.state = newMedia.state;
	}
	if (newMedia.btdevices) {
		console.log("BLE: updated btdevices: " + JSON.stringify(newMedia.btdevices));
		mediaState.devices = newMedia.btdevices;
	}
	if (newMedia.locations) {
		console.log("BLE: updated locations: " + JSON.stringify(newMedia.locations));
		mediaState.locations = newMedia.locations;
	}
	if (newMedia.battery) {
		console.log("BLE: updated battery: " + JSON.stringify(newMedia.battery));
		mediaState.battery = newMedia.battery;
	}
	return mediaState
}

sendCommand = async function (mediaState, command, arg) {
	// console.log("BLE: sendCommand: periperheral: " + JSON.stringify(mediaState.connectedPeripheral));
	// Send request command
	if (mediaState.connectedPeripheral) {
		console.log("BLE: send command " + command + " " + arg + "on device " + mediaState.connectedPeripheral.id);
		mediaState = BLEIDs.BLELogger(mediaState, "BLE: send command " + command + "on device " + mediaState.connectedPeripheral.id, false);
		try {
			const data = stringToBytes('{command:"' + command + '", arg:"' + arg + '"};\n');
			await BleManager.write(mediaState.connectedPeripheral.id,
				BLEIDs.UARTservice,
				BLEIDs.txCharacteristic,
				data, 
				18); // MTU Size
			mediaState = BLEIDs.BLELogger(mediaState, "BLE: successfully sent " + command, false);
			return true;
		}
		catch (error) {
			mediaState.connectedPeripheral.connected = false;
			mediaState = BLEIDs.BLELogger(mediaState, "BLE: getstate: " + error, true);
			return false;
		}
	}
	else
		return false;
}

exports.setTrack = async function (mediaState, mediaType, idx) {
	console.log("BLE: setTrack: " + mediaType + " " + idx);
	// Remote channel numbers are 0..n
	sendCommand(mediaState, mediaType, idx);
	return mediaState;
}

exports.refreshDevices = async function (mediaState) {
	sendCommand(mediaState, "BTScan", volume);
	return mediaState;
};

exports.onUpdateVolume = async function (volume, mediaState) {
	sendCommand(mediaState, "Volume", volume);
	return mediaState;
};

exports.readVolume = async function (mediaState) {
		return mediaState;
};

exports.onGTFO = async function (value, mediaState) {
	sendCommand(mediaState, "Gtfo", value);
	mediaState = BLEIDs.BLELogger(mediaState, "BLE: GTFO submitted value: " + value, false);
	return mediaState;
};

exports.onEnableMaster = async function (value, mediaState) {
	sendCommand(mediaState, "Master", value);
	return mediaState;
};


exports.readLocation = async function (mediaState) {
	// Get locations for last 10 mins
	sendCommand(mediaState, "Location", 600);
	return mediaState;
};




