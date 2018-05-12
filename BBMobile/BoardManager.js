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
import Touchable from "react-native-platform-touchable";

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
			selectedPeripheral: BLEBoardData.emptyMediaState.peripheral,
			mediaState: BLEBoardData.emptyMediaState,
			locationState: "",
			showDiscoverScreen: false,
			showAdminScreen: false,
			discoveryState: "Connect To Board",
			automaticallyConnect: true,
			backgroundLoop: null,
			title: "Board Management",
		};

		this.handleDiscoverPeripheral = this.handleDiscoverPeripheral.bind(this);
		this.handleStopScan = this.handleStopScan.bind(this);
		this.handleDisconnectedPeripheral = this.handleDisconnectedPeripheral.bind(this);
		this.handleAppStateChange = this.handleAppStateChange.bind(this);
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
				mediaState: BLEBoardData.emptyMediaState,
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

			if (this.state.backgroundLoop)
				clearInterval(this.state.backgroundLoop);

			this.setState({
				selectedPeripheral: BLEBoardData.emptyMediaState.peripheral,
				mediaState: BLEBoardData.emptyMediaState,
				scanning: true,
				peripherals: new Map(),
				automaticallyConnect: automaticallyConnect,
				backgroundLoop: null,
			});

			try {
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
						mediaState: BLEBoardData.emptyMediaState,
						showDiscoverScreen: false,
						showAdminScreen: false,
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

						var mediaState = await BLEBoardData.createMediaState(this.state.selectedPeripheral);

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
			if (this.state.mediaState) {

				try {
					var mediaState = await BLEBoardData.readLocation(this.state.mediaState);
					this.setState({
						mediaState: mediaState,
					});
				}
				catch (error) {
					console.log("BoardManager Location Loop Failed:" + error);
				}
			}
		}, 3000);
		this.setState({ backgroundLoop: backgroundTimer });
	}

	static navigationOptions = ({ navigation }) => {
		const { params } = navigation.state;

		return {
			title: params ? params.title : "Media Management",
		}
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
		else if (this.state.discoveryState.startsWith("Connected")) {
			color = "green";
			enableControls = "auto";
		}

		if (!this.state.showDiscoverScreen)
			return (
				<View style={styles.container}>
					<View style={styles.contentContainer}>
						{(!this.state.showAdminScreen) ? <MediaManagement pointerEvents={enableControls} mediaState={this.state.mediaState} />
							: <AdminManagement pointerEvents={enableControls} mediaState={this.state.mediaState} />
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
											selectedPeripheral: BLEBoardData.emptyMediaState.peripheral,
											mediaState: BLEBoardData.emptyMediaState,
											showDiscoverScreen: true,
											showAdminScreen: false,
											discoveryState: "Connect To Board",
											backgroundLoop: null,
										});

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

										var title;
										if (!this.state.showAdminScreen) // reversing current state
											title = "Administration";
										else
											title = "Media Management";
										this.props.navigation.setParams({ title: title });

										this.setState({
											showDiscoverScreen: false,
											showAdminScreen: !this.state.showAdminScreen,
										});

									}}
									style={{
										height: 50,
									}}
									background={Touchable.Ripple("blue")}>
									<Text style={styles.rowText}>
										{(!this.state.showAdminScreen) ? "Administration"
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
