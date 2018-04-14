import React, {
	Component
} from "react";
import {
	StyleSheet,
	Text,
	View,
	Dimensions,
	ScrollView,
} from "react-native";
import VolumeController from "./VolumeController";
import TrackController from "./TrackController";
import BatteryController from "./BatteryController";
import BLEBoardData from "./BLEBoardData";
import FileSystemConfig from "./FileSystemConfig";
import Touchable from "react-native-platform-touchable";
const window = Dimensions.get("window");

//import BleManager from "react-native-ble-manager";

export default class MediaManagement extends Component {
	constructor(props) {
		super(props);

		this.state = {
			peripheral: { name: null }, 
			mediaState: BLEBoardData.emptyMediaState,
			refreshButtonClicked: false,
		};
	}

	static navigationOptions = {
		title: "Media Management",
	};

	async onUpdateVolume(event) {
		this.setState({ mediaState: await BLEBoardData.onUpdateVolume(event, this.state.mediaState) });
		console.log("Media Management: Set Media State After Update.");
	}
	async onSelectAudioTrack(idx) {
		this.setState({ mediaState: await BLEBoardData.setTrack(this.state.mediaState, "Audio", idx) });
		console.log("Media Management: Set Media State After Update.");
	}
	async onSelectVideoTrack(idx) {
		this.setState({ mediaState: await BLEBoardData.setTrack(this.state.mediaState, "Video", idx) });
		console.log("Media Management: Set Media State After Update.");
	}

	async loadFromBLE() {

		try {
			if (!this.state.refreshButtonClicked) {

				this.setState({ refreshButtonClicked: true });

				var storedPeripheral = await FileSystemConfig.getDefaultPeripheral();
				console.log("MediaManagement Load From BLE: " + JSON.stringify(storedPeripheral));
				var mediaState = await BLEBoardData.createMediaState(storedPeripheral);

				this.setState({
					refreshButtonClicked: false,
					mediaState: mediaState
				});
			}
		}
		catch (error) {
			console.log("MediaManagement Error: " + error);
		}

	}
	async componentDidMount() {

		await this.loadFromBLE();
	}

	render() {

		var connectedText = "";

		if (this.state.mediaState.peripheral.connected)
			connectedText = "Connected to " + this.state.mediaState.peripheral.name;
		else
			connectedText = "Not connected to " + this.state.mediaState.peripheral.name;

		var color;
		if (this.state.mediaState.peripheral)
			color = this.state.mediaState.peripheral.connected ? "green" : "#fff";
		else
			color = "green";

		return (
			<View style={styles.container}>
				<ScrollView style={styles.scroll}>
					<VolumeController onUpdateVolume={this.onUpdateVolume} mediaState={this.state.mediaState} />
					<BatteryController mediaState={this.state.mediaState} />
					<TrackController onSelectTrack={this.onSelectAudioTrack} mediaState={this.state.mediaState} mediaType="Audio" />
					<TrackController onSelectTrack={this.onSelectVideoTrack} mediaState={this.state.mediaState} mediaType="Video" />
				</ScrollView>
				<Touchable
					onPress={async () => {
						await this.loadFromBLE();
					}
					}
					style={{
						backgroundColor: color,
						height: 50,
					}}
					background={Touchable.Ripple("blue")}>
					<Text style={styles.rowText}>{connectedText}</Text>
				</Touchable>
				<Touchable
					onPress={() => {
						this.props.navigation.navigate("Admin");
					}}
					style={{
						height: 50,
					}}
					background={Touchable.Ripple("blue")}>
					<Text style={styles.rowText}>Board Management</Text>
				</Touchable>
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
