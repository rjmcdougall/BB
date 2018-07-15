import React, { Component } from "react";
import { View, Text } from "react-native";
import ModalDropdown from "react-native-modal-dropdown";
import PropTypes from "prop-types";
import Touchable from "react-native-platform-touchable";
import StyleSheet from "./StyleSheet";

export default class DeviceController extends Component {
	constructor(props) {
		super(props);
		this.state = {
			refreshButtonClicked: false,
		};

		if (this.props.refreshFunction)
			this.refreshFunction = this.props.refreshFunction.bind(this);
	}

	render() {

		var tracks = this.props.mediaState.device.devices.map(a => a.deviceLabel);

		return (
			<View style={StyleSheet.button}>
				<View style={{
					flex: 1,
					flexDirection: "row",
				}}>
					<View style={{ height: 40 }}>
						<Text style={StyleSheet.rowText}>Devices</Text></View>
				</View>
				<View style={{ height: 40 }}>
					<ModalDropdown options={tracks}
						defaultValue={tracks[this.props.mediaState.device.deviceNo]}
						style={StyleSheet.button}
						dropdownStyle={StyleSheet.button}
						textStyle={StyleSheet.dropDownRowText}
						dropdownTextStyle={StyleSheet.dropDownRowText}
						dropdownTextHighlightStyle={StyleSheet.dropDownRowText}
						onSelect={async (idx) => {
							await this.props.onSelectTrack(idx);
						}}
					/>
				</View>
				<View style={StyleSheet.button}>
					<Touchable
						onPress={async () => {

							this.setState({ mediaState: await this.props.refreshFunction() });

							return true;
						}}
						background={Touchable.Ripple("blue")}>
						<Text style={StyleSheet.buttonTextCenter}> Refresh BT Devices
						</Text>
					</Touchable>
				</View>
			</View>
		);
	}
}

DeviceController.propTypes = {
	mediaType: PropTypes.string,
	mediaState: PropTypes.object,
	onSelectTrack: PropTypes.func,
	refreshFunction: PropTypes.func,
	displayRefreshButton: PropTypes.bool,
};

