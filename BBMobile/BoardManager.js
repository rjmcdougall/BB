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

const BleManagerModule = NativeModules.BleManager;
const bleManagerEmitter = new NativeEventEmitter(BleManagerModule);
export default class BoardManager extends Component {
	constructor() {
		super();

		this.state = {
			scanning: false,
			peripherals: new Map(),
			appState: "",
			selectedPeripheral: StateBuilder.blankMediaState().peripheral,
			mediaState: StateBuilder.blankMediaState(),
			locationState: "",
			showScreen: Constants.MEDIA_MANAGEMENT,
			discoveryState: Constants.DISCONNECTED,
			automaticallyConnect: true,
			backgroundLoop: null,
			title: "Board Management",
			boardData: [],
			boardColor: "blue",
		};

		this.handleDiscoverPeripheral = this.handleDiscoverPeripheral.bind(this);
		this.handleStopScan = this.handleStopScan.bind(this);
		this.handleDisconnectedPeripheral = this.handleDisconnectedPeripheral.bind(this);
		this.handleAppStateChange = this.handleAppStateChange.bind(this);
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

		BleManager.start({
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
		}

		// if there is a default peripheral saved, scan and attempt to load that board.
		var config = await FileSystemConfig.getDefaultPeripheral();
		if (config) {
			this.setState({
				boardName: config.name,
			});

			await this.startScan(true);
		}
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

	componentWillUnmount() {
		this.handlerDiscover.remove();
		this.handlerStop.remove();
		this.handlerDisconnect.remove();
		if (this.state.backgroundLoop)
			clearInterval(this.state.backgroundLoop);
	}

	handleDisconnectedPeripheral(data) {

		let peripheral = data.peripheral;
		if (peripheral.name == this.state.boardName) {
			peripheral.connected = false;

			if (this.state.backgroundLoop)
				clearInterval(this.state.backgroundLoop);

			this.setState({
				selectedPeripheral: peripheral,
				mediaState: StateBuilder.blankMediaState(),
				discoveryState: Constants.DISCONNECTED,
				backgroundLoop: null,
			});
			console.log("BoardManager: Disconnected from " + peripheral.name);
		}
	}

	handleStopScan() {
		console.log("BoardManager: Scan is stopped");
		this.setState({
			scanning: false
		});
	}

	async startScan(automaticallyConnect) {

		if (!this.state.scanning) {

			try {

				console.log("BoardManager: Clearing Interval: ");

				if (this.state.backgroundLoop)
					clearInterval(this.state.backgroundLoop);

				console.log("BoardManager: Clearing State: ");

				this.setState({
					selectedPeripheral: StateBuilder.blankMediaState().peripheral,
					mediaState: StateBuilder.blankMediaState(),
					scanning: true,
					discoveryState: Constants.DISCONNECTED,
					peripherals: new Map(),
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

			if (peripheral.connected) {
				try {
					BleManager.disconnect(peripheral.id);
				}
				catch (error) {
					console.log("BoardManager: Failed to Disconnect" + error);
				}
			} else {

				try {

					// store default in filesystem.
					await FileSystemConfig.setDefaultPeripheral(peripheral);

					var boardName = peripheral.name;

					await BleManager.stopScan();

					if (this.state.backgroundLoop)
						clearInterval(this.state.backgroundLoop);

					this.setState({
						selectedPeripheral: peripheral,
						mediaState: StateBuilder.blankMediaState(),
						showScreen: Constants.MEDIA_MANAGEMENT,
						boardName: boardName,
						discoveryState: Constants.DISCONNECTED,
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
	}

	async onLoadAPILocations() {
		this.setState({ mediaState: await await BBComAPIData.fetchLocations(this.state.mediaState) });
	}
	async onUpdateVolume(event) {
		this.setState({ mediaState: await BLEBoardData.onUpdateVolume(event, this.state.mediaState) });
	}
	async onSelectAudioTrack(idx) {
		this.setState({ mediaState: await BLEBoardData.setTrack(this.state.mediaState, "Audio", idx) });
	}
	async onSelectVideoTrack(idx) {
		this.setState({ mediaState: await BLEBoardData.setTrack(this.state.mediaState, "Video", idx) });
	}
	async onSelectDevice(idx) {
		this.setState({ mediaState: await BLEBoardData.setTrack(this.state.mediaState, "Device", idx) });
	}
	async onRefreshDevices() {
		this.setState({ mediaState: await BLEBoardData.refreshDevices(this.state.mediaState) });
	}

	async onPressSearchForBoards() {

		if (!this.state.scanning) {

			try {
				await BleManager.disconnect(this.state.selectedPeripheral.id);
			}
			catch (error) {
				console.log("BoardManager: Pressed Search For Boards: " + error);
			}

			if (this.state.backgroundLoop)
				clearInterval(this.state.backgroundLoop);

			this.setState({
				peripherals: new Map(),
				appState: "",
				selectedPeripheral: StateBuilder.blankMediaState().peripheral,
				mediaState: StateBuilder.blankMediaState(),
				showScreen: Constants.DISCOVER,
				discoveryState: Constants.DISCONNECTED,
				backgroundLoop: null,
			});

		}
	}

	async handleDiscoverPeripheral(peripheral) {
		try {

			// add to the list of peripherals for the board picker.
			var peripherals = this.state.peripherals;
			if (!peripherals.has(peripheral.id)) {

				console.log("BoardManager Found New Peripheral:" + peripheral.name);

				peripheral.connected = false;
				peripherals.set(peripheral.id, peripheral);

				this.setState({ peripherals: peripherals, });

				// if it is your default peripheral, connect automatically.
				if (peripheral.name == this.state.boardName) {

					this.setState({
						selectedPeripheral: peripheral,
					});

					if (this.state.automaticallyConnect) {
						console.log("BoardManager: Automatically Connecting To: " + peripheral.name);
						this.setState({ discoveryState: Constants.LOCATED });

						var mediaState = await StateBuilder.createMediaState(this.state.selectedPeripheral);

						var foundBoard = this.state.boardData.filter((board) => {
							return board.name == this.state.boardName;
						});
						var color = foundBoard[0].color;

						this.setState({
							mediaState: mediaState,
							discoveryState: Constants.CONNECTED,
							boardColor: color,
						});

						// Kick off a per-second location reader 
						await this.readLocationLoop(this.state.mediaState);
						console.log("BoardManager: Begin Background Location Loop");
					}
				}
			}
		}
		catch (error) {
			console.log("BoardManager Found Peripheral Error:" + error);
		}
	}

	async readLocationLoop() {

		var backgroundTimer = setInterval(async () => {
			console.log("Location Loop");
			if (this.state.mediaState) {
				console.log("Found Media State");
				try {
					var mediaState = await BLEBoardData.readLocation(this.state.mediaState);
					console.log("Called Location Update");
					this.setState({
						mediaState: mediaState,
					});
				}
				catch (error) {
					console.log("BoardManager Location Loop Failed:" + error);
				}
			}
		}, 8000);
		this.setState({ backgroundLoop: backgroundTimer });
	}

	render() {

		var color = "#fff";
		var enableControls = "none";
		var connectionButtonText = "";
		var boardName = "board";
		if (this.state.boardName)
			boardName = this.state.boardName;

		switch (this.state.discoveryState) {
			case Constants.DISCONNECTED:
				color = "#fff";
				enableControls = "none";
				connectionButtonText = "Connect to " + boardName;
				break;
			case Constants.LOCATED:
				color = "yellow";
				enableControls = "none";
				connectionButtonText = "Located " + boardName;
				break;
			case Constants.CONNECTED:
				if (!this.state.mediaState.isError)
					color = "green";
				else
					color = "red";
				enableControls = "auto";
				connectionButtonText = "Connected To " + boardName;
				break;
		}

		return (
			<View style={{ flex: 1 }}>
				<View style={{ flexDirection: "row" }}>
					<View style={{ margin: 5, paddingTop: 10 }}>
						<Image
							style={{ width: 45, height: 40, }}
							source={require("./images/BurnerBoardIcon-1026.png")}
						/>
					</View>
					<View style={{ flex: 1 }}>
						<BatteryController mediaState={this.state.mediaState} />
					</View>
				</View>
				<View style={{ flex: 1, flexDirection: "row" }}>
					<LeftNav onNavigate={this.onNavigate} showScreen={this.state.showScreen} onPressSearchForBoards={this.onPressSearchForBoards} />
					<View style={{ flex: 1 }}>
						<View style={{ flex: 1 }}>
							{(this.state.showScreen == Constants.MEDIA_MANAGEMENT) ? <MediaManagement pointerEvents={enableControls} mediaState={this.state.mediaState} onUpdateVolume={this.onUpdateVolume} onSelectAudioTrack={this.onSelectAudioTrack} onSelectVideoTrack={this.onSelectVideoTrack} onLoadAPILocations={this.onLoadAPILocations} /> : <View></View>}
							{(this.state.showScreen == Constants.DIAGNOSTIC) ? <Diagnostic pointerEvents={enableControls} mediaState={this.state.mediaState} /> : <View></View>}
							{(this.state.showScreen == Constants.ADMINISTRATION) ? <AdminManagement setUserPrefs={this.props.setUserPrefs} userPrefs={this.props.userPrefs} pointerEvents={enableControls} mediaState={this.state.mediaState} onSelectDevice={this.onSelectDevice} onRefreshDevices={this.onRefreshDevices} /> : <View></View>}
							{(this.state.showScreen == Constants.MAP) ? <MapController mediaState={this.state.mediaState} onLoadAPILocations={this.onLoadAPILocations} /> : <View></View>}
							{(this.state.showScreen == Constants.DISCOVER) ? <DiscoverController startScan={this.startScan} peripherals={this.state.peripherals} scanning={this.state.scanning} boardData={this.state.boardData} onSelectPeripheral={this.onSelectPeripheral} /> : <View></View>}
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