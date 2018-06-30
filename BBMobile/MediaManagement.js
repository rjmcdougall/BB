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
import StateBuilder from "./StateBuilder";

const window = Dimensions.get("window");

import PropTypes from "prop-types";

export default class MediaManagement extends Component {
	constructor(props) {
		super(props);
 
		this.onUpdateVolume = this.props.onUpdateVolume.bind(this);
		this.onSelectAudioTrack = this.props.onSelectAudioTrack.bind(this);
		this.onSelectVideoTrack = this.props.onSelectVideoTrack.bind(this);
	}
 
	render() {

		return (
			<View style={styles.container} pointerEvents={this.props.pointerEvents}>
				<ScrollView style={styles.scroll}>
					<VolumeController onUpdateVolume={this.onUpdateVolume} mediaState={this.props.mediaState} />
					<BatteryController mediaState={this.props.mediaState} />
					<TrackController onSelectTrack={this.onSelectAudioTrack} mediaState={this.props.mediaState} mediaType="Audio" />
					<TrackController onSelectTrack={this.onSelectVideoTrack} mediaState={this.props.mediaState} mediaType="Video" />
					<MapController mediaState={this.props.mediaState} />
				</ScrollView>
			</View>
		);
	}
}
MediaManagement.propTypes = {
	mediaState: PropTypes.object,
	pointerEvents: PropTypes.string,
	onUpdateVolume: PropTypes.func,
	onSelectAudioTrack: PropTypes.func,
	onSelectVideoTrack: PropTypes.func,
};

MediaManagement.defaultProps = {
	mediaState: StateBuilder.blankMediaState(),
	pointerEvents: "none",

};

const styles = StyleSheet.create({
	container: {
		flex: 1,
		backgroundColor: "#FFF",
		width: window.width
	},
});
