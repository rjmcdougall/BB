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
					<VolumeController pointerEvents={this.props.pointerEvents} sendCommand={this.props.sendCommand} boardState={this.props.boardState} />
					<TrackController pointerEvents={this.props.pointerEvents} sendCommand={this.props.sendCommand} audio={this.props.audio} video={this.props.video} boardState={this.props.boardState} mediaType="Audio" />
					<TrackController pointerEvents={this.props.pointerEvents} sendCommand={this.props.sendCommand} audio={this.props.audio} video={this.props.video} boardState={this.props.boardState} mediaType="Video" />
				</ScrollView>
			</View>
		);
	}
}
MediaManagement.propTypes = {
	boardState: PropTypes.object,
	pointerEvents: PropTypes.string,
	sendCommand: PropTypes.func,
	audio: PropTypes.array,
	video: PropTypes.array,
};

MediaManagement.defaultProps = {
	boardState: StateBuilder.blankBoardState(),
	pointerEvents: "none",

};
