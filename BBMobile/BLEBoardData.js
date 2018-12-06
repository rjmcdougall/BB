
import BleManager from "react-native-ble-manager";
import BLEIDs from "./BLEIDs";
import { bin } from "charenc";
import StateBuilder from "./StateBuilder";
import { stringToBytes } from 'convert-string';

exports.createMediaState = async function (peripheral) {
	try {
		var mediaState = StateBuilder.blankMediaState();
		mediaState.peripheral = peripheral;
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
	if (mediaState.peripheral) {
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
};


// Upload the JSON from the brain to the local mediaState
exports.updateMediaState = function (mediaState, newMedia) {
	if (newMedia.boards) {
		console.log("updated boards");
		mediaState.boards = newMedia.boards;
	}
	if (newMedia.video) {
		console.log("updated video");
		mediaState.video = newMedia.video;
	}
	if (newMedia.audio) {
		console.log("updated audio");
		mediaState.audio = newMedia.audio;
	}
	if (newMedia.state) {
		console.log("updated state");
		mediaState.state = newMedia.state;
	}
	if (newMedia.locations) {
		console.log("updated locations");
		mediaState.locations = newMedia.state;
	}
	if (newMedia.battery) {
		console.log("updated battery");
		mediaState.battery = newMedia.battery;
	}
	return mediaState
}

exports.refreshDevices = async function (mediaState) {
	mediaState = await this.readTrack(mediaState, "Device");
	mediaState = await this.loadDevices(mediaState);
	return mediaState;
};

sendCommand = async function (mediaState, command, arg) {
// Send request command
	if (!arg) {
		arg = "";
	}
	if (mediaState.peripheral) {
		mediaState = BLEIDs.BLELogger(mediaState, "BLE: send command " + command + "on device " + mediaState.peripheral.id, false);
		try {
			const data = stringToBytes('{command:"' + command + '", arg:"' + arg + '"};\n');
			await BleManager.write(mediaState.peripheral.id,
				BLEIDs.UARTservice,
				BLEIDs.txCharacteristic,
				data, 
				18); // MTU Size
			mediaState = BLEIDs.BLELogger(mediaState, "BLE: successfully sent " + command, false);
			return true;
		}
		catch (error) {
			mediaState.peripheral.connected = false;
			mediaState = BLEIDs.BLELogger(mediaState, "BLE: getstate: " + error, true);
			return false;
		}
	}
	else
		return false;
}

exports.setTrack = async function (mediaState, mediaType, idx) {
	// Remote channel numbers are 0..n
	sendCommand(mediaType, idx);
	return mediaState;
}

exports.onUpdateVolume = async function (volume, mediaState) {
	sendCommand("volume", volume);
	return mediaState;
};

exports.readVolume = async function (mediaState) {
		return mediaState;
};

exports.onGTFO = async function (value, mediaState) {
	sendCommand("gtfo", value);
	mediaState = BLEIDs.BLELogger(mediaState, "BLE: GTFO submitted value: " + value, false);
	return mediaState;
};

exports.onEnableMaster = async function (value, mediaState) {
	sendCommand("master", value);
	return mediaState;
};


exports.readLocation = async function (mediaState) {
		return mediaState;
};




