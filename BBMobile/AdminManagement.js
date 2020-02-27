import React, { Component } from "react";
import {
	View,
	ScrollView,
	Text,
} from "react-native";
import WifiController from "./WifiController";
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
 
		if (this.props.boardState.bm == null) {
			blockMasterBackgroundColor = "grey";
		}
		else { 
			if (this.props.boardState.bm == 0) {
				blockMasterBackgroundColor = "skyblue";
			}
			else {
				blockMasterBackgroundColor = "green";
			}
		}

		return (
			<View style={StyleSheet.container}>
				<ScrollView>
					<View style={{ height: 10 }}></View>
					<View style={StyleSheet.button}>
						<Touchable
							onPress={async () => {
								await this.props.sendCommand("EnableGTFO", !this.props.boardState.g);
								return true;
							}}
							style={[{ backgroundColor: (this.props.boardState.g) ? "green" : "skyblue"  }]}
							background={Touchable.Ripple("blue")}>
							<Text style={StyleSheet.buttonTextCenter}> GTFO </Text>
						</Touchable>
					</View>
					<View style={{ height: 10 }}></View>
					<View style={StyleSheet.button}>
						<Touchable
							onPress={async () => {
								await this.props.sendCommand("EnableMaster", !this.props.boardState.am);
								return true;
							}}
							style={[{ backgroundColor: (this.props.boardState.am) ? "green" : "skyblue"  }]}
							background={Touchable.Ripple("blue")}>
							<Text style={StyleSheet.buttonTextCenter}> Master Remote
							</Text>
						</Touchable>
					</View>
					<View style={{ height: 10 }}></View>
					<View style={StyleSheet.button}>
						<Touchable
							onPress={async () => {
								await this.props.sendCommand("BlockMaster", !this.props.boardState.bm);
								return true;
							}}
							style={[{ backgroundColor: blockMasterBackgroundColor }]}
							background={Touchable.Ripple("blue")}>
							<Text style={StyleSheet.buttonTextCenter}> Block Master Remote
							</Text>
						</Touchable>
					</View>
					<View style={{ height: 50 }}></View>
					<WifiController wifi={this.props.wifi} boardState={this.props.boardState} sendCommand={this.props.sendCommand} />
					<View style={{ height: 50 }}></View>
					<DeviceController devices={this.props.devices} boardState={this.props.boardState} mediaType="Device" sendCommand={this.props.sendCommand} />
					<View style={{ height: 50 }}></View>
					<View style={StyleSheet.button}>
						<Touchable
							onPress={async () => {
								await this.props.sendCommand("SetCrisis", !this.props.boardState.r);
								return true;
							}}
							style={[{ backgroundColor: (this.props.boardState.r) ? "red" : "skyblue"  }]}
							background={Touchable.Ripple("blue")}>
							<Text style={StyleSheet.buttonTextCenter}> EMERGENCY!
							</Text>
						</Touchable>
					</View>
					<View style={{ height: 150 }}></View>
				</ScrollView>
			</View>
		);
	}
}

AdminManagement.propTypes = {
	boardState: PropTypes.object,
	sendCommand: PropTypes.func,
	devices: PropTypes.array,
	wifi: PropTypes.array,
};

