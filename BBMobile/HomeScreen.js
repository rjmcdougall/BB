import React, {
	Component
} from "react";
import { 
	StyleSheet,
	Text,
	View, 
	Button, 
	NativeEventEmitter,
	NativeModules,
	Platform,
	PermissionsAndroid,
	ListView,
	ScrollView,
	AppState,
	Dimensions,
} from "react-native";
import BleManager from "react-native-ble-manager";
//import BleManager from "./BLEManagerFake";
import BLEIDs from "./BLEIDs"; 
import Touchable from "react-native-platform-touchable";

const window = Dimensions.get("window");
const ds = new ListView.DataSource({
	rowHasChanged: (r1, r2) => r1 !== r2
});

const BleManagerModule = NativeModules.BleManager;
const bleManagerEmitter = new NativeEventEmitter(BleManagerModule);

export default class HomeScreen extends Component {
	constructor() {
		super();

		this.state = {
			scanning: false,
			peripherals: new Map(),
			appState: "",
			selectedPeripheral: null,
		};

		this.audioChannels = {};
		this.audioChannelSelection = { channel: "" };

		this.handleDiscoverPeripheral = this.handleDiscoverPeripheral.bind(this);
		this.handleStopScan = this.handleStopScan.bind(this);
		this.handleUpdateValueForCharacteristic = this.handleUpdateValueForCharacteristic.bind(this);
		this.handleDisconnectedPeripheral = this.handleDisconnectedPeripheral.bind(this);
		this.handleAppStateChange = this.handleAppStateChange.bind(this);

		this.BLEIDs = new BLEIDs();

	}

	componentDidMount() {
		AppState.addEventListener("change", this.handleAppStateChange);

		BleManager.start({
			showAlert: false
		});

		this.handlerDiscover = bleManagerEmitter.addListener("BleManagerDiscoverPeripheral", this.handleDiscoverPeripheral);
		this.handlerStop = bleManagerEmitter.addListener("BleManagerStopScan", this.handleStopScan);
		this.handlerDisconnect = bleManagerEmitter.addListener("BleManagerDisconnectPeripheral", this.handleDisconnectedPeripheral);
		this.handlerUpdate = bleManagerEmitter.addListener("BleManagerDidUpdateValueForCharacteristic", this.handleUpdateValueForCharacteristic);

		if (Platform.OS === "android" && Platform.Version >= 23) {
			PermissionsAndroid.check(PermissionsAndroid.PERMISSIONS.ACCESS_COARSE_LOCATION).then((result) => {
				if (result) {
					console.log("Permission is OK");
				} else {
					PermissionsAndroid.requestPermission(PermissionsAndroid.PERMISSIONS.ACCESS_COARSE_LOCATION).then((result) => {
						if (result) {
							console.log("User accept");
						} else {
							console.log("User refuse");
						}
					});
				}
			});
		}
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

	componentWillUnmount() {
		this.handlerDiscover.remove();
		this.handlerStop.remove();
		this.handlerDisconnect.remove();
		this.handlerUpdate.remove();
	}

	handleDisconnectedPeripheral(data) {
		let peripherals = this.state.peripherals;
		let peripheral = peripherals.get(data.peripheral);
		if (peripheral) {
			peripheral.connected = false;
			peripherals.set(peripheral.id, peripheral);
			this.setState({
				peripherals
			});
		}
		console.log("Disconnected from " + data.peripheral);
	}

	handleUpdateValueForCharacteristic(data) {
		console.log("Received data from " + data.peripheral + " characteristic " + data.characteristic, data.value);
	}

	handleStopScan() {
		console.log("Scan is stopped");
		this.setState({
			scanning: false
		});
	}

	async startScan() {
		if (!this.state.scanning) {
			this.setState({
				peripherals: new Map()
			});

			try {
				var results = await BleManager.scan([this.BLEIDs.bbUUID], 3, true);
				console.log("HomeScreen: Scanning: " + JSON.stringify(results));
				this.setState({
					scanning: true
				});
			}
			catch (error) {
				console.log("HomeScreen: Failed to Scan: " + error);
			}
		}
	}

	async retrieveConnected() {

		try {
			var results = await BleManager.getConnectedPeripherals([]);
			var peripherals = this.state.peripherals;
			for (var i = 0; i < results.length; i++) {
				var peripheral = results[i];
				peripheral.connected = true;
				peripherals.set(peripheral.id, peripheral);
				this.setState({
					peripherals
				});
			}
		}
		catch (error) {
			console.log("HomeScreen: Failed to Retreive Connected Peripherals." + error);
		}
	}

	handleDiscoverPeripheral(peripheral) {
		var peripherals = this.state.peripherals;
		if (!peripherals.has(peripheral.id)) {
			peripherals.set(peripheral.id, peripheral);
			this.setState({
				peripherals
			});
		}
	}
	static navigationOptions = {
		title: "Board Management",
	};

	async connectToPeripheral(peripheral) {
		if (peripheral) {
			if (peripheral.connected) {
				try {
					BleManager.disconnect(peripheral.id);
				}
				catch (error) {
					console.log("HomeScreen: Failed to Disconnect" + error);
				}
			} else {

				try {
					await BleManager.connect(peripheral.id);

					let peripherals = this.state.peripherals;
					let selectedPeripheral = peripherals.get(peripheral.id);
					if (selectedPeripheral) {
						selectedPeripheral.connected = true;
						peripherals.set(peripheral.id, selectedPeripheral);

						console.log("HomeScreen: selected peripheral:" + this.state.selectedPeripheral);
					}
					console.log("HomeScreen: Connected to " + peripheral.id);

					var peripheralInfo = await BleManager.retrieveServices(peripheral.id);

					console.log("HomeScreen: Services Retreived: " + peripheralInfo);

					this.setState({
						peripherals: peripherals,
						selectedPeripheral: selectedPeripheral,
					});

					this.props.navigation.navigate("MediaScreen", { peripheral: selectedPeripheral });

				}
				catch (error) {
					console.log("HomeScreen: Connection error", error);
				}
			}
		}
	}

	render() {
		const list = Array.from(this.state.peripherals.values());
		const dataSource = ds.cloneWithRows(list);
		const { navigate } = this.props.navigation;

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
									onPress={() => this.connectToPeripheral(item)}
									style={{ backgroundColor: color }}
									background={Touchable.Ripple("blue")}>
									<Text style={styles.rowText}>{item.name}</Text>
								</Touchable>

							);
						}}
					/>
				</ScrollView>
				{/* <MapController peripheral={this.state.selectedPeripheral} /> */}
				<Button
					title="Go to BB.com"
					onPress={() =>
						navigate("BBCom")
					}
				/>
			</View>
		);
	}

}

const styles = StyleSheet.create({
	container: {
		flex: 1,
		backgroundColor: "#FFF",
		width: window.width,
		height: window.height
	},
	scroll: {
		backgroundColor: "#f0f0f0",
		margin: 10,
	},
	touchableStyle: {
		backgroundColor: "#ccc",
		margin: 10,
	},
	rowText: {
		margin: 5,
		fontSize: 12,
		textAlign: "center",
		padding: 10,
	},
});
