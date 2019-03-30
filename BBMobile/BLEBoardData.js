
import BleManager from "react-native-ble-manager";
import BLEIDs from "./BLEIDs";
import StateBuilder from "./StateBuilder";
import Constants from "./Constants";
import { stringToBytes } from 'convert-string';
var AsyncLock = require('async-lock');
var lock = new AsyncLock();

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
		console.log("BLE: updated boards");
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

sendCommand = function (mediaState, command, arg) {
	// Send request command
	if (mediaState.connectedPeripheral.connected == Constants.CONNECTED) {
		console.log("BLE: send command " + command + " " + arg + " on device " + mediaState.connectedPeripheral.id);
		mediaState = BLEIDs.BLELogger(mediaState, "BLE: send command " + command + "on device " + mediaState.connectedPeripheral.id, false);
		lock.acquire('send', function (done) {
			// async work
			try {
				const data = stringToBytes('{command:"' + command + '", arg:"' + arg + '"};\n');
				BleManager.write(mediaState.connectedPeripheral.id,
					BLEIDs.UARTservice,
					BLEIDs.txCharacteristic,
					data,
					18); // MTU Size
				mediaState = BLEIDs.BLELogger(mediaState, "BLE: successfully sent " + command, false);
			}
			catch (error) {
				console.log("BLE: send command " + command + " " + arg + " failed on device " + mediaState.connectedPeripheral.id);
				mediaState.connectedPeripheral.connected = false;
				mediaState = BLEIDs.BLELogger(mediaState, "BLE: getstate: " + error, true);
			}
			done();
		}, function () {
			// lock released
			console.log("BLE: send command " + command + " " + arg + " done on device " + mediaState.connectedPeripheral.id);
			return true;
		});
	}
	else {
		console.log("BLE: send command peripheral" + JSON.stringify(mediaState.connectedPeripheral.id));
		return false;
	}
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

exports.createMediaState = async function (peripheral) {
	try {
		var mediaState = StateBuilder.blankMediaState();
		mediaState.connectedPeripheral = peripheral;

		mediaState = BLEIDs.BLELogger(mediaState, "StateBuilder: Getting BLE Data for " + peripheral.name, false);
		mediaState = await this.refreshMediaState(mediaState);
		mediaState = BLEIDs.BLELogger(mediaState, "StateBuilder: Gettig Boards Data from API ", false);

		var boards = await StateBuilder.getBoards();
		mediaState.boards = boards;

		return mediaState;
	}
	catch (error) {
		console.log("StateBuilder: " + BLEIDs.fixErrorMessage(error));
	}
};


