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
 
		var blockMasterBackgroundColor;
 
		if (this.props.mediaState.state.blockMaster == null) {
			blockMasterBackgroundColor = "grey";
		}
		else { 
			if (this.props.mediaState.state.blockMaster == 0) {
				blockMasterBackgroundColor = "skyblue";
			}
			else {
				blockMasterBackgroundColor = "green";
			}
		}

		//
		return (
			<View style={StyleSheet.container}>
				<ScrollView>
					<View style={{ height: 10 }}></View>
					<View style={StyleSheet.button}>
						<Touchable
							onPress={async () => {
								await this.props.sendCommand(this.props.mediaState, "EnableGTFO", !this.props.mediaState.state.GTFO);
								return true;
							}}
							style={[{ backgroundColor: (this.props.mediaState.state.GTFO) ? "green" : "skyblue"  }]}
							background={Touchable.Ripple("blue")}>
							<Text style={StyleSheet.buttonTextCenter}> GTFO </Text>
						</Touchable>
					</View>
					<View style={{ height: 10 }}></View>
					<View style={StyleSheet.button}>
						<Touchable
							onPress={async () => {
								await this.props.sendCommand(this.props.mediaState, "EnableMaster", !this.props.mediaState.state.audioMaster);
								return true;
							}}
							style={[{ backgroundColor: (this.props.mediaState.state.audioMaster) ? "green" : "skyblue"  }]}
							background={Touchable.Ripple("blue")}>
							<Text style={StyleSheet.buttonTextCenter}> Master Remote
							</Text>
						</Touchable>
					</View>
					<View style={{ height: 10 }}></View>
					<View style={StyleSheet.button}>
						<Touchable
							onPress={async () => {
								await this.props.sendCommand(this.props.mediaState, "BlockMaster", !this.props.mediaState.state.blockMaster);
								return true;
							}}
							style={[{ backgroundColor: blockMasterBackgroundColor }]}
							background={Touchable.Ripple("blue")}>
							<Text style={StyleSheet.buttonTextCenter}> Block Master Remote
							</Text>
						</Touchable>
					</View>
					<View style={{ height: 50 }}></View>
					<DeviceController onSelectTrack={async (value) => await this.props.sendCommand(this.props.mediaState, "Device", value)} mediaState={this.props.mediaState} mediaType="Device" sendCommand={this.props.sendCommand} />
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

