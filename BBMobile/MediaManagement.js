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
import Touchable from "react-native-platform-touchable";
const window = Dimensions.get("window");

import PropTypes from "prop-types";

export default class MediaManagement extends Component {
	constructor(props) {
		super(props);

		this.state = {
			mediaState: BLEBoardData.emptyMediaState,
			refreshButtonClicked: false,
			connectionMessage: "Scanning For Board",
		};
	}
 
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

				console.log("MediaManagement: Load From BLE: " + JSON.stringify(this.state.mediaState.peripheral));
				var mediaState = await BLEBoardData.createMediaState(this.state.mediaState.peripheral);

				console.log("MEdiaManagement Binding Media State: " + JSON.stringify(this.state.mediaState));

				this.setState({
					refreshButtonClicked: false,
					mediaState: mediaState,
					connectionMessage: "Connected to " + this.state.mediaState.peripheral.name,
				});
			}
			else {
				console.log("Media Management: ALready Loading.");
			}
		}
		catch (error) {
			console.log("MediaManagement Error: " + error);
		}
	}

	async componentWillReceiveProps(nextProps) {

		console.log("MediaManagement: Received Props: " + JSON.stringify(nextProps.peripheral));
		var newMediaState = this.state.mediaState;
		newMediaState.peripheral = nextProps.peripheral;

		if (newMediaState.peripheral.id != "12345") {
			console.log("Received Props: " + JSON.stringify(newMediaState.peripheral));
			this.setState({
				mediaState: newMediaState,
				connectionMessage: "Located " + newMediaState.peripheral.name
			});
			await this.loadFromBLE();
		}
	}

	render() {

		var color;
		if (!this.state.connectionMessage.startsWith("Connected"))
			color = "#fff";
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
					<Text style={styles.rowText}>{this.state.connectionMessage}</Text>
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
MediaManagement.propTypes = {
	peripheral: PropTypes.object,
};

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
