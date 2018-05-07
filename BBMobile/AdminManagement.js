import React, {
	Component
} from "react";
import {
	StyleSheet,
	View,
	Dimensions,
	ScrollView,
} from "react-native";
import TrackController from "./TrackController";
import BLEBoardData from "./BLEBoardData";
const window = Dimensions.get("window");

import PropTypes from "prop-types";

export default class AdminManagement extends Component {
	constructor(props) {
		super(props);

		this.state = {
			mediaState: BLEBoardData.emptyMediaState,
		};

		this.onSelectDevice = this.onSelectDevice.bind(this);

	}

	async onSelectDevice(idx) {
		this.setState({ mediaState: await BLEBoardData.setTrack(this.state.mediaState, "Device", idx) });
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
					<TrackController onSelectTrack={this.onSelectDevice} mediaState={this.state.mediaState} mediaType="Device" />
				</ScrollView>
			</View>
		);
	}
}
AdminManagement.propTypes = {
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
