import React, {
	Component
} from "react";
import {
	StyleSheet,
	View,
	Dimensions,
	ScrollView,
} from "react-native";
import VolumeController from "./VolumeController";
import TrackController from "./TrackController";
import BatteryController from "./BatteryController";
import MapController from "./MapController";
import BLEBoardData from "./BLEBoardData";
const window = Dimensions.get("window");

import PropTypes from "prop-types";

export default class MediaManagement extends Component {
	constructor(props) {
		super(props);

		this.state = {
			mediaState: BLEBoardData.emptyMediaState,
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

	async componentWillReceiveProps(nextProps) {

		if (nextProps.mediaState) {
			if (this.state.mediaState.peripheral.id != nextProps.mediaState.peripheral.id) {
				this.setState({
					mediaState: nextProps.mediaState,
				});
			}
		}
		else
			console.log("MediaManagement: Null NextProps");
	}

	render() {

		return (
			<View style={styles.container} pointerEvents={this.props.pointerEvents}>
				<ScrollView style={styles.scroll}>
					<VolumeController onUpdateVolume={this.onUpdateVolume} mediaState={this.state.mediaState} />
					<BatteryController mediaState={this.state.mediaState} />
					<TrackController onSelectTrack={this.onSelectAudioTrack} mediaState={this.state.mediaState} mediaType="Audio" />
					<TrackController onSelectTrack={this.onSelectVideoTrack} mediaState={this.state.mediaState} mediaType="Video" />
					<MapController mediaState={this.state.mediaState} />
				</ScrollView>
			</View>
		);
	}
}
MediaManagement.propTypes = {
	mediaState: PropTypes.object,
	locationState: PropTypes.object,
	pointerEvents: PropTypes.string,
};

const styles = StyleSheet.create({
	container: {
		flex: 1,
		backgroundColor: "#FFF",
		width: window.width
	},
});
