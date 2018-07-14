import React, {
	Component
} from "react";
import { 
	View,
	ScrollView,
} from "react-native";
import VolumeController from "./VolumeController";
import TrackController from "./TrackController";
import BatteryController from "./BatteryController";
import MapController from "./MapController";
import StateBuilder from "./StateBuilder";

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
			<View pointerEvents={this.props.pointerEvents}>
				<ScrollView>
					<VolumeController onUpdateVolume={this.onUpdateVolume} mediaState={this.props.mediaState} />
					<BatteryController mediaState={this.props.mediaState} />
					<TrackController onSelectTrack={this.onSelectAudioTrack} mediaState={this.props.mediaState} mediaType="Audio" />
					<TrackController onSelectTrack={this.onSelectVideoTrack} mediaState={this.props.mediaState} mediaType="Video" />
					<MapController mediaState={this.props.mediaState} onLoadAPILocations={this.props.onLoadAPILocations} />
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
	onLoadAPILocations: PropTypes.func,
};

MediaManagement.defaultProps = {
	mediaState: StateBuilder.blankMediaState(),
	pointerEvents: "none",

};
 