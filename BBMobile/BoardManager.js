import React, { Component } from "react";
import { View, NativeEventEmitter, NativeModules, Platform, PermissionsAndroid, AppState, Text, Image, } from "react-native";
import BleManager from "react-native-ble-manager";
import BLEIDs from "./BLEIDs";
import FileSystemConfig from "./FileSystemConfig";
import MediaManagement from "./MediaManagement";
import AdminManagement from "./AdminManagement";
import Diagnostic from "./Diagnostic";
import Touchable from "react-native-platform-touchable";
import StateBuilder from "./StateBuilder";
import Constants from "./Constants";
import LeftNav from "./LeftNav";
import MapController from "./MapController";
import BatteryController from "./BatteryController";
import StyleSheet from "./StyleSheet";
import DiscoverController from "./DiscoverController";
import PropTypes from "prop-types";
import { Buffer } from "buffer";
var AsyncLock = require("async-lock");
var lock = new AsyncLock();
import { stringToBytes } from "convert-string";

const BleManagerModule = NativeModules.BleManager;
const bleManagerEmitter = new NativeEventEmitter(BleManagerModule);
export default class BoardManager extends Component {
	constructor() {
		super();

		this.state = {
			scanning: false,
			boardBleDevices: new Map(),
			appState: "",
			connectedPeripheral: StateBuilder.blankMediaState().peripheral,
			mediaState: StateBuilder.blankMediaState(),
			showScreen: Constants.MEDIA_MANAGEMENT,
			automaticallyConnect: true,
			backgroundLoop: null,
			boardData: [],
			rxBuffers: [],
			logLines: StateBuilder.blankLogLines(),
		};

		this.handleDiscoverPeripheral = this.handleDiscoverPeripheral.bind(this);
		this.handleStopScan = this.handleStopScan.bind(this);
		this.handleDisconnectedPeripheral = this.handleDisconnectedPeripheral.bind(this);
		this.handleAppStateChange = this.handleAppStateChange.bind(this);
		this.handleNewData = this.handleNewData.bind(this);
		this.sendCommand = this.sendCommand.bind(this);
		this.onSelectAudioTrack = this.onSelectAudioTrack.bind(this);
		this.onLoadAPILocations = this.onLoadAPILocations.bind(this);
		this.onPressSearchForBoards = this.onPressSearchForBoards.bind(this);
		this.onNavigate = this.onNavigate.bind(this);
		this.onSelectPeripheral = this.onSelectPeripheral.bind(this);
		this.startScan = this.startScan.bind(this);
	}

	async componentDidMount() {
		AppState.addEventListener("change", this.handleAppStateChange);

		await BleManager.start({
			showAlert: false
		});

		var boards = await this.getBoards();

		if (boards) {
			this.setState({
				boardData: boards,
			});
		}

		this.handlerDiscover = bleManagerEmitter.addListener("BleManagerDiscoverPeripheral", this.handleDiscoverPeripheral);
		this.handlerStop = bleManagerEmitter.addListener("BleManagerStopScan", this.handleStopScan);
		this.handlerDisconnect = bleManagerEmitter.addListener("BleManagerDisconnectPeripheral", this.handleDisconnectedPeripheral);
		this.handlerNewData = bleManagerEmitter.addListener("BleManagerDidUpdateValueForCharacteristic", this.handleNewData);

		// this is a hack for android permissions. Not required for IOS.
		if (Platform.OS === "android" && Platform.Version >= 23) {
			PermissionsAndroid.check(PermissionsAndroid.PERMISSIONS.ACCESS_COARSE_LOCATION).then((result) => {
				if (result) {
					console.log("BoardManager: Permission is OK");
				} else {
					PermissionsAndroid.requestPermission(PermissionsAndroid.PERMISSIONS.ACCESS_COARSE_LOCATION).then((result) => {
						if (result) {
							console.log("BoardManager: User accept");
						} else {
							console.log("BoardManager: User refuse");
						}
					});
				}
			});

			PermissionsAndroid.check(PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION).then((result) => {
				if (result) {
					console.log("BoardManager: Permission is OK");
				} else {
					PermissionsAndroid.requestPermission(PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION).then((result) => {
						if (result) {
							console.log("BoardManager: User accept");
						} else {
							console.log("BoardManager: User refuse");
						}
					});
				}
			});
		}

		// if there is a default BleDevice saved, scan and attempt to load that board.
		var config = await FileSystemConfig.getDefaultPeripheral();
		if (config) {
			this.setState({
				boardName: config.name,
			});

			await this.startScan(true);
		}
		this.readPhoneLocationLoop();
	}

	handleAppStateChange(nextAppState) {
		if (this.state.appState.match(/inactive|background/) && nextAppState === "active") {
			console.log("BoardManager: App has come to the foreground!");
			BleManager.getConnectedPeripherals([]).then((peripheralsArray) => {
				console.log("BoardManager: Connected boards: " + peripheralsArray.length);
			});
		}
		this.setState({
			appState: nextAppState
		});
	}

	handleNewData(newData) {
		try {
			//console.log( "BLE: newData:" + JSON.stringify(newData));
			var rxBuffers = this.state.rxBuffers;
			// Convert bytes array to string
			var data = newData.value;
			var tmpData = Buffer.alloc(data.length);
			var tmpDataLen = 0;
			for (var i = 0; i < data.length; i++) {
				var oneChar = data[i];
				// Do we have the end-of-json delimeter?
				if (oneChar == ";".charCodeAt(0)) {
					if (tmpData.length > 0) {
						// Push the new bytes avail
						var tmpDataBuffer = Buffer.alloc(tmpDataLen);
						tmpData.copy(tmpDataBuffer, 0, 0, tmpDataLen);
						rxBuffers.push(tmpDataBuffer);
					}
					var newMessage = Buffer.concat(rxBuffers);
					var newState = JSON.parse(newMessage.toString("ascii"));
					console.log(newState);
					// Setup the app-specific mediaState structure
					this.setState({ mediaState: this.updateMediaState(this.state.mediaState, newState) });
					rxBuffers = [];
					this.setState({ rxBuffers: rxBuffers });
					tmpData = Buffer.alloc(1024);
					tmpDataLen = 0;
				} else {
					// Add characters to buffer
					if (oneChar > 0) {
						tmpData[tmpDataLen] = oneChar;
						tmpDataLen++;
					}
				}
			}
			tmpDataBuffer = Buffer.alloc(tmpDataLen);
			tmpData.copy(tmpDataBuffer, 0, 0, tmpDataLen);
			if (tmpDataLen > 0) {
				if (!rxBuffers) {
					rxBuffers = [tmpDataBuffer];
				} else {
					rxBuffers.push(tmpDataBuffer);
				}
			}
		} catch (error) {
			console.log("BLE:handleNewData error: " + error);
			console.log("BLE:handleNewData message: " + newMessage);
			rxBuffers = [];
			this.setState({ rxBuffers: rxBuffers });
		}
	}

	componentWillUnmount() {
		this.handlerDiscover.remove();
		this.handlerStop.remove();
		this.handlerDisconnect.remove();
		this.handlerNewData.remove();
		if (this.state.backgroundLoop)
			clearInterval(this.state.backgroundLoop);
	}

	handleDisconnectedPeripheral(data) {

		let peripheral = data.peripheral;
		console.log("BoardManager: Disconnected from " + JSON.stringify(peripheral));
		// Update state 
		var dev = this.state.boardBleDevices.get(peripheral);
		if (dev != null) {
			console.log("BoardManager: Disconnected from " + JSON.stringify(dev));
			dev.connected = Constants.DISCONNECTED;
		}
		if (this.state.connectedPeripheral) {
			if (peripheral == this.state.connectedPeripheral.id) {
				console.log("BoardManager: our dev Disconnected from " + JSON.stringify(dev));
				if (this.state.backgroundLoop)
					clearInterval(this.state.backgroundLoop);
				this.setState({
					connectedPeripheral: StateBuilder.blankMediaState().peripheral,
					mediaState: StateBuilder.blankMediaState(),
					backgroundLoop: null,
				});
			}
		}
	}

	handleStopScan() {
		console.log("BoardManager: Scan is stopped");
		this.setState({
			scanning: false
		});
	}

	async sleep(ms) {
		await this._sleep(ms);
	}

	_sleep(ms) {
		return new Promise((resolve) => setTimeout(resolve, ms));
	}

	async startScan(automaticallyConnect) {

		if (!this.state.scanning) {
			try {
				console.log("BoardManager: Clearing Interval: ");

				if (this.state.backgroundLoop)
					clearInterval(this.state.backgroundLoop);

				console.log("BoardManager: Clearing State: ");

				if (this.state.connectedPeripheral)
					if (this.state.connectedPeripheral.id != "12345") {
						await BleManager.disconnect(this.state.connectedPeripheral.id);
						console.log("Disconnecting BLE From " + this.state.connectedPeripheral.name);
					}

				this.setState({
					connectedPeripheral: StateBuilder.blankMediaState().peripheral,
					mediaState: StateBuilder.blankMediaState(),
					scanning: true,
					boardBleDevices: new Map(),
					automaticallyConnect: automaticallyConnect,
					backgroundLoop: null,
				});

				console.log("BoardManager: Scanning with automatic connect: " + automaticallyConnect);
				await BleManager.scan([BLEIDs.bbUUID], 5, true);
			}
			catch (error) {
				console.log("BoardManager: Failed to Scan: " + error);
			}
		}
	}

	onNavigate(nav) {
		this.setState({ showScreen: nav });
	}

	async onSelectPeripheral(peripheral) {
		if (peripheral) {
			console.log("onSelectPeripheral " + JSON.stringify(peripheral));

			if (peripheral.connected == Constants.CONNECTED) {
				try {
					console.log("Disconnecting BLE From " + peripheral.name);
					await BleManager.disconnect(peripheral.id);
				}
				catch (error) {
					console.log("BoardManager: Failed to Disconnect" + error);
				}
			}
			try {
				// store default in filesystem.
				await FileSystemConfig.setDefaultPeripheral(peripheral);

				var boardName = peripheral.name;

				if (this.state.backgroundLoop)
					clearInterval(this.state.backgroundLoop);

				this.setState({
					connectedPeripheral: peripheral,
					mediaState: StateBuilder.blankMediaState(),
					showScreen: Constants.MEDIA_MANAGEMENT,
					boardName: boardName,
					scanning: false,
					backgroundLoop: null,
				});

				await this.startScan(true);
			}
			catch (error) {
				console.log("BoardManager: Connection error", error);
			}

		}
	}

	// Upload the JSON from the brain to the local mediaState
	updateMediaState(mediaState, newMedia) {
		console.log("BLE: new update from brain");
		if (newMedia.boards) {
			this.l("BLE: updated boards", false);
			mediaState.boards = newMedia.boards;
		}
		if (newMedia.video) {
			this.l("BLE: updated video", false);
			mediaState.video = newMedia.video;
		}
		if (newMedia.audio) {
			this.l("BLE: updated audio", false);
			mediaState.audio = newMedia.audio;
		}
		if (newMedia.state) {
			this.l("BLE: updated state", false);
			mediaState.state = newMedia.state;
		}
		if (newMedia.btdevices) {
			this.l("BLE: updated devices", false);
			if (newMedia.btdevices.length > 0)
				mediaState.devices = newMedia.btdevices;
		}
		if (newMedia.locations) {
			this.l("BLE: updated locations", false);
			mediaState.locations = newMedia.locations;
		}
		if (newMedia.battery) {
			this.l("BLE: updated battery", false);
			mediaState.battery = newMedia.battery;
		}
		return mediaState;
	}

	async createMediaState(peripheral) {
		try {
			var mediaState = StateBuilder.blankMediaState();
			mediaState.connectedPeripheral = peripheral;

			this.l("BLE: Getting BLE Data for " + peripheral.name, false);
			mediaState = await this.refreshMediaState(mediaState);

			this.l("API: Gettig Boards Data", false);
			var boards = await this.getBoards();
			mediaState.boards = boards;

			return mediaState;
		}
		catch (error) {
			console.log("StateBuilder: " + error);
		}
	}

	l(logLine, isError) {
		if (logLine != null && isError != null) {
			var logArray = this.state.logLines;
			logArray.push({ logLine: logLine, isError: isError });
			this.setState({ logLines: logArray });
		}
	}

	async refreshMediaState(mediaState) {

		if (mediaState.connectedPeripheral) {
			try {
				this.l("BLE: requesting state ", false);
				if (await this.sendCommand(mediaState, "getall", "") == false) {
					return mediaState;
				}
				return mediaState;
			}
			catch (error) {

				this.l("BLE: Refresh Media Error: " + error, true);
				return mediaState;
			}
		}
		else {
			return mediaState;
		}
	}

	sendCommand(mediaState, command, arg) {
		// Send request command
		if (mediaState.connectedPeripheral.connected == Constants.CONNECTED) {
			this.l("BLE: send command " + command + " on device " + mediaState.connectedPeripheral.name, false);

			var bm = this;
			lock.acquire("send", function (done) {
				// async work
				try {
					const data = stringToBytes("{command:\"" + command + "\", arg:\"" + arg + "\"};\n");
					BleManager.write(mediaState.connectedPeripheral.id,
						BLEIDs.UARTservice,
						BLEIDs.txCharacteristic,
						data,
						18); // MTU Size

					bm.l("BLE: successfully requested " + command, false);

				}
				catch (error) {
					mediaState.connectedPeripheral.connected = false;
					bm.l("BLE: getstate: " + error, true);
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
	onSelectAudioTrack = async function (idx) {
		this.sendCommand(this.state.mediaState, "Audio", idx);
	}

	async onLoadAPILocations() {
		this.setState({ mediaState: await this.fetchLocations(this.state.mediaState) });
	}

	async onPressSearchForBoards() {

		if (!this.state.scanning) {

			try {
				await BleManager.disconnect(this.state.connectedPeripheral.id);
			}
			catch (error) {
				console.log("BoardManager: Pressed Search For Boards: " + error);
			}

			if (this.state.backgroundLoop)
				clearInterval(this.state.backgroundLoop);

			this.setState({
				//	boardBleDevices: new Map(),
				appState: "",
				connectedPeripheral: StateBuilder.blankMediaState().peripheral,
				mediaState: StateBuilder.blankMediaState(),
				showScreen: Constants.DISCOVER,
				backgroundLoop: null,
			});
		}
	}

	async handleDiscoverPeripheral(peripheral) {
		try {

			// update the list of boardBleDevices for the board picker.
			var boardBleDevices = this.state.boardBleDevices;

			if (!boardBleDevices.has(peripheral.id)) {

				console.log("BoardManager Found New Peripheral:" + peripheral.name);

				peripheral.connected = Constants.DISCONNECTED;

				var boardBleDeviceArray = Array.from(boardBleDevices.values());
				var bleBoardDeviceExists = boardBleDeviceArray.filter((board) => {
					if (board.id == peripheral.id)
						return true;
				});

				if (bleBoardDeviceExists.length > 0) {
					console.log("BLE DEVICE ALREADY EXISTSED" + peripheral.id);
					boardBleDevices.delete(bleBoardDeviceExists.id);
				}

				boardBleDevices.set(peripheral.id, peripheral);
				this.setState({ boardBleDevices: boardBleDevices });
			}

			// if it is your default peripheral, connect automatically.
			if (peripheral.name == this.state.boardName) {
				await this.connectToPeripheral(peripheral);
			}
		}
		catch (error) {
			console.log("BoardManager Found Peripheral Error:" + error);
		}
	}

	async connectToPeripheral(peripheral) {

		// Update state 
		var boardBleDevices = this.state.boardBleDevices;
		var boardBleDevice = this.state.boardBleDevices.get(peripheral.id);
		try {
			this.setState({
				connectedPeripheral: boardBleDevice,
			});

			if (this.state.automaticallyConnect) {

				if (boardBleDevice.connected == Constants.DISCONNECTED) {
					console.log("BoardManager: Automatically Connecting To: " + peripheral.name);

					// Update status 
					boardBleDevice.connected = Constants.CONNECTING;
					boardBleDevices.set(boardBleDevice.id, boardBleDevice);
					console.log("BLE: Connecting to device: " + boardBleDevice.id);

					try {
						await BleManager.connect(boardBleDevice.id);
						await this.sleep(1000);
						console.log("BLE: Retreiving services");
						await BleManager.retrieveServices(boardBleDevice.id);
						await this.sleep(1000);
						console.log("BLE: Setting rx notifications ");

						// Can't await setNotificatoon due to a bug in blemanager (missing callback)
						this.setNotificationRx(boardBleDevice.id);
						// Sleep until it's done (guess)
						await this.sleep(1000);

						// Update status 
						boardBleDevice.connected = Constants.CONNECTED;
						boardBleDevices.set(boardBleDevice.id, boardBleDevice);
						console.log("BLE connectToPeripheral: Now go setup and read all the state ");

						// Now go setup and read all the state for the first time
						var mediaState = await this.createMediaState(boardBleDevice);
						this.setState({ mediaState: mediaState });

						// Kick off a per-second location reader 
						console.log("BoardManager: Begin Background Location Loop");
						await this.readLocationLoop(this.state.mediaState);

					} catch (error) {
						console.log("BLE: Error connecting: " + error);
						console.log("BLE: Error connecting: bledevice = " + JSON.stringify(boardBleDevice));
						// Update status 
						boardBleDevice.connected = Constants.DISCONNECTED;
						boardBleDevices.set(boardBleDevice.id, boardBleDevice);
					}
				}
			}
		}
		catch (error) {
			console.log(error);
		}
	}

	async setNotificationRx(peripheralId) {
		try {
			var success = await BleManager.startNotification(peripheralId,
				BLEIDs.UARTservice,
				BLEIDs.rxCharacteristic);
			if (success == null) {
				console.log("BLE:successfullysetnotificationonrx");
			} else {
				console.log("BLE:errorsettingnotificationonrx:" + success);
			}
		} catch (error) {
			console.log("BLE:errorsettingnotificationonrx:" + error);
		}
	}
	async readLocationLoop() {

		var backgroundTimer = setInterval(async () => {
			if (this.state.mediaState) {
				try {
					this.sendCommand(this.state.mediaState, "Location", 600);
				}
				catch (error) {
					console.log("BoardManager: Location Loop Failed:" + error);
				}
			}
		}, 15000);
		this.setState({ backgroundLoop: backgroundTimer });
	}

	async readPhoneLocationLoop() {

		// android play requires data for location to work.
		//	if (Platform.OS === "android") {
		var phoneBackgroundTimer = setInterval(async () => {

			if (this.state.mediaState) {
				try {
					var mediaState = await StateBuilder.getPhoneLocation(this.state.mediaState);
					this.setState({
						mediaState: mediaState,
					});
				}
				catch (error) {
					console.log("BoardManager: Phone Location Loop Failed:");
					console.log(error);
				}
			}
		}, 8000);
		this.setState({ phoneBackgroundLoop: phoneBackgroundTimer });
		//	}

	}


	async getBoards() {
		try {
			var boards = null;

			boards = await this.fetchBoards();

			console.log(boards);

			if (boards) {
				await FileSystemConfig.setBoards(boards);
			}
			else {
				boards = await this.getBoards();
			}

			if (boards)
				return boards;
			else
				return null;
		}
		catch (error) {
			console.log("StateBuilder: Error: " + error);
		}
		return boards;
	}

	async fetchBoards() {
		//const API = "http://www.fakeresponse.com/api/?sleep=5";
		const API = "https://us-central1-burner-board.cloudfunctions.net/boards";
		//const API = "https://www.burnerboard.com/boards/";
		try {
			var response = await this.advFetch(API, {
				headers: {
					"Accept": "application/json",
					"Content-Type": "application/json",
				}
			}, 2000);

			var boardsJSON = await response.json();
			var boardsText = await JSON.stringify(boardsJSON);

			if (boardsText.length > 20) // make sure it isn't empty.
				return boardsJSON;
			else
				return null;

		}
		catch (error) {
			console.log(error);
			return null;
		}
	}

	async advFetch(url, headers, timeout) {
		const TIMEOUT = timeout;
		let didTimeOut = false;

		return new Promise(function (resolve, reject) {
			const timeout = setTimeout(() => {
				didTimeOut = true;
				reject(new Error("Request timed out"));
			}, TIMEOUT);

			fetch(url, headers).then(function (response) {
				clearTimeout(timeout);
				if (!didTimeOut) {
					resolve(response);
				}
			})
				.catch(function (err) {
					if (didTimeOut) {
						return;
					}
					reject(err);
				});
		});
	}

	async fetchLocations(mediaState) {

		//const API = "http://192.168.1.66:3001/boards/locations/";
		const API = "https://us-central1-burner-board.cloudfunctions.net/boards/locations/";

		try {
			var response = await this.advFetch(API, {
				headers: {
					"Accept": "application/json",
					"Content-Type": "application/json",
				}
			}, 5000);

			var apiLocations = await response.json();

			mediaState.apiLocations = apiLocations.map((board) => {
				return {
					board: board.board,
					latitude: board.lat,
					longitude: board.lon,
					dateTime: board.time,
				};
			});

			this.l("API: Locations Fetch Found " + mediaState.apiLocations.length + " boards", false);

			return mediaState;
		}
		catch (error) {
			this.l("API: Locations: " + error, true);

			return mediaState;
		}
	}
	render() {

		var color = "#fff";
		var enableControls = "none";
		var connectionButtonText = "";
		var boardName = "board";

		if (this.state.boardName)
			boardName = this.state.boardName;

		if (this.state.connectedPeripheral) {

			var connected = this.state.connectedPeripheral.connected;

			if (!connected) {
				connected = Constants.DISCONNECTED;
			}

			if (this.state.mediaState.video.localName != "loading..."
				&& this.state.mediaState.audio.localName != "loading..."
				&& this.state.mediaState.state.volume != 0) {
				color = "green";
				enableControls = "auto";
				connectionButtonText = "Loaded " + boardName;
			}
			else {
				switch (connected) {
					case Constants.DISCONNECTED:
						color = "#fff";
						enableControls = "none";
						connectionButtonText = "Connect to " + boardName;
						break;
					case Constants.CONNECTING:
						color = "yellow";
						enableControls = "none";
						connectionButtonText = "Connecting To " + boardName;
						break;
					case Constants.CONNECTED:
						color = "yellow";
						enableControls = "none";
						connectionButtonText = "Connected To " + boardName;
						break;
				}
			}

		}
		else {
			color = "#fff";
			enableControls = "none";
			connectionButtonText = "Select Board";
		}



		return (
			<View style={{ flex: 1 }}>
				<View style={{ flexDirection: "row" }}>
					{(!this.props.userPrefs.isDevilsHand) ?
						<View style={{ margin: 5, paddingTop: 10 }}>
							<Image style={{ width: 45, height: 40, }} source={require("./images/BurnerBoardIcon-1026.png")} />
						</View>
						: <View></View>
					}
					<View style={{ flex: 1 }}>
						<BatteryController mediaState={this.state.mediaState} />
					</View>
					{(this.props.userPrefs.isDevilsHand) ?
						<View style={{ margin: 5, paddingTop: 10 }}>
							<Image style={{ width: 45, height: 40, }} source={require("./images/BurnerBoardIcon-1026.png")} />
						</View>
						: <View></View>
					}
				</View>
				<View style={{ flex: 1, flexDirection: "row" }}>
					{(!this.props.userPrefs.isDevilsHand) ? <LeftNav onNavigate={this.onNavigate} showScreen={this.state.showScreen} onPressSearchForBoards={this.onPressSearchForBoards} /> : <View></View>}
					<View style={{ flex: 1 }}>
						<View style={{ flex: 1 }}>
							{(this.state.showScreen == Constants.MEDIA_MANAGEMENT) ? <MediaManagement pointerEvents={enableControls} mediaState={this.state.mediaState} sendCommand={this.sendCommand} onLoadAPILocations={this.onLoadAPILocations} /> : <View></View>}
							{(this.state.showScreen == Constants.DIAGNOSTIC) ? <Diagnostic pointerEvents={enableControls} logLines={this.state.logLines} mediaState={this.state.mediaState} /> : <View></View>}
							{(this.state.showScreen == Constants.ADMINISTRATION) ? <AdminManagement onLoadAPILocations={this.onLoadAPILocations} setUserPrefs={this.props.setUserPrefs} userPrefs={this.props.userPrefs} pointerEvents={enableControls} mediaState={this.state.mediaState} sendCommand={this.sendCommand} /> : <View></View>}
							{(this.state.showScreen == Constants.MAP) ? <MapController userPrefs={this.props.userPrefs} mediaState={this.state.mediaState} /> : <View></View>}
							{(this.state.showScreen == Constants.DISCOVER) ? <DiscoverController startScan={this.startScan} boardBleDevices={this.state.boardBleDevices} scanning={this.state.scanning} boardData={this.state.boardData} onSelectPeripheral={this.onSelectPeripheral} /> : <View></View>}
						</View>
						<View style={StyleSheet.footer}>
							<Touchable
								onPress={async () => {

									await this.startScan(true);

								}
								}
								style={{
									backgroundColor: color,
									flex: 1,
								}}
								background={Touchable.Ripple("blue")}>
								<Text style={StyleSheet.connectButtonTextCenter}>{connectionButtonText} {this.state.scanning ? "(scanning)" : ""}</Text>
							</Touchable>
						</View>
					</View>
					{(this.props.userPrefs.isDevilsHand) ? <LeftNav onNavigate={this.onNavigate} showScreen={this.state.showScreen} onPressSearchForBoards={this.onPressSearchForBoards} /> : <View></View>}
				</View>
			</View>
		);

	}
}

BoardManager.propTypes = {
	userPrefs: PropTypes.object,
	setUserPrefs: PropTypes.func,
};

BoardManager.defaultProps = {
	userPrefs: StateBuilder.blankUserPrefs(),
};
