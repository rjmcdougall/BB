
var UUIDs = new Array();

//Emulate a serial term
//exports.bbUUID = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
//UUIDs.push({ UUID: "6e400001-b5a3-f393-e0a9-e50e24dcca9e", name: "bbUUID" });

exports.bbUUID = "58fdc6ee-15d1-11e8-b642-0ed5f89f718b";
UUIDs.push({ UUID: "58fdc6ee-15d1-11e8-b642-0ed5f89f718b", name: "bbUUID" });

exports.UARTservice = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
exports.rxCharacteristic = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
exports.txCharacteristic = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
exports.CCCD = "00002902-0000-1000-8000-00805f9b34fb";
UUIDs.push({ UUID: "6e400001-b5a3-f393-e0a9-e50e24dcca9e", name: "UARTservice" });
UUIDs.push({ UUID: "6e400002-b5a3-f393-e0a9-e50e24dcca9e", name: "rxCharacteristic" });
UUIDs.push({ UUID: "6e400003-b5a3-f393-e0a9-e50e24dcca9e", name: "txCharacteristic" });
UUIDs.push({ UUID: "00002902-0000-1000-8000-00805f9b34fb", name: "CCCD" });

exports.BLELogger = function (mediaState, logText, isError) {
	logText = this.fixErrorMessage(logText, mediaState);
	mediaState.logLines.push({ logLine: logText, isError: isError });
	if (!mediaState.isError) {
		if (!logText.startsWith("BLE: IPAddressError:"))
			mediaState.isError = isError;
	}
	console.log(logText);
	return mediaState;
};

exports.fixErrorMessage = function (logText, mediaState) {
	return logText;
	// DKW NEED TO CHECK THIS!!! LOG IS UNUSABLE
	// for (var i = 0; i < UUIDs.length; i++) {
	// 	logText = logText.replace("UUID " + UUIDs[i].UUID, UUIDs[i].name);
	// }
	// logText = logText.replace(mediaState.peripheral.id, mediaState.peripheral.name);

	// return logText;
};
