import React, {
	Component
} from "react";
import {
	View,
	ScrollView,
	Text,
	Linking,
} from "react-native";

import DeviceController from "./DeviceController";
import BLEBoardData from "./BLEBoardData";
import Touchable from "react-native-platform-touchable";
import PropTypes from "prop-types";
import StateBuilder from "./StateBuilder";
import StyleSheet from "./StyleSheet";

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
			backgroundColor = "skyblue";
		}
		else {
			masterText = "Disable Master";
			backgroundColor = "green";
		}
		//
		return (



			<View style={StyleSheet.container}>

				<ScrollView>
					<DeviceController onSelectTrack={this.onSelectDevice} mediaState={this.props.mediaState} mediaType="Device" refreshFunction={this.props.onRefreshDevices} />
					<View style={{ height: 10 }}></View>
					<View style={StyleSheet.button}>
						<Touchable
							onPress={async () => {

								await BLEBoardData.onGTFO(1, this.props.mediaState);
								return true;

							}}
							background={Touchable.Ripple("blue")}>
							<Text style={StyleSheet.buttonTextCenter}> GTFO </Text>
						</Touchable>
					</View>
					<View style={StyleSheet.button}>
						<Touchable
							onPress={async () => {

								if (this.props.mediaState.audioMaster == 0)
									this.setState({ mediaState: await BLEBoardData.onEnableMaster(1, this.props.mediaState) });
								else
									this.setState({ mediaState: await BLEBoardData.onEnableMaster(0, this.props.mediaState) });

								return true;
							}}
							style={[{ backgroundColor: backgroundColor }]}
							background={Touchable.Ripple("blue")}>
							<Text style={StyleSheet.buttonTextCenter}> {masterText}
							</Text>
						</Touchable>
					</View>
					<View style={StyleSheet.button}>
						<Touchable
							onPress={async () => {
								var supported = await Linking.canOpenURL("https://burnerboard.com");
								if (supported) {
									Linking.openURL("https://burnerboard.com");
								} else {
									console.log("Don't know how to open URI: " + "https://burnerboard.com");
								}
								return true;
							}}
							background={Touchable.Ripple("blue")}>
							<Text style={StyleSheet.buttonTextCenter}>Go To BB.Com</Text>
						</Touchable>
					</View>
				</ScrollView>
			</View>


		);
	}
}
AdminManagement.propTypes = {
	mediaState: PropTypes.object,
	locationState: PropTypes.object,
	pointerEvents: PropTypes.string,
	onSelectDevice: PropTypes.func,
	onRefreshDevices: PropTypes.func,
};

AdminManagement.defaultProps = {
	mediaState: StateBuilder.blankMediaState(),
};

