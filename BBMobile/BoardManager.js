import React, {
	Component
} from "react";
import {
	StyleSheet,
	View,
	NativeEventEmitter,
	NativeModules,
	Platform,
	PermissionsAndroid,
	AppState,
	Text,
	ScrollView,
	ListView,
} from "react-native";
import BleManager from "react-native-ble-manager";
import BLEIDs from "./BLEIDs";
import FileSystemConfig from "./FileSystemConfig";
import BLEBoardData from "./BLEBoardData";
import MediaManagement from "./MediaManagement";
import AdminManagement from "./AdminManagement";
import Diagnostic from "./Diagnostic";
import Touchable from "react-native-platform-touchable";
import StateBuilder from "./StateBuilder";

const ds = new ListView.DataSource({
	rowHasChanged: (r1, r2) => r1 !== r2
});

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
			showScreen: "Media Management",
			discoveryState: "Connect To Board",
			automaticallyConnect: true,
			backgroundLoop: null,
			title: "Board Management",
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
	}

	async componentDidMount() {
		AppState.addEventListener("change", this.handleAppStateChange);

		BleManager.start({
			showAlert: false
		});

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
				discoveryState: "Connect To Board",
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

					this.props.navigation.setParams({ title: "Media Management" });

					this.setState({
						selectedPeripheral: peripheral,
						mediaState: StateBuilder.blankMediaState(),
						showScreen: "Media Management",
						boardName: boardName,
						discoveryState: "Connect To Board",
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

	async onUpdateVolume(event) {
		this.setState({ mediaState: await BLEBoardData.onUpdateVolume(event, this.state.mediaState) });
		console.log("Media Management: Set Media State After Update Volume.");
	}
	async onSelectAudioTrack(idx) {
		this.setState({ mediaState: await BLEBoardData.setTrack(this.state.mediaState, "Audio", idx) });
		console.log("Media Management: Set Media State After after Select Audio.");
	}
	async onSelectVideoTrack(idx) {
		this.setState({ mediaState: await BLEBoardData.setTrack(this.state.mediaState, "Video", idx) });
		console.log("Media Management: Set Media State After Select Video.");
	}
	async onSelectDevice(idx) {
		this.setState({ mediaState: await BLEBoardData.setTrack(this.state.mediaState, "Device", idx) });
		console.log("Media Management: Set Media State After Select Device.");
	}
	async onRefreshDevices() {
		this.setState({ mediaState: await BLEBoardData.refreshDevices(this.state.mediaState) });
		console.log("Media Management: Set Media State After Refresh Devices.");
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
						this.setState({ discoveryState: "Located " + this.state.selectedPeripheral.name, });

						var mediaState = await StateBuilder.createMediaState(this.state.selectedPeripheral);

						this.setState({
							mediaState: mediaState,
							discoveryState: "Connected To " + this.state.selectedPeripheral.name,
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

	static navigationOptions = ({ navigation }) => {
		const { params } = navigation.state;

		return {
			title: params ? params.title : "Media Management",
		};
	};

	render() {

		const list = Array.from(this.state.peripherals.values());
		const dataSource = ds.cloneWithRows(list);

		var color = "#fff";
		var enableControls = "none";

		if (this.state.discoveryState.startsWith("Located")) {
			color = "yellow";
			enableControls = "none";
		}
		else if (this.state.showScreen != "Discover" && this.state.discoveryState.startsWith("Connected")) {
			if (!this.state.mediaState.isError) {
				color = "green";
			}
			else {
				color = "red";
			}
			enableControls = "auto";
		}

		if (!(this.state.showScreen == "Discover"))

			return (
				<View style={styles.container}>
					<View style={styles.contentContainer}>
						{(this.state.showScreen == "Media Management") ? <MediaManagement pointerEvents={enableControls} mediaState={this.state.mediaState} onUpdateVolume={this.onUpdateVolume} onSelectAudioTrack={this.onSelectAudioTrack} onSelectVideoTrack={this.onSelectVideoTrack} />
							: (this.state.showScreen == "Diagnostic") ? <Diagnostic pointerEvents={enableControls} mediaState={this.state.mediaState} />
								: <AdminManagement pointerEvents={enableControls} mediaState={this.state.mediaState} navigation={this.props.navigation} onSelectDevice={this.onSelectDevice} onRefreshDevices={this.onRefreshDevices} />
						}

						<Touchable
							onPress={async () => {
								await this.startScan(true);
							}
							}
							style={{
								backgroundColor: color,
								height: 50,
							}}
							background={Touchable.Ripple("blue")}>
							<Text style={styles.rowText}>{this.state.discoveryState} {this.state.scanning ? "(scanning)" : ""}</Text>
						</Touchable>
						<View style={styles.footer}>
							<View style={styles.button}>
								<Touchable
									onPress={async () => {
										if (!this.state.scanning) {

											try {
												await BleManager.disconnect(this.state.selectedPeripheral.id);
											}
											catch (error) {
												console.log("BoardManager: Pressed Search For Boards: " + error);
											}

											if (this.state.backgroundLoop)
												clearInterval(this.state.backgroundLoop);

											this.props.navigation.setParams({ title: "Search For Boards" });

											this.setState({
												peripherals: new Map(),
												appState: "",
												selectedPeripheral: StateBuilder.blankMediaState().peripheral,
												mediaState: StateBuilder.blankMediaState(),
												showScreen: "Discover",
												discoveryState: "Connect To Board",
												backgroundLoop: null,
											});
										}
									}}
									style={{
										height: 50,
									}}
									background={Touchable.Ripple("blue")}>
									<Text style={styles.rowText}>Search for Boards</Text>
								</Touchable>
							</View>
							<View style={styles.button}>
								<Touchable
									onPress={async () => {

										var newScreen;
										if (this.state.showScreen == "Media Management") {
											newScreen = "Administration";
										} else if ((this.state.showScreen == "Administration")) {
											newScreen = "Diagnostic";
										} else if (this.state.showScreen == "Diagnostic") {
											newScreen = "Media Management";
										}

										this.props.navigation.setParams({ title: newScreen });

										this.setState({
											showScreen: newScreen,
										});

									}}
									style={{
										height: 50,
									}}
									background={Touchable.Ripple("blue")}>
									<Text style={styles.rowText}>
										{
											(this.state.showScreen == "Media Management") ? "Administration"
												: (this.state.showScreen == "Administration") ? "Diagnostic"
													: "Media Management"
										}
									</Text>
								</Touchable>
							</View>
						</View>
					</View>
				</View>);
		else
			return (
				<View style={styles.container}>

					<Touchable
						onPress={() => this.startScan(false)}
						style={styles.touchableStyle}
						background={Touchable.Ripple("blue")}>
						<Text style={styles.rowText}>Scan for Burner Boards ({this.state.scanning ? "scanning" : "paused"})</Text>
					</Touchable>

					<ScrollView style={styles.scroll}>
						{(list.length == 0) &&
							<Text style={styles.rowText}>No Boards Found</Text>
						}
						<ListView
							enableEmptySections={true}
							dataSource={dataSource}
							renderRow={(item) => {

								return (

									<Touchable
										onPress={async () => await this.onSelectPeripheral(item)}
										style={styles.touchableStyle}
										background={Touchable.Ripple("blue")}>
										<Text style={styles.rowText}>{item.name}</Text>
									</Touchable>

								);
							}}
						/>
					</ScrollView>
				</View>
			);
	}
}

const styles = StyleSheet.create({
	container: {
		flex: 1,
		backgroundColor: "#FFF",
	},
	contentContainer: {
		flex: 1 // pushes the footer to the end of the screen
	},
	rowText: {
		margin: 5,
		fontSize: 14,
		textAlign: "center",
		padding: 10,
	},
	touchableStyle: {
		backgroundColor: "lightblue",
		margin: 5,
	},
	footer: {
		height: 50,
		flexDirection: "row",
		justifyContent: "space-between"
	},
	button: {
		width: "50%",
		height: 50
	}
});
