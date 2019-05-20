
import BleManager from "react-native-ble-manager";
import BLEIDs from "./BLEIDs";
import StateBuilder from "./StateBuilder";
import Constants from "./Constants";
import { stringToBytes } from 'convert-string';
var AsyncLock = require('async-lock');
var lock = new AsyncLock();

exports.refreshMediaState = async function (mediaState) {

	if (mediaState.connectedPeripheral) {
		try {
			mediaState = BLEIDs.BLELogger(mediaState, "BLE: requesting state ", false);
			if (await module.exports.sendCommand(mediaState, "getall", "") == false) {
				return mediaState;
			}
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
		mediaState = BLEIDs.BLELogger(mediaState, "BLE: updated boards", false);
		mediaState.boards = newMedia.boards;
	}
	if (newMedia.video) {
		console.log("BLE: updated video: " + JSON.stringify(newMedia.video));
		mediaState = BLEIDs.BLELogger(mediaState, "BLE: updated video", false);
		mediaState.video = newMedia.video;
	}
	if (newMedia.audio) {
		console.log("BLE: updated audio: " + JSON.stringify(newMedia.audio));
		mediaState = BLEIDs.BLELogger(mediaState, "BLE: updated audio", false);
		mediaState.audio = newMedia.audio;
	}
	if (newMedia.state) {
		mediaState = BLEIDs.BLELogger(mediaState, "BLE: updated state", false);
		console.log("BLE: updated state: " + JSON.stringify(newMedia.state));
		mediaState.state = newMedia.state;
	}
	if (newMedia.btdevices) {
		mediaState = BLEIDs.BLELogger(mediaState, "BLE: updated devices", false);
		console.log("BLE: updated btdevices: " + JSON.stringify(newMedia.btdevices));
		if(newMedia.btdevices.length>0)
			mediaState.devices = newMedia.btdevices;
	}
	if (newMedia.locations) {
		mediaState = BLEIDs.BLELogger(mediaState, "BLE: updated locations", false);
		console.log("BLE: updated locations: " + JSON.stringify(newMedia.locations));
		mediaState.locations = newMedia.locations;
	}
	if (newMedia.battery) {
		mediaState = BLEIDs.BLELogger(mediaState, "BLE: updated battery", false);
		console.log("BLE: updated battery: " + JSON.stringify(newMedia.battery));
		mediaState.battery = newMedia.battery;
	}
	return mediaState
}

exports.sendCommand = function (mediaState, command, arg) {
	// Send request command
	if (mediaState.connectedPeripheral.connected == Constants.CONNECTED) {
		console.log("BLE: send command " + command + " " + arg + " on device " + mediaState.connectedPeripheral.id);
		mediaState = BLEIDs.BLELogger(mediaState, "BLE: send command " + command + " on device " + mediaState.connectedPeripheral.name, false);
		lock.acquire('send', function (done) {
			// async work
			try {
				const data = stringToBytes('{command:"' + command + '", arg:"' + arg + '"};\n');
				BleManager.write(mediaState.connectedPeripheral.id,
					BLEIDs.UARTservice,
					BLEIDs.txCharacteristic,
					data,
					18); // MTU Size
				mediaState = BLEIDs.BLELogger(mediaState, "BLE: successfully requested " + command, false);
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
 




exports.createMediaState = async function (peripheral) {
	try {
		var mediaState = StateBuilder.blankMediaState();
		mediaState.connectedPeripheral = peripheral;

		mediaState = BLEIDs.BLELogger(mediaState, "BLE: Getting BLE Data for " + peripheral.name, false);
		mediaState = await this.refreshMediaState(mediaState);

		mediaState = BLEIDs.BLELogger(mediaState, "API: Gettig Boards Data", false);
		var boards = await StateBuilder.getBoards();
		mediaState.boards = boards;

		return mediaState;
	}
	catch (error) {
		console.log("StateBuilder: " + BLEIDs.fixErrorMessage(error));
	}
};


