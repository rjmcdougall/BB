
import BleManager from "react-native-ble-manager";
import BLEIDs from "./BLEIDs";

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
	battery: 87,
};

exports.emptyMediaState = {
	peripheral: {
		name: "loading...",
		id: "12345",
		connected: false,
	},
	audio: {
		channelInfo: "loading...",
		maxChannel: 1,
		volume: 0,
		channels:
			[{ channelNo: 1, channelInfo: "loading..." }]
	},
	video: {
		channelInfo: "loading...",
		maxChannel: 1,
		channels: [{ channelNo: 1, channelInfo: "loading..." }]
	},
	battery: 0,
};

exports.createMediaState = async function (peripheral) {

	try {

		console.log("BLEBoardData: " + JSON.stringify(peripheral));
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
			console.log("BLEBoardData: Connected");

			console.log("BLEBoardData: Retreiving Services.");
			var peripheralInfo = await BleManager.retrieveServices(mediaState.peripheral.id);
			console.log("BLEBoardData: Services Retreived: " + peripheralInfo);

			mediaState = await this.readTrack(mediaState, "Audio");
			mediaState = await this.readTrack(mediaState, "Video");
			mediaState = await this.refreshTrackList(mediaState, "Audio");
			mediaState = await this.refreshTrackList(mediaState, "Video");
			mediaState = await this.readVolume(mediaState);
			mediaState = await this.readBattery(mediaState);

			console.log("BLEBoardData: RefreshMediaState: " + JSON.stringify(mediaState));
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

exports.readTrack = async function (mediaState, mediaType) {

	var service = "";
	var channelCharacteristic = "";

	if (mediaType == "Audio") {
		service = BLEIDs.AudioService;
		channelCharacteristic = BLEIDs.AudioChannelCharacteristic;
	}
	else {
		service = BLEIDs.VideoService;
		channelCharacteristic = BLEIDs.VideoChannelCharacteristic;
	}
	console.log("BLEBoardData readTrack MediaType: " + mediaType);

	if (mediaState.peripheral) {
		try {

			if (!fakeBLE) {
				console.log("BLEBoardData " + mediaType + " doing Read current/track counts: " + mediaState.peripheral);
				var readData = await BleManager.read(mediaState.peripheral.id,
					service,
					channelCharacteristic);

				if (mediaType == "Audio") {
					mediaState.audio.channelNo = readData[1];
					mediaState.audio.maxChannel = readData[0];
					console.log("BLEBoardData " + mediaType + " Selected Channel No: " + mediaState.audio.channelNo);
					console.log("BLEBoardData " + mediaType + " Max Channel: " + mediaState.audio.maxChannel);
				}
				else {
					mediaState.video.channelNo = readData[1];
					mediaState.video.maxChannel = readData[0];
					console.log("BLEBoardData " + mediaType + " Selected Channel No: " + mediaState.video.channelNo);
					console.log("BLEBoardData " + mediaType + " Max Channel: " + mediaState.video.maxChannel);
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

	console.log("BLEBoardData " + mediaType + " Read Listing:" + mediaState.peripheral);
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
					if (channelNo == channelNo)
						if (mediaType == "Audio")
							mediaState.audio.channelInfo = channels[channelNo].channelInfo;
						else
							mediaState.video.channelInfo = channels[channelNo].channelInfo;

					console.log("BLEBoardData " + mediaType + " Add Info channel: " + channelNo + ", name = " + channelInfo);
				}
			}
			console.log("BLEBoardData " + mediaType + " found channels: " + JSON.stringify(channels));

			if (mediaType == "Audio")
				mediaState.audio.channels = channels;
			else
				mediaState.video.channels = channels;

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
			var newMediaState = await await this.readTrack(mediaState, mediaType);

			return newMediaState;
		}
		catch (error) {
			console.log("BLEBoardData " + mediaType + " " + error);
		}
	}
};

exports.onUpdateVolume = async function (event, mediaState) {

	console.log("BLEBoardData: submitted value: " + JSON.stringify(event.value));
	var newVolume = event.value;

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