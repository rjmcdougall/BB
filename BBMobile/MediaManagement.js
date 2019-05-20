import React, {
	Component
} from "react";
import { 
	View,
	ScrollView,
} from "react-native";
import VolumeController from "./VolumeController";
import TrackController from "./TrackController";
import StateBuilder from "./StateBuilder";

import PropTypes from "prop-types";

export default class MediaManagement extends Component {
	constructor(props) {
		super(props);
	}
 
	render() {
		return (
			<View>
				<ScrollView>
					<VolumeController pointerEvents={this.props.pointerEvents} sendCommand={this.props.sendCommand} mediaState={this.props.mediaState} />
					<TrackController pointerEvents={this.props.pointerEvents} sendCommand={this.props.sendCommand} mediaState={this.props.mediaState} mediaType="Audio" />
					<TrackController pointerEvents={this.props.pointerEvents} sendCommand={this.props.sendCommand} mediaState={this.props.mediaState} mediaType="Video" />
				</ScrollView>
			</View>
		);
	}
}
MediaManagement.propTypes = {
	mediaState: PropTypes.object,
	pointerEvents: PropTypes.string,
	sendCommand: PropTypes.func,
	sendCommand: PropTypes.func,
	onLoadAPILocations: PropTypes.func,
};

MediaManagement.defaultProps = {
	mediaState: StateBuilder.blankMediaState(),
	pointerEvents: "none",

};
 