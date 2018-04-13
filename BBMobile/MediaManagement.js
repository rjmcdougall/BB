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

export default class MediaManagement extends Component {
	constructor(props) {
		super(props);

		this.state = {
			peripheral: { name: null },//params.peripheral,
			mediaState: BLEBoardData.fakeMediaState,
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
	async componentDidMount() {
		var storedPeripheral = await FileSystemConfig.getDefaultPeripheral();
		var mediaState = await BLEBoardData.createMediaState(storedPeripheral);
		this.setState({ mediaState: mediaState });
	}

	render() {

		var connectedText = "";

		if (this.state.mediaState.peripheral.connected)
			connectedText = "Connected to " + this.state.mediaState.peripheral.name;
		else
			connectedText = "Not connected to " + this.state.mediaState.peripheral.name;

		var color = this.state.mediaState.peripheral.connected ? "green" : "#fff";

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
						if (!this.state.refreshButtonClicked) {
							try {
								this.setState({ refreshButtonClicked: true });
								this.setState({ mediaState: await BLEBoardData.refreshMediaState(this.state.mediaState) });
								this.setState({ refreshButtonClicked: false });
							}
							catch (error) {
								console.log("VolumeController Error: " + error);
							}
						}
					}
					}
					style={{
						backgroundColor: color,
						height: 50,
					}}
					background={Touchable.Ripple("blue")}>
					<Text style={styles.rowText}>{connectedText}</Text>
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
		color: "white",
	},
});
