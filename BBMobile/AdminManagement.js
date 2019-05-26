import React, { Component } from "react";
import {
	View,
	ScrollView,
	Text,
} from "react-native";

import DeviceController from "./DeviceController";
import Touchable from "react-native-platform-touchable";
import PropTypes from "prop-types";
import StyleSheet from "./StyleSheet";

export default class AdminManagement extends Component {
	constructor(props) {
		super(props);

		this.state = {
			isActive: false
		};
	}

	render() {

		var masterText;
		var backgroundColor;
		var GTFOText;
		var GTFOBackgroundColor;
		if (this.props.mediaState.state.audioMaster == 0) {
			masterText = "Enable Master";
			backgroundColor = "skyblue";
		}
		else {
			masterText = "Disable Master";
			backgroundColor = "green";
		}

		if (this.props.mediaState.state.GTFO == 0) {
			GTFOText = "Enable GTFO";
			GTFOBackgroundColor = "skyblue";
		}
		else {
			GTFOText = "Disable GTFO";
			GTFOBackgroundColor = "green";
		}

		//
		return (
			<View style={StyleSheet.container}>
				<ScrollView>
					<DeviceController onSelectTrack={(value) => this.props.sendCommand(this.props.mediaState, "Device", value)} mediaState={this.props.mediaState} mediaType="Device" sendCommand={this.props.sendCommand} />
					<View style={{ height: 10 }}></View>
					<View style={StyleSheet.button}>
						<Touchable
							onPress={async () => {
								this.props.sendCommand(this.props.mediaState, "EnableGTFO", !this.props.mediaState.state.GTFO);
								return true;
							}}
							style={[{ backgroundColor: GTFOBackgroundColor }]}
							background={Touchable.Ripple("blue")}>
							<Text style={StyleSheet.buttonTextCenter}> {GTFOText} </Text>
						</Touchable>
					</View>
					<View style={StyleSheet.button}>
						<Touchable
							onPress={async () => {
								this.props.sendCommand(this.props.mediaState,  "EnableMaster", !this.props.mediaState.state.audioMaster);
								return true;
							}}
							style={[{ backgroundColor: backgroundColor }]}
							background={Touchable.Ripple("blue")}>
							<Text style={StyleSheet.buttonTextCenter}> {masterText}
							</Text>
						</Touchable>
					</View>
					<View style={{ height: 200 }}></View>
				</ScrollView>
			</View>
		);
	}
}

AdminManagement.propTypes = {
	mediaState: PropTypes.object,
	sendCommand: PropTypes.func
};

