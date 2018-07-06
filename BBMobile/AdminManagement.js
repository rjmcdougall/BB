import React, {
	Component
} from "react";
import {
	StyleSheet,
	View,
	ScrollView,
	Text,
} from "react-native";

import TrackController from "./TrackController";
import BLEBoardData from "./BLEBoardData";
import Touchable from "react-native-platform-touchable";
import PropTypes from "prop-types";
import StateBuilder from "./StateBuilder";

//import GoogleSignIn from "react-native-google-sign-in";

export default class AdminManagement extends Component {
	constructor(props) {
		super(props);

		this.onSelectDevice = this.props.onSelectDevice.bind(this);
		this.onRefreshDevices = this.props.onRefreshDevices.bind(this);
	}
 
	render() {

		var masterText;
		var backgroundColor;

		if (this.props.mediaState.audioMaster == 0) {
			masterText = "Enable Master";
			backgroundColor = "lightblue";
		}
		else {
			masterText = "Disable Master";
			backgroundColor = "green";
		}
		//
		return (


			<View style={styles.container}  >
				<View style={styles.contentContainer}>

					<ScrollView style={styles.scroll}>
						<TrackController onSelectTrack={this.onSelectDevice} mediaState={this.props.mediaState} mediaType="Device" refreshFunction={this.props.onRefreshDevices} />
						<View style={styles.button}>
							<Touchable
								onPress={async () => {

									await BLEBoardData.onGTFO(1, this.props.mediaState);
									return true;

								}}
								style={styles.touchableStyle}
								background={Touchable.Ripple("blue")}>
								<Text style={styles.rowText}> GTFO </Text>
							</Touchable>
						</View>
						<View style={styles.button}>
							<Touchable
								onPress={async () => {

									if (this.props.mediaState.audioMaster == 0)
										this.setState({ mediaState: await BLEBoardData.onEnableMaster(1, this.props.mediaState) });
									else
										this.setState({ mediaState: await BLEBoardData.onEnableMaster(0, this.props.mediaState) });

									return true;
								}}
								style={[styles.touchableStyle, { backgroundColor: backgroundColor }]}
								background={Touchable.Ripple("blue")}>
								<Text style={styles.rowText}> {masterText}
								</Text>
							</Touchable>
						</View>
						<View style={styles.button}>
							<Touchable
								onPress={async () => {

									return true;
 
								}}
								style={styles.touchableStyle}
								background={Touchable.Ripple("blue")}>
								<Text style={styles.rowText}>Go To BB.Com</Text>
							</Touchable>
						</View>
					</ScrollView>
				</View>

			</View>
		);
	}
}
AdminManagement.propTypes = {
	mediaState: PropTypes.object,
	locationState: PropTypes.object,
	pointerEvents: PropTypes.string,
	navigation: PropTypes.object,
	onSelectDevice: PropTypes.func,
	onRefreshDevices: PropTypes.func,
};

AdminManagement.defaultProps = {
	mediaState: StateBuilder.blankMediaState(),
};

const styles = StyleSheet.create({
	container: {
		flex: 1,
		backgroundColor: "#FFF",
	},
	contentContainer: {
		flex: 1 // pushes the footer to the end of the screen
	},
	rowText: {
		margin: 5,
		fontSize: 14,
		textAlign: "center",
		padding: 10,
	},
	touchableStyle: {
		backgroundColor: "lightblue",
		margin: 5,
		height: 50,
	},
	button: {
		height: 50,
		margin: 5,
	}
});