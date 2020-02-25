import React, { Component } from "react";
import { View, Text } from "react-native";
import ModalDropdown from "./ModalDropdown";
import PropTypes from "prop-types";
import Touchable from "react-native-platform-touchable";
import StyleSheet from "./StyleSheet";

export default class DeviceController extends Component {
	constructor(props) {
		super(props);
		this.state = {
			refreshButtonClicked: false,
		};

		this.pairButton = this.pairButton.bind(this);
	}

	pairButton(value){
		var dev = this.props.devices[value];
		this.props.sendCommand("BTSelect", dev.address);
	}
	render() {

 
		var devs = this.props.devices.map(a => a.name + (a.paired ? "paired" : ""));

		return (

			<View style={StyleSheet.button}>
				<View style={{
					flex: 1,
					flexDirection: "row",
				}}>
					<View style={{ height: 40 }}>
						<Text style={StyleSheet.rowText}>Connect to Hitachi Button</Text></View>
				</View>
				<View style={{ height: 40 }}>
					<ModalDropdown options={devs}
						style={StyleSheet.button}
						dropdownStyle={StyleSheet.button}
						textStyle={StyleSheet.dropDownRowText}
						dropdownTextStyle={StyleSheet.dropDownRowText}
						dropdownTextHighlightStyle={StyleSheet.dropDownRowText}
						onSelect={this.pairButton.bind(this)}
					/>
				</View>
				<View style={StyleSheet.button}>
					<Touchable
						onPress={async () => {
							try {
								await this.props.sendCommand("BTScan", null);
								return true;
							}
							catch (error) {
								console.log(error);
							}

						}}
						background={Touchable.Ripple("blue")}>
						<Text style={StyleSheet.buttonTextCenter}> Scan for Buttons
						</Text>
					</Touchable>
				</View>
			</View>
		);
	}
}

DeviceController.propTypes = {
	mediaType: PropTypes.string,
	devices: PropTypes.array,
	sendCommand: PropTypes.func,
	displayRefreshButton: PropTypes.bool,
};

