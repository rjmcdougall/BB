import React, { Component } from "react";
import { View, NativeEventEmitter, NativeModules, Platform, PermissionsAndroid, AppState, Text, Image, } from "react-native";
import BleManager from "react-native-ble-manager";
import BLEIDs from "./BLEIDs";
import FileSystemConfig from "./FileSystemConfig";
import BLEBoardData from "./BLEBoardData";
import MediaManagement from "./MediaManagement";
import AdminManagement from "./AdminManagement";
import Diagnostic from "./Diagnostic";
import Touchable from "react-native-platform-touchable";
import StateBuilder from "./StateBuilder";
import BBComAPIData from "./BBComAPIData";
import Constants from "./Constants";
import LeftNav from "./LeftNav";
import MapController from "./MapController";
import BatteryController from "./BatteryController";
import StyleSheet from "./StyleSheet";
import DiscoverController from "./DiscoverController";
import PropTypes from "prop-types";
import { Buffer } from 'buffer';

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
			locationState: "",
			showScreen: Constants.MEDIA_MANAGEMENT,
			automaticallyConnect: true,
			backgroundLoop: null,
			title: "Board Management",
			boardData: [],
			boardColor: "blue",
			rxBuffers: [],
		};

		this.handleDiscoverPeripheral = this.handleDiscoverPeripheral.bind(this);
		this.handleStopScan = this.handleStopScan.bind(this);
		this.handleDisconnectedPeripheral = this.handleDisconnectedPeripheral.bind(this);
		this.handleAppStateChange = this.handleAppStateChange.bind(this);
		this.handleNewData = this.handleNewData.bind(this);
		this.onEnableGTFO = this.onEnableGTFO.bind(this);
		this.onEnableMaster = this.onEnableMaster.bind(this);
		this.onUpdateVolume = this.onUpdateVolume.bind(this);
		this.onSelectAudioTrack = this.onSelectAudioTrack.bind(this);
		this.onSelectVideoTrack = this.onSelectVideoTrack.bind(this);
		this.onSelectDevice = this.onSelectDevice.bind(this);
		this.onRefreshDevices = this.onRefreshDevices.bind(this);
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

		var boards = await StateBuilder.getBoards();

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
			data = newData.value;
			characteristic = newData.characteristic;
			var tmpData = Buffer.alloc(data.length);
			var tmpDataLen = 0;
			for (var i = 0; i < data.length; i++) {
				var oneChar = data[i];
				// Do we have the end-of-json delimeter?
				if (oneChar == ";".charCodeAt(0)) {
					if (tmpData.length > 0) {
						// Push the new bytes avail
						tmpDataBuffer = Buffer.alloc(tmpDataLen);
						tmpData.copy(tmpDataBuffer, 0, 0, tmpDataLen);
						rxBuffers.push(tmpDataBuffer);
					}
					newMessage = Buffer.concat(rxBuffers);
					var newState = JSON.parse(newMessage.toString('ascii'));
					console.log(newState);
					// Setup the app-specific mediaState structure
					this.setState({ mediaState: BLEBoardData.updateMediaState(this.state.mediaState, newState) });
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
		dev = this.state.boardBleDevices.get(peripheral);
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

	onUpdateVolume = async function (volume) {
		BLEBoardData.sendCommand(this.state.mediaState, "Volume", volume);
	};
	onEnableGTFO = async function (value) {
		BLEBoardData.sendCommand(this.state.mediaState, "EnableGTFO", value);  
	};
	onRefreshDevices = async function() {
		BLEBoardData.sendCommand(this.state.mediaState, "BTScan", null);
	}
	onEnableMaster = async function (value) {
		BLEBoardData.sendCommand(this.state.mediaState, "EnableMaster", value);
	};
	onSelectAudioTrack = async function (idx) {
		BLEBoardData.sendCommand(this.state.mediaState, "Audio", idx);
	}

	async onLoadAPILocations() {
		this.setState({ mediaState: await BBComAPIData.fetchLocations(this.state.mediaState) });
	}
	async onSelectVideoTrack(idx) {
		BLEBoardData.sendCommand(this.state.mediaState, "Video", idx);

	}
	async onSelectDevice(idx) {
		BLEBoardData.sendCommand(this.state.mediaState, "Device", idx);
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
					console.log("BLE DEVICE ALREADY EXISTSED" + peripheral.id)
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
		boardBleDevice = this.state.boardBleDevices.get(peripheral.id);
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
						var mediaState = await BLEBoardData.createMediaState(boardBleDevice);

						// Kick off a per-second location reader 
						await this.readLocationLoop(this.state.mediaState);
						console.log("BoardManager: Begin Background Location Loop");
						this.setState({ mediaState: mediaState });

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
					BLEBoardData.sendCommand(this.state.mediaState, "Location", 600);
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

	render() {

		var color = "#fff";
		var enableControls = "none";
		var connectionButtonText = "";
		var boardName = "board";

		if (this.state.boardName)
			boardName = this.state.boardName;

		if (this.state.connectedPeripheral) {

			connected = this.state.connectedPeripheral.connected;

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
							{(this.state.showScreen == Constants.MEDIA_MANAGEMENT) ? <MediaManagement pointerEvents={enableControls} mediaState={this.state.mediaState} onUpdateVolume={this.onUpdateVolume} onSelectAudioTrack={this.onSelectAudioTrack} onSelectVideoTrack={this.onSelectVideoTrack} onLoadAPILocations={this.onLoadAPILocations} /> : <View></View>}
							{(this.state.showScreen == Constants.DIAGNOSTIC) ? <Diagnostic pointerEvents={enableControls} mediaState={this.state.mediaState} /> : <View></View>}
							{(this.state.showScreen == Constants.ADMINISTRATION) ? <AdminManagement onLoadAPILocations={this.onLoadAPILocations} setUserPrefs={this.props.setUserPrefs} userPrefs={this.props.userPrefs} pointerEvents={enableControls} mediaState={this.state.mediaState} onEnableMaster={this.onEnableMaster} onEnableGTFO={this.onEnableGTFO} onSelectDevice={this.onSelectDevice} onRefreshDevices={this.onRefreshDevices} /> : <View></View>}
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
