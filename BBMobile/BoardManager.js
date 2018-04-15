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
	Dimensions,
	Text,
	ScrollView,
	ListView,
} from "react-native";
import BleManager from "react-native-ble-manager";
import BLEIDs from "./BLEIDs";
import FileSystemConfig from "./FileSystemConfig";
import BLEBoardData from "./BLEBoardData";
import MediaManagement from "./MediaManagement";
import Touchable from "react-native-platform-touchable";

const window = Dimensions.get("window");
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
			showDiscoverScreen: false,
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

		var config = await FileSystemConfig.getDefaultPeripheral();
		if (config) {
			this.setState({
				boardName: config.name,
			});

			await this.startScan();
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
	}

	handleDisconnectedPeripheral(data) {

		let peripheral = data.peripheral;
		if (peripheral.name == this.state.boardName) {
			peripheral.connected = false;

			this.setState({
				selectedPeripheral: peripheral,
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

	async startScan() {
		if (!this.state.scanning) {
			this.setState({
				selectedPeripheral: BLEBoardData.emptyMediaState.peripheral,
				scanning: true,
				peripherals: new Map()
			});

			try {
				console.log("BoardManager: Scanning: ");
				await BleManager.scan([BLEIDs.bbUUID], 3, true);
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

					await BleManager.connect(peripheral.id);

					let peripherals = this.state.peripherals;
					peripheral.connected = true;
					peripherals.set(peripheral.id, peripheral);
					console.log("BoardManager: Connected to " + peripheral.id);

					// store default in filesystem.
					console.log("BoardManager: Storing Peripheral to Filesystem.");
					await FileSystemConfig.setDefaultPeripheral(peripheral);
					console.log("BoardManager: Stored");

					var boardName = peripheral.name;

					this.setState({
						selectedPeripheral: peripheral,
						showDiscoverScreen: false,
						boardName: boardName,
					});

					//	await BleManager.scan([BLEIDs.bbUUID], 3, true);
				}
				catch (error) {
					console.log("BoardManager: Connection error", error);
				}
			}
		}
	}

	async handleDiscoverPeripheral(peripheral) {
		try {
			console.log("BoardManager Found Peripheral:" + peripheral.name);

			// add to the list of peripherals for the board picker.
			var peripherals = this.state.peripherals;
			if (!peripherals.has(peripheral.id)) {
				peripherals.set(peripheral.id, peripheral);
			}

			// if it is your default peripheral, connect automatically.
			peripheral.connected = false;
			if (peripheral.name == this.state.boardName) {

				this.setState({
					selectedPeripheral: peripheral,
					peripherals: peripherals
				});
				console.log("BoardManager Discovered " + peripheral.name);
			}
		}
		catch (error) {
			console.log("BoardManager Found Peripheral Error:" + error);
		}
	}

	static navigationOptions = {
		title: "Board Management",
	};

	render() {

		console.log("Board Manager Rendering: " + this.state.showDiscoverScreen);

		const list = Array.from(this.state.peripherals.values());
		const dataSource = ds.cloneWithRows(list);


		if (!this.state.showDiscoverScreen)
			return (
				<View style={styles.container}>
					<MediaManagement peripheral={this.state.selectedPeripheral} startScan={this.startScan} />
					<Touchable
						onPress={async () => {
							try {
								await BleManager.disconnect(this.state.selectedPeripheral.id);
							}
							catch (error) {
								console.log("Pressed Search For Boards: " + error);
							}
							this.setState({
								peripherals: new Map(),
								appState: "",
								selectedPeripheral: BLEBoardData.emptyMediaState.peripheral,
								showDiscoverScreen: true,
							});

						}}
						style={{
							height: 50,
						}}
						background={Touchable.Ripple("blue")}>
						<Text style={styles.rowText}>Search for Boards</Text>
					</Touchable>
				</View>);
		else
			return (
				<View style={styles.container}>

					<Touchable
						onPress={() => this.startScan()}
						style={styles.touchableStyle}
						background={Touchable.Ripple("blue")}>
						<Text style={styles.rowText}>Scan for Burner Boards ({this.state.scanning ? "scanning" : "paused"})</Text>
					</Touchable>

					<ScrollView style={styles.scroll}>
						{(list.length == 0) &&
							<Text style={styles.rowText}>No peripherals</Text>
						}
						<ListView
							enableEmptySections={true}
							dataSource={dataSource}
							renderRow={(item) => {
								const color = item.connected ? "green" : "#fff";
								return (

									<Touchable
										onPress={async () => await this.onSelectPeripheral(item)}
										style={{ backgroundColor: color }}
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
		width: window.width
	},
	rowText: {
		margin: 5,
		fontSize: 14,
		textAlign: "center",
		padding: 10,
	},
});
