
import BleManager from "react-native-ble-manager";
import BLEIDs from "./BLEIDs";
import { bin } from "charenc";

var fakeBLE = false;

exports.fakeMediaState = {
	peripheral: {
		name: "Fake Board",
		id: "12345"
	},
	audio: {
		channelInfo: "Audio 2",
		maxChannel: 3,
		volume: 50,
		channels:
			[{ channelNo: 1, channelInfo: "Audio 1" },
			{ channelNo: 2, channelInfo: "Audio 2" },
			{ channelNo: 3, channelInfo: "Audio 3" }]
	},
	video: {
		channelInfo: "Video 2",
		maxChannel: 3,
		channels: [{ channelNo: 1, channelInfo: "Video 1" },
		{ channelNo: 2, channelInfo: "Video 2" },
		{ channelNo: 3, channelInfo: "Video 3" }]
	},
	device: {
		deviceNo: 1,
		maxDevice: 1,
		devices: [{ deviceNo: 1, deviceInfo: "loading...", deviceLabel: "loading...", isPaired: false, }]
	},
	battery: 87,
};

exports.emptyMediaState = {
	peripheral: {
		name: "loading...",
		id: "12345",
		connected: false,
	},
	audio: {
		channelNo: 1,
		maxChannel: 1,
		volume: 0,
		channels:
			[{ channelNo: 1, channelInfo: "loading..." }]
	},
	video: {
		channelNo: 1,
		maxChannel: 1,
		channels: [{ channelNo: 1, channelInfo: "loading..." }]
	},
	device: {
		deviceNo: 1,
		maxDevice: 1,
		devices: [{ deviceNo: 1, deviceInfo: "loading...", deviceLabel: "loading...", isPaired: false, }]
	},
	battery: 0,
};

exports.createMediaState = async function (peripheral) {

	try {

		console.log("BLEBoardData: " + peripheral.name);
		var mediaState = this.emptyMediaState;
		mediaState.peripheral = peripheral;
		return await this.refreshMediaState(mediaState);
	}
	catch (error) {
		console.log("BLEBoardData: " + error);
	}
};

exports.refreshMediaState = async function (mediaState) {
	if (!fakeBLE) {
		try {

			console.log("BLEBoardData: Connecting MediaState: " + mediaState.peripheral.id);
			await BleManager.connect(mediaState.peripheral.id);

			await BleManager.retrieveServices(mediaState.peripheral.id);

			mediaState = await this.readTrack(mediaState, "Audio");
			mediaState = await this.readTrack(mediaState, "Video");
			mediaState = await this.readTrack(mediaState, "Device");
			mediaState = await this.refreshTrackList(mediaState, "Audio");
			mediaState = await this.refreshTrackList(mediaState, "Video");
			mediaState = await this.loadDevices(mediaState);
			mediaState = await this.readVolume(mediaState);
			mediaState = await this.readBattery(mediaState);

			console.log("BLEBoardData: RefreshMediaState Complete: ");
			return mediaState;
		}
		catch (error) {
			console.log("BLEBoardData Refresh Media Error: " + error);
		}
	}
	else {
		console.log("BLEBoardData: FAKE Refreshing from BLE");
		var newMediaState = this.fakeMediaState;
		newMediaState.peripheral = mediaState.peripheral;
		return newMediaState;
	}
};


exports.loadDevices = async function (mediaState) {

	try {
		console.log("BLEBoardData Load Devices request scan  ");

		await BleManager.write(mediaState.peripheral.id,
			BLEIDs.BTDeviceService,
			BLEIDs.BTDeviceInfoCharacteristic,
			[1]);
	}
	catch (error) {
		console.log("BLEBoardData Load Devices " + error);
	}

	var devices = [];

	try {
		if (mediaState.peripheral) {
			for (var n = 1; n <= mediaState.device.maxDevice; n++) {

				var readData = await BleManager.read(
					mediaState.peripheral.id,
					BLEIDs.BTDeviceService,
					BLEIDs.BTDeviceInfoCharacteristic);

				var deviceInfo = "";
				if (readData.length > 3) {
					var deviceNo = readData[0];
					//var deviceMax = readData[1];
					var deviceStatus = readData[2];
					var isPaired;
					var deviceLabel;
					for (var i = 3; i < readData.length; i++) {
						deviceInfo += String.fromCharCode(readData[i]);
					}
					if (deviceStatus == 80) {
						deviceLabel = deviceInfo + " (Paired)";
						isPaired = true;
					} else {
						deviceLabel = deviceInfo;
						isPaired = false;
					}
				}
				if (deviceInfo && 0 != deviceInfo.length) {
					devices[deviceNo] = {
						deviceNo: deviceNo,
						deviceInfo: deviceInfo,
						deviceLabel: deviceLabel,
						isPaired: isPaired
					};

					console.log("BLEBoardData Load Devices: " + devices.length + " Added: ");
					mediaState.device.devices = devices;

					return mediaState;
				}
			}
			console.log("BLEBoardData Load Devices found devices: " + JSON.stringify(devices));
		}
	}
	catch (error) {
		console.log("BTController Load Devices Error: " + error);
	}
};

exports.readTrack = async function (mediaState, mediaType) {

	var service = "";
	var channelCharacteristic = "";

	if (mediaType == "Audio") {
		service = BLEIDs.AudioService;
		channelCharacteristic = BLEIDs.AudioChannelCharacteristic;
	}
	else if (mediaType == "Device") {
		service = BLEIDs.BTDeviceService;
		channelCharacteristic = BLEIDs.BTDeviceSelectCharacteristic;
	}
	else {
		service = BLEIDs.VideoService;
		channelCharacteristic = BLEIDs.VideoChannelCharacteristic;
	}

	if (mediaState.peripheral) {
		try {

			if (!fakeBLE) {
				var readData = await BleManager.read(mediaState.peripheral.id,
					service,
					channelCharacteristic);


				console.log("BLEBoardData Read Track: Selected: " + readData[1] + " Max: " + readData[0]);

				if (mediaType == "Audio") {
					mediaState.audio.channelNo = readData[1];
					mediaState.audio.maxChannel = readData[0];
				}
				else if (mediaType == "Device") {
					mediaState.device.channelNo = readData[1];
					mediaState.device.maxChannel = readData[0];
				}
				else {
					mediaState.video.channelNo = readData[1];
					mediaState.video.maxChannel = readData[0];
				}
			}
			else {
				console.log("BLEBoardData: ReadTrack Faked");
			}
			return mediaState;

		}
		catch (error) {
			console.log("BLEBoardData " + mediaType + " read track error: " + error);
		}
	}
};

exports.refreshTrackList = async function (mediaState, mediaType) {

	var service = "";
	var infoCharacteristic = "";
	var maxChannel = 0;

	if (mediaType == "Audio") {
		service = BLEIDs.AudioService;
		infoCharacteristic = BLEIDs.AudioInfoCharacteristic;
		maxChannel = mediaState.audio.maxChannel;
	}
	else {
		service = BLEIDs.VideoService;
		infoCharacteristic = BLEIDs.VideoInfoCharacteristic;
		maxChannel = mediaState.video.maxChannel;
	}

	var channels = [];

	try {
		if (mediaState.peripheral) {
			for (var n = 1; n <= maxChannel; n++) {

				var readData = await BleManager.read(mediaState.peripheral.id, service, infoCharacteristic);
				var channelNo = readData[0];
				var channelInfo = "";
				for (var i = 1; i < readData.length; i++) {
					channelInfo += String.fromCharCode(readData[i]);
				}
				if (channelInfo && 0 != channelInfo.length) {
					channels[channelNo] = { channelNo: channelNo, channelInfo: channelInfo };
				}
			}
			if (mediaType == "Audio") {
				mediaState.audio.channels = channels;
			}
			else {
				mediaState.video.channels = channels;
			}
			console.log("BLEBoardData: " + channels.length + " " + mediaType + " Added: ");

			return mediaState;
		}
	}
	catch (error) {
		console.log("BLEBoardData " + this.state.mediaType + " Error: " + error);
	}
};

exports.setTrack = async function (mediaState, mediaType, idx) {

	var service = "";
	var channelCharacteristic = "";
	var channelNo;
	var trackNo = parseInt(idx);

	if (mediaType == "Audio") {
		service = BLEIDs.AudioService;
		channelCharacteristic = BLEIDs.AudioChannelCharacteristic;
		channelNo = mediaState.audio.channels[trackNo].channelNo;
	}
	else if (mediaType == "Device") {
		service = BLEIDs.BTDeviceService;
		channelCharacteristic = BLEIDs.BTDeviceSelectCharacteristic;
		channelNo = bin.stringToBytes(this.state.devices[idx].deviceInfo)
	}
	else {
		service = BLEIDs.VideoService;
		channelCharacteristic = BLEIDs.VideoChannelCharacteristic;
		channelNo = mediaState.video.channels[trackNo].channelNo;
	}

	console.log("BLEBoardData " + mediaType + " SetTrack submitted value: " + channelNo);
	if (mediaState.peripheral) {

		try {

			if (!fakeBLE) {
				await BleManager.write(mediaState.peripheral.id,
					service,
					channelCharacteristic,
					[channelNo]);

				console.log("BLEBoardData " + mediaType + " Update: " + channelNo);
			}
			else {
				mediaState.audio.channelNo = channelNo;
			}
			var newMediaState = await this.readTrack(mediaState, mediaType);

			return newMediaState;
		}
		catch (error) {
			console.log("BLEBoardData " + mediaType + " " + error);
		}
	}
};

exports.onUpdateVolume = async function (event, mediaState) {

	var newVolume = event.value;
	console.log("BLEBoardData: submitted value: " + newVolume);

	if (mediaState.peripheral) {

		try {
			if (!fakeBLE) {
				await BleManager.write(mediaState.peripheral.id,
					BLEIDs.AudioService,
					BLEIDs.AudioVolumeCharacteristic,
					[newVolume]);
			}
			else {
				mediaState.audio.volume = newVolume;
			}
			var newMediaState = await this.readVolume(mediaState);

			return newMediaState;
		}
		catch (error) {
			console.log("BLEBoardData Update Volume Error: " + error);
		}
	}
};

exports.readVolume = async function (mediaState) {

	if (mediaState.peripheral) {
		try {
			if (!fakeBLE) {
				var readData = await BleManager.read(mediaState.peripheral.id, BLEIDs.AudioService, BLEIDs.AudioVolumeCharacteristic);
				console.log("BLEBoardData: Read Volume: " + readData[0]);
				mediaState.audio.volume = readData[0];
			}
			else {
				console.log("BLEBoardData: ReadVolume Faked");
			}
			return mediaState;
		}
		catch (error) {
			console.log("BLEBoardData Read Volume Error: " + error);
		}
	}
};

exports.readBattery = async function (mediaState) {

	if (mediaState.peripheral) {
		try {
			var readData = await BleManager.read(mediaState.peripheral.id, BLEIDs.BatteryService, BLEIDs.BatteryCharacteristic);
			console.log("BLEBoardData Read Battery: " + readData[0]);
			mediaState.battery = readData[0];
			return mediaState;
		}
		catch (error) {
			console.log("BLEBoardData Read Battery Error: " + error);
		}
	}
};