
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
		await sleep(3000);
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
			mediaState = await this.getRemoteJson(mediaState, "getstate");
			mediaState = BLEIDs.BLELogger(mediaState, "BLE: RefreshMediaState Complete: ", false);
			await sleep(3000);
			return mediaState;
		}
		catch (error) {
			mediaState = BLEIDs.BLELogger(mediaState, "BLE: Refresh Media Error: " + error, true);
			await sleep(10000);
			return mediaState;
		}
	}
	else {
		return mediaState;
	}
};

async function sleep(ms: number) {
await _sleep(ms);
}

function _sleep(ms: number) {
return new Promise((resolve) => setTimeout(resolve, ms));
}

exports.refreshDevices = async function (mediaState) {
	mediaState = await this.readTrack(mediaState, "Device");
	mediaState = await this.loadDevices(mediaState);
	return mediaState;
};

exports.getRemoteJson = async function (mediaState, command) {

	if (await sendCommand(mediaState, command) == false) {
		return mediaState;
	}

	return mediaState;
};


sendCommand = async function (mediaState, command) {
// Send request command
	if (mediaState.peripheral) {
		mediaState = BLEIDs.BLELogger(mediaState, "BLE: send command " + command + "on device " + mediaState.peripheral.id, false);
		try {
			const data = stringToBytes('{command:"' + command + '"};\n');
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

var service = "";
var channelCharacteristic = "";
var channelNo;
var trackNo = parseInt(idx);

if (mediaType == "Audio") {
channelNo = [mediaState.audio.channels[trackNo].channelNo];
}
else if (mediaType == "Device") {
channelNo = bin.stringToBytes(mediaState.device.devices[trackNo].deviceInfo);
}
else {
channelNo = [mediaState.video.channels[trackNo].channelNo];
}
	return mediaState;
}

exports.onUpdateVolume = async function (volume, mediaState) {
		return mediaState;
};

exports.readVolume = async function (mediaState) {
		mediaState.audio.volume = 0;
		return mediaState;
};

exports.onGTFO = async function (value, mediaState) {

	mediaState = BLEIDs.BLELogger(mediaState, "BLE: GTFO submitted value: " + value, false);
	return mediaState;
};

exports.onEnableMaster = async function (value, mediaState) {

		return mediaState;
};


exports.readLocation = async function (mediaState) {

		return mediaState;

};




