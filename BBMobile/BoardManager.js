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
} from "react-native";
import BleManager from "react-native-ble-manager";
import BLEIDs from "./BLEIDs";
import FileSystemConfig from "./FileSystemConfig";
import BLEBoardData from "./BLEBoardData";
import MediaManagement from "./MediaManagement";

const window = Dimensions.get("window");
const BleManagerModule = NativeModules.BleManager;
const bleManagerEmitter = new NativeEventEmitter(BleManagerModule);

export default class BoardManager extends Component {
	constructor() {
		super();

		this.state = {
			scanning: false,
			appState: "",
			selectedPeripheral: BLEBoardData.emptyMediaState.peripheral,
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

		var storedPeripheral = await FileSystemConfig.getDefaultPeripheral();
		var boardName = storedPeripheral.name;
		console.log("BoardManager Load Favorite Board From File: " + boardName);
		this.setState({ boardName: boardName });

		console.log("BoardManager Start Scan: " + boardName);
		await this.startScan(); 
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

		let peripheral = data.peripheral;
		if (peripheral.name == this.state.boardName) {
			peripheral.connected = false;

			this.setState({
				selectedPeripheral: peripheral,
			});
			console.log("Disconnected from " + peripheral.name);
		}
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
				scanning: true
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

	async handleDiscoverPeripheral(peripheral) {
		try{
			console.log("BoardManager Found Peripheral:" + JSON.stringify(peripheral));
			peripheral.connected = false;
			if (peripheral.name == this.state.boardName) {
				
				this.setState({ selectedPeripheral: peripheral });
				console.log("BoardManager Discovered " + peripheral.name);
			}
		}
		catch(error){
			console.log("BoardManager Found Peripheral Error:" + error);
		}
	}

	static navigationOptions = {
		title: "Board Management",
	};

	render() {
		return (
			<View style={styles.container}>
				<MediaManagement peripheral={this.state.selectedPeripheral} />
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
});
