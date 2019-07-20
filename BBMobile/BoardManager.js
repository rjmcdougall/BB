import React, { Component } from "react";
import { View, NativeEventEmitter, NativeModules, PermissionsAndroid, AppState, Text, Image, } from "react-native";
import BleManager from "react-native-ble-manager";
import Cache from "./Cache";
import MediaManagement from "./MediaManagement";
import AdminManagement from "./AdminManagement";
import AppManagement from "./AppManagement";
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
import ContentResolver from './ContentResolver';
var cr = new ContentResolver();

var AsyncLock = require("async-lock");
var lock = new AsyncLock({timeout: 5000});
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
			connectedPeripheral: StateBuilder.blankPeripheral(),
			mediaState: StateBuilder.blankMediaState(),
			showScreen: Constants.MEDIA_MANAGEMENT,
			automaticallyConnect: true,
			backgroundLoop: null,
			boardData: [],
			rxBuffers: [],
			logLines: StateBuilder.blankLogLines(),
			map: StateBuilder.blankMap(),
			boardData: StateBuilder.blankBoardData(),
			locations: StateBuilder.blankLocations(),
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
		this.setMap = this.setMap.bind(this);

	}


	setMap(map) {
		this.setState({ map: map });
	}

	async componentDidMount() {
		AppState.addEventListener("change", this.handleAppStateChange);

		await BleManager.start({
			showAlert: false
		});

		var boards = await Cache.get(Constants.BOARDS);

		if (boards) {
			this.setState({
				boardData: boards,
			});
			this.l("Loading Board Data from Cache", false, boards);
		}
		else
			this.l("No Board Data found in cache", false, null);

		this.handlerDiscover = bleManagerEmitter.addListener("BleManagerDiscoverPeripheral", this.handleDiscoverPeripheral);
		this.handlerStop = bleManagerEmitter.addListener("BleManagerStopScan", this.handleStopScan);
		this.handlerDisconnect = bleManagerEmitter.addListener("BleManagerDisconnectPeripheral", this.handleDisconnectedPeripheral);
		this.handlerNewData = bleManagerEmitter.addListener("BleManagerDidUpdateValueForCharacteristic", this.handleNewData);

		// this is a hack for android permissions. Not required for IOS.
		if (Constants.IS_ANDROID && Constants.HAS_ANDROID_VERSION) {
			PermissionsAndroid.check(PermissionsAndroid.PERMISSIONS.ACCESS_COARSE_LOCATION).then((result) => {
				if (result) {
					this.l("Permission is OK", false, null);
				} else {
					PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.ACCESS_COARSE_LOCATION).then((result) => {
						if (result) {
							this.l("User accept", false, null);
						} else {
							this.l("User refuse", true, null);
						}
					});
				}
			});

			PermissionsAndroid.check(PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION).then((result) => {
				if (result) {
					this.l("Permission is OK", false, null);
				} else {
					PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION).then((result) => {
						if (result) {
							this.l("User accept", false, null);
						} else {
							this.l("User refuse", true, null);
						}
					});
				}
			});
		}

		// if there is a default BleDevice saved, scan and attempt to load that board.
		var config = await Cache.get(Constants.DEFAULT_PERIPHERAL);
		if (config) {
			this.setState({
				boardName: config.name,
			});

			await this.startScan(true);
		}

		// Kick off a per-second location reader 
		this.l("Begin Background Location Loop", false, null);
		await this.readLocationLoop();

	}

	handleAppStateChange(nextAppState) {
		if (this.state.appState.match(/inactive|background/) && nextAppState === "active") {
			console.log("App has come to the foreground!");
			BleManager.getConnectedPeripherals([]).then((peripheralsArray) => {
				console.log("Connected boards: " + peripheralsArray.length);
			});
		}
		this.setState({
			appState: nextAppState
		});
	}

	handleNewData(newData) {
		try {
			var bm = this;
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
					try {
						var newMessage = Buffer.concat(rxBuffers);
						var newState = JSON.parse(newMessage.toString("ascii"));
						this.l("new data", false, newState);
						// Setup the app-specific mediaState structure
						this.setState({ mediaState: this.updateMediaState(this.state.mediaState, newState) });	
					}
					catch (error) {
						this.l("Bad JSON detected. Update succeeded but the app does not reflect that.  slow down pushing commads!", true, newState);
					}
					rxBuffers = [];
					this.setState({ rxBuffers: rxBuffers });
					tmpData = Buffer.alloc(1024);
					tmpDataLen = 0;
					lock.acquire("send", async function (done) {
						console.log("lock released");
						done();
					});
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
			this.l("handleNewData error: " + error, true, newMessage);
			rxBuffers = [];
			this.setState({ rxBuffers: rxBuffers });
		}
	}

	componentWillUnmount() {
		this.handlerDiscover.remove();
		this.handlerStop.remove();
		this.handlerDisconnect.remove();
		this.handlerNewData.remove(); 
	}

	async handleDisconnectedPeripheral(data) {

		let peripheral = data.peripheral;

		try {
			// Update state 
			var dev = this.state.boardBleDevices.get(peripheral);
			if (dev != null) {
				this.l("Disconnected from " + dev.name, false, dev);
				dev.connectionStatus = Constants.DISCONNECTED;
			}
			if (this.state.connectedPeripheral) {
				if (peripheral == this.state.connectedPeripheral.id) {
					this.l("Disconnected from active peripheral after " + (((new Date()) - dev.connectionStartTime) / 1000) + " seconds", true, dev);
					this.setState({
						connectedPeripheral: StateBuilder.blankPeripheral(),
						mediaState: StateBuilder.blankMediaState()
					});
				}
			}
		}
		catch (error) {
			this.l("Disconnect Error ", true, data);
		}

	}

	handleStopScan() {
		this.l("Scan is stopped", false, null);
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
 
				if (this.state.connectedPeripheral)
					if (this.state.connectedPeripheral.id != "12345") {
						await BleManager.disconnect(this.state.connectedPeripheral.id);
					}

				this.setState({
					connectedPeripheral: StateBuilder.blankPeripheral(),
					mediaState: StateBuilder.blankMediaState(),
					scanning: true,
					boardBleDevices: new Map(),
					automaticallyConnect: automaticallyConnect
				});

				this.l("Scanning with automatic connect: " + automaticallyConnect, false, null);
				await BleManager.scan([Constants.bbUUID], 5, true);
			}
			catch (error) {
				this.l("Failed to Scan: " + error, true, null);
			}
		}
	}

	onNavigate(nav) {
		this.setState({ showScreen: nav });
	}

	async onSelectPeripheral(peripheral) {
		if (peripheral) {

			var boardBLEDevices = this.state.boardBleDevices;

			if (peripheral.connectionStatus != Constants.DISCONNECTED) {
				try {
					this.l("Disconnecting BLE From " + peripheral.name, false, null);
					await BleManager.disconnect(peripheral.id);
					peripheral.connectionStatus = Constants.DISCONNECTED;
					boardBLEDevices.set(peripheral.id, peripheral);
				}
				catch (error) {
					this.l("Failed to Disconnect" + error, true, null);
				}
			}
			try {
				// store default in filesystem.
				await Cache.set(Constants.DEFAULT_PERIPHERAL, peripheral);

				var boardName = peripheral.name;

				this.setState({
					connectedPeripheral: peripheral,
					boardBLEDevices: boardBLEDevices,
					mediaState: StateBuilder.blankMediaState(),
					showScreen: Constants.MEDIA_MANAGEMENT,
					boardName: boardName,
					scanning: false,
				});

				await this.startScan(true);
			}
			catch (error) {
				this.l("Connection error" + error, true, null);
			}

		}
	}

	// Upload the JSON from the brain to the local mediaState
	updateMediaState(mediaState, newMedia) {

		try {
			if (newMedia.boards) {
				this.l(" updated boards", false, newMedia.boards);
				Cache.set(Constants.BOARDS, newMedia.boards);
				this.setState({ boardData: newMedia.boards });
			}
			if (newMedia.video) {
				this.l("updated video", false, newMedia.video);
				mediaState.video = newMedia.video;
				Cache.set(Constants.VIDEOPREFIX + this.state.connectedPeripheral.name, newMedia.video);
			}
			if (newMedia.audio) {
				this.l("updated audio", false, newMedia.audio);
				Cache.set(Constants.AUDIOPREFIX + this.state.connectedPeripheral.name, newMedia.audio);
				mediaState.audio = newMedia.audio;
			}
			if (newMedia.state) {
				this.l("updated state", false, newMedia.state);
				mediaState.state = newMedia.state;
			}
			if (newMedia.btdevices) {
				this.l("updated devices", false, newMedia.btdevices);
				Cache.set(Constants.BTDEVICESPREFIX + this.state.connectedPeripheral.name, newMedia.btdevices);
				if (newMedia.btdevices.length > 0) {
					mediaState.devices = newMedia.btdevices;
				}
			}
			if (newMedia.locations) {
				this.l("updated locations", false, newMedia.locations);
				this.setState({ locations: newMedia.locations });
			}
		}
		catch (error) {
			this.l("Error Updating Media State " + error, true, null);
		}

		return mediaState;
	}
	
	async createMediaState(peripheral) {
		try {
			var mediaState = StateBuilder.blankMediaState();

			this.l("Getting BLE Data for " + peripheral.name, false, null);
			mediaState = await this.refreshMediaState(mediaState);

			return mediaState;
		}
		catch (error) {
			this.l("Error creating media state " + error, true, null);
		}
	}

	l(logLine, isError, body) {
		if (logLine != null && isError != null) {
			var logArray = this.state.logLines;
			if (logArray.length > Constants.MAX_DIAGNOSTIC_LINES)
				logArray.splice(0, 1);
			logArray.push({ logLine: logLine, isError: isError, body: body });
			console.log(logLine);
			if (body != null) console.log(body);
			this.setState({ logLines: logArray });
		}
	}

	async refreshMediaState(mediaState) {

		if (this.state.connectedPeripheral) {
			try {
				this.l("requesting media state ", false, null);
				var audio = await Cache.get(Constants.AUDIOPREFIX + this.state.connectedPeripheral.name);
				var video = await Cache.get(Constants.VIDEOPREFIX + this.state.connectedPeripheral.name);
				var btdevices = await Cache.get(Constants.BTDEVICESPREFIX + this.state.connectedPeripheral.name);

				if (audio != null && video != null) {

					this.l("AV found in cache, skipping", false, audio.concat(video));
					mediaState.audio = audio;
					mediaState.video = video;
					mediaState.devices = btdevices;

					if (await this.sendCommand("Location", "") == false) {
						return mediaState;
					}
				}
				else {
					if (await this.sendCommand("getall", "") == false) {
						return mediaState;
					}
				}

				return mediaState;
			}
			catch (error) {

				this.l("Refresh Media Error: " + error, true);
				return mediaState;
			}
		}
		else {
			return mediaState;
		}
	}

	async sendCommand(command, arg) {
		// Send request command
		if (this.state.connectedPeripheral.connectionStatus == Constants.CONNECTED) {
			this.l("send command " + command + " on device " + this.state.connectedPeripheral.name, false);

			var bm = this;
			console.log("aquire lock");
			lock.acquire("send", async function () {
				// async work
				try {
					const data = stringToBytes("{command:\"" + command + "\", arg:\"" + arg + "\"};\n");
					await BleManager.write(bm.state.connectedPeripheral.id,
						Constants.UARTservice,
						Constants.txCharacteristic,
						data,
						18); // MTU Size
				}
				catch (error) {
					bm.state.connectedPeripheral.connectionStatus = Constants.DISCONNECTED;
					bm.l("getstate: " + error, true);
				}
			}, function () {
				return true;
			});
		}
		else {
			return false;
		}
	}
	onSelectAudioTrack = async function (idx) {
		await this.sendCommand("Audio", idx);
	}

	async onLoadAPILocations() {
		this.setState({ mediaState: await this.fetchLocations(this.state.mediaState) });
	}

	async onPressSearchForBoards() {

		if (!this.state.scanning) {

			try {
				await BleManager.disconnect(this.state.connectedPeripheral.id);

				this.setState({
					//	boardBleDevices: new Map(),
					appState: "",
					connectedPeripheral: StateBuilder.blankperipheral(),
					mediaState: StateBuilder.blankMediaState(),
					showScreen: Constants.DISCOVER
				});
			}
			catch (error) {
				this.l("Pressed Search For Boards: " + error, true, null);
			}
		}
	}
 
	async handleDiscoverPeripheral(peripheral) {
		try {

			// update the list of boardBleDevices for the board picker.
			var boardBleDevices = this.state.boardBleDevices;

			if (!boardBleDevices.has(peripheral.id)) {

				this.l("BoardManager Found New Peripheral:" + peripheral.name, false, null);

				peripheral.connectionStatus = Constants.DISCONNECTED;

				var boardBleDeviceArray = Array.from(boardBleDevices.values());
				var bleBoardDeviceExists = boardBleDeviceArray.filter((board) => {
					if (board.name == peripheral.name && board.id != peripheral)
						return true;
				});

				if (bleBoardDeviceExists.length > 0) {
					boardBleDevices.delete(bleBoardDeviceExists.id);
				}

				boardBleDevices.set(peripheral.id, peripheral);
				this.setState({ boardBleDevices: boardBleDevices });
			}

			// if it is your default peripheral, connect automatically.
			if (this.state.automaticallyConnect && peripheral.name == this.state.boardName) {
				await this.connectToPeripheral(peripheral);
			}

		}
		catch (error) {
			this.l("BoardManager Found Peripheral Error:" + error, true, null);
		}
	}

	async connectToPeripheral(peripheral) {

		// Update state 
		var boardBleDevices = this.state.boardBleDevices;
		var boardBleDevice = boardBleDevices.get(peripheral.id);

		try {

			if (boardBleDevice.connectionStatus == Constants.DISCONNECTED) {
				this.l("Automatically Connecting To: " + peripheral.name, false, null);

				// Update status 
				boardBleDevice.connectionStatus = Constants.CONNECTING;
				boardBleDevices.set(boardBleDevice.id, boardBleDevice);

				this.setState({
					connectedPeripheral: boardBleDevice,
					boardBleDevices: boardBleDevices,
				});

				try {
					await BleManager.connect(boardBleDevice.id);
					await this.sleep(Constants.CONNECT_SLEEP());
					this.l("Retreiving services", false, null);
					await BleManager.retrieveServices(boardBleDevice.id);
					await this.sleep(Constants.RETRIEVE_SERVICES_SLEEP());

					// Can't await setNotificatoon due to a bug in blemanager (missing callback)
					this.setNotificationRx(boardBleDevice.id);
					// Sleep until it's done (guess)
					await this.sleep(Constants.SET_NOTIFICATIONS_SLEEP());

					// Update status 
					boardBleDevice.connectionStatus = Constants.CONNECTED;
					boardBleDevice.connectionStartTime = new Date();
					boardBleDevices.set(boardBleDevice.id, boardBleDevice);

					// Now go setup and read all the state for the first time
					var mediaState = await this.createMediaState(boardBleDevice);
					this.setState({
						mediaState: mediaState,
						connectedPeripheral: boardBleDevice,
						boardBleDevices: boardBleDevices,
					});

				} catch (error) {
					this.l("Error connecting: " + error, true, null);

					// Update status 
					boardBleDevice.connectionStatus = Constants.DISCONNECTED;
					boardBleDevices.set(boardBleDevice.id, boardBleDevice);

					this.setState({
						connectedPeripheral: boardBleDevice,
						boardBleDevices: boardBleDevices,
					});
				}
			}
		}
		catch (error) {
			this.l(error, true, null);
		}
	}

	async setNotificationRx(peripheralId) {
		try {
			var e = await BleManager.startNotification(peripheralId,
				Constants.UARTservice,
				Constants.rxCharacteristic);
			if (e == null) {
				this.l("successfully set notificationon rx", false);
			} else {
				this.l("error " + e, true, null);
			}
		} catch (error) {
			this.l("error setting notification on rx:" + error, true, null);
		}
	}
	async readLocationLoop() {

		var backgroundTimer = setInterval(async () => {
			if (this.props.userPrefs.isMonitor) {
				try {
					var boardsJSON = JSON.parse(await cr.getLocationJSON());
					this.l("Got locations from ContentResolver", boardsJSON);
					console.log(boardsJSON);
					this.setState({ locations: boardsJSON });
				}
				catch (error) {
					this.l("Attempted to get locations via ContentResolver since we are in Monitor Mode, but failed", true, error);
				}
			}
			else {
				if (this.state.connectedPeripheral) {
					if (this.state.connectedPeripheral.connectionStatus == Constants.CONNECTED) {
						try {
							await this.sendCommand("Location", "");
						}
						catch (error) {
							this.l("Location Loop Failed:" + error, true, null);
						}
					}
					else
						this.l("Skip Location Loop. Not connected.", false, null);
				}
				else
					this.l("Skip Location Loop. Not connected.", false, null);
			}
		}, Constants.LOCATION_CHECK_INTERVAL());
		this.setState({ backgroundLoop: backgroundTimer });
	}

	render() {

		var color = "#fff";
		var enableControls = "none";
		var connectionButtonText = "";
		var boardName = "board";

		if (this.state.boardName)
			boardName = this.state.boardName;

		if (this.state.connectedPeripheral) {
 
			if (this.state.mediaState.video.localName != "loading..."
				&& this.state.mediaState.audio.localName != "loading..."
				&& this.state.mediaState.state.volume != -1) {
				color = "green";
				enableControls = "auto";
				connectionButtonText = "Loaded " + boardName;
			}
			else {
				switch (this.state.connectedPeripheral.connectionStatus) {
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


		if (!this.props.userPrefs.isMonitor)
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
							<BatteryController battery={this.state.mediaState.state.battery} />
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
								{(this.state.showScreen == Constants.APP_MANAGEMENT) ? <AppManagement onLoadAPILocations={this.onLoadAPILocations} setUserPrefs={this.props.setUserPrefs} userPrefs={this.props.userPrefs} /> : <View></View>}
								{(this.state.showScreen == Constants.MAP) ? <MapController userPrefs={this.props.userPrefs} mediaState={this.state.mediaState} locations={this.state.locations} setMap={this.setMap} map={this.state.map} boardData={this.state.boardData} setUserPrefs={this.props.setUserPrefs} /> : <View></View>}
								{(this.state.showScreen == Constants.DISCOVER) ? <DiscoverController startScan={this.startScan} boardBleDevices={this.state.boardBleDevices} scanning={this.state.scanning} boardData={this.state.boardData} onSelectPeripheral={this.onSelectPeripheral} /> : <View></View>}
							</View>
							<View style={StyleSheet.footer}>
								<Touchable
									onPress={async () => {
										try {
											await this.startScan(true);
										}
										catch (error) {
											this.l("Failed to Connext " + error);
										}
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
								
		else {
			return (
				<View style={StyleSheet.monitorContainer}>
					<View style={StyleSheet.monitorMap}>
						<MapController userPrefs={this.props.userPrefs} setUserPrefs={this.props.setUserPrefs} mediaState={this.state.mediaState} locations={this.state.locations} setMap={this.setMap} map={this.state.map} boardData={this.state.boardData} />
					</View>
					<View style={StyleSheet.batteryList}>
						{this.buildBatteryList()}
					</View>
				</View>
			);
		} 
	}

	buildBatteryList(){
		var a = new Array();
		var BM = this;
		

		var locations = new Array();
		locations.push({ board: "pegasus", b: 40 });
		locations.push({ board: "vega", b: 60 });
		locations.push({ board: "sexy", b: 80 });

		//locations.map((board) => {
		this.state.locations.map((board) => {
			var color=StateBuilder.boardColor(board.board, BM.state.boardData);
 			//color="pink";
			var batteryGauge = (
				<View key={board.board + "v2"} style={{ flexDirection: "row", backgroundColor: color }}>
					<View key={board.board + "v4"} style={{ flex: .5 }}>
						<View key={board.board + "v1"} style={{ marginVertical: 20 }}>
							<Text style={{ fontSize: 24, fontWeight: "bold" }} key={board.board + "txt"} >{board.board}</Text>
						</View>
					</View>
					<View key={board.board + "v3"} style={{ flex: 1 }}><BatteryController key={board.board + "bat"} id={board.board + "bat"} battery={board.b} /></View>
				</View>
			)
			a.push(batteryGauge);
		});

		return a;
	}
}

BoardManager.propTypes = {
	userPrefs: PropTypes.object,
	setUserPrefs: PropTypes.func,
};

BoardManager.defaultProps = {
	userPrefs: StateBuilder.blankUserPrefs(),
};
