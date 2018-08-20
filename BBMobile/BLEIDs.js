
var UUIDs = new Array();

exports.bbUUID = "58fdc6ee-15d1-11e8-b642-0ed5f89f718b";
UUIDs.push({ UUID: "58fdc6ee-15d1-11e8-b642-0ed5f89f718b", name: "bbUUID" });

exports.locationService = "03c21568-159a-11e8-b642-0ed5f89f718b";
exports.locationCharacteristic = "03c2193c-159a-11e8-b642-0ed5f89f718b";
exports.locationDescriptor = "03c21a90-159a-11e8-b642-0ed5f89f718b";
UUIDs.push({ UUID: "03c21568-159a-11e8-b642-0ed5f89f718b", name: "locationService" });
UUIDs.push({ UUID: "03c2193c-159a-11e8-b642-0ed5f89f718b", name: "locationCharacteristic" });
UUIDs.push({ UUID: "03c21a90-159a-11e8-b642-0ed5f89f718b", name: "locationDescriptor" });

exports.bbConfig = "03c21db0-159a-11e8-b642-0ed5f89f718b";
UUIDs.push({ UUID: "03c21db0-159a-11e8-b642-0ed5f89f718b", name: "bbConfig" });

exports.AudioVolumeCharacteristic = "59629212-1938-11e8-accf-0ed5f89f718b";
UUIDs.push({ UUID: "59629212-1938-11e8-accf-0ed5f89f718b", name: "AudioVolumeCharacteristic" });

exports.AudioService = "89239614-1937-11e8-accf-0ed5f89f718b";
exports.AudioInfoCharacteristic = "892398a8-1937-11e8-accf-0ed5f89f718b";
exports.AudioChannelCharacteristic = "892399e8-1937-11e8-accf-0ed5f89f718b";
UUIDs.push({ UUID: "89239614-1937-11e8-accf-0ed5f89f718b", name: "AudioService" });
UUIDs.push({ UUID: "892398a8-1937-11e8-accf-0ed5f89f718b", name: "AudioInfoCharacteristic" });
UUIDs.push({ UUID: "892399e8-1937-11e8-accf-0ed5f89f718b", name: "AudioChannelCharacteristic" });

exports.VideoService = "89239614-9937-11e8-accf-0ed5f89f718b";
exports.VideoInfoCharacteristic = "892398a8-9937-11e8-accf-0ed5f89f718b";
exports.VideoChannelCharacteristic = "892399e8-9937-11e8-accf-0ed5f89f718b";
UUIDs.push({ UUID: "89239614-9937-11e8-accf-0ed5f89f718b", name: "VideoService" });
UUIDs.push({ UUID: "892398a8-9937-11e8-accf-0ed5f89f718b", name: "VideoInfoCharacteristic" });
UUIDs.push({ UUID: "892399e8-9937-11e8-accf-0ed5f89f718b", name: "VideoChannelCharacteristic" });

exports.BatteryService = "4dfc5ef6-22a9-11e8-b467-0ed5f89f718b";
exports.BatteryCharacteristic = "4dfc6194-22a9-11e8-b467-0ed5f89f718b";
UUIDs.push({ UUID: "4dfc5ef6-22a9-11e8-b467-0ed5f89f718b", name: "BatteryService" });
UUIDs.push({ UUID: "4dfc6194-22a9-11e8-b467-0ed5f89f718b", name: "BatteryCharacteristic" });

exports.BTDeviceService = "89239614-8937-11e8-accf-0ed5f89f718b";
exports.BTDeviceInfoCharacteristic = "892398a8-8937-11e8-accf-0ed5f89f718b";
exports.BTDeviceSelectCharacteristic = "892399e8-8937-11e8-accf-0ed5f89f718b";
UUIDs.push({ UUID: "89239614-8937-11e8-accf-0ed5f89f718b", name: "BTDeviceService" });
UUIDs.push({ UUID: "892398a8-8937-11e8-accf-0ed5f89f718b", name: "BTDeviceInfoCharacteristic" });
UUIDs.push({ UUID: "892399e8-8937-11e8-accf-0ed5f89f718b", name: "BTDeviceSelectCharacteristic" });

exports.AudioSyncService = "89279614-8937-11e8-accf-0ed5f89f718b";
exports.AudioSyncStatsCharacteristic = "892799e8-8937-11e8-accf-0ed5f89f718b";
exports.AudioSyncRemoteCharacteristic = "892799e8-8937-11e8-abcf-0ed5f89f718b";
UUIDs.push({ UUID: "89279614-8937-11e8-accf-0ed5f89f718b", name: "AudioSyncService" });
UUIDs.push({ UUID: "892799e8-8937-11e8-accf-0ed5f89f718b", name: "AudioSyncStatsCharacteristic" });
UUIDs.push({ UUID: "892799e8-8937-11e8-abcf-0ed5f89f718b", name: "AudioSyncRemoteCharacteristic" });

exports.AppCommandsService = "03c21568-111a-11e8-b642-0ed5f89f718b";
exports.AppCommandsGTFOCharacteristic = "03c2193c-111b-11e8-b642-0ed5f89f718b";
exports.AppCommandsAPKVersionCharacteristic = "03c2193c-111c-11e8-b642-0ed5f89f718b";
exports.AppCommandsAPKUpdateDateCharacteristic = "03c2193c-111d-11e8-b642-0ed5f89f718b";
exports.AppCommandsIPAddressCharacteristic = "03c2193c-111c-11aa-b642-0ed5f89f718b";

UUIDs.push({ UUID: "03c21568-111a-11e8-b642-0ed5f89f718b", name: "AppCommandsService" });
UUIDs.push({ UUID: "03c2193c-111b-11e8-b642-0ed5f89f718b", name: "AppCommandsGTFOCharacteristic" });
UUIDs.push({ UUID: "03c2193c-111c-11e8-b642-0ed5f89f718b", name: "AppCommandsAPKVersionCharacteristic" });
UUIDs.push({ UUID: "03c2193c-111d-11e8-b642-0ed5f89f718b", name: "AppCommandsAPKUpdateDateCharacteristic" });
UUIDs.push({ UUID: "03c2193c-111c-11aa-b642-0ed5f89f718b", name: "AppCommandsIPAddressCharacteristic" });

exports.BLELogger = function (mediaState, logText, isError) {
	logText = this.fixErrorMessage(logText, mediaState);
	mediaState.logLines.push({logLine: logText, isError: isError});
	if(!mediaState.isError){
		if(!logText.startsWith("BLE: IPAddressError:"))
			mediaState.isError = isError;
	}
	console.log(logText);
	return mediaState;
};

exports.fixErrorMessage = function (logText, mediaState) {
	for (var i = 0; i < UUIDs.length; i++) {
		logText = logText.replace("UUID " + UUIDs[i].UUID, UUIDs[i].name);
	}
	logText = logText.replace(mediaState.peripheral.id, mediaState.peripheral.name);

	return logText;
};