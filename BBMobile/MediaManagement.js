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
			pointerEvents: "none",
		};

		this.onUpdateVolume = this.onUpdateVolume.bind(this);
		this.onSelectAudioTrack = this.onSelectAudioTrack.bind(this);
		this.onSelectVideoTrack = this.onSelectVideoTrack.bind(this);
		
	}

	async onUpdateVolume(event) {
		console.log("Media Management: Set Media State After Update.");
		this.setState({ mediaState: await BLEBoardData.onUpdateVolume(event, this.state.mediaState) });
	}
	async onSelectAudioTrack(idx) {
		this.setState({ mediaState: await BLEBoardData.setTrack(this.state.mediaState, "Audio", idx) });
		console.log("Media Management: Set Media State After Update.");
	}
	async onSelectVideoTrack(idx) {
		this.setState({ mediaState: await BLEBoardData.setTrack(this.state.mediaState, "Video", idx) });
		console.log("Media Management: Set Media State After Update.");
	}
 
	async OLD_componentWillReceiveProps(nextProps) {

		console.log("Next Props: " + JSON.stringify(nextProps.mediaState));
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
	pointerEvents: PropTypes.string,
};

//MediaManagement.defaultProps = {
//	mediaState: BLEBoardData.emptyMediaState,
//	pointerEvents: "none",
//};

const styles = StyleSheet.create({
	container: {
		flex: 1,
		backgroundColor: "#FFF",
		width: window.width
	},
});
