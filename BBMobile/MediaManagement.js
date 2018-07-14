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
			<View>
				<ScrollView>
					<VolumeController pointerEvents={this.props.pointerEvents} onUpdateVolume={this.onUpdateVolume} mediaState={this.props.mediaState} />
					<TrackController pointerEvents={this.props.pointerEvents} onSelectTrack={this.onSelectAudioTrack} mediaState={this.props.mediaState} mediaType="Audio" />
					<TrackController pointerEvents={this.props.pointerEvents} onSelectTrack={this.onSelectVideoTrack} mediaState={this.props.mediaState} mediaType="Video" />
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
 