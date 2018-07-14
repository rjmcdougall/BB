import React, { Component } from "react";
import { View, Text, StyleSheet } from "react-native";
import ModalDropdown from "react-native-modal-dropdown";
import PropTypes from "prop-types";
import Touchable from "react-native-platform-touchable";

export default class DeviceController extends Component {
	constructor(props) {
		super(props);
		this.state = {
			refreshButtonClicked: false,
		};

		this.onSelectTrack = this.props.onSelectTrack.bind(this);
		if (this.props.refreshFunction)
			this.refreshFunction = this.props.refreshFunction.bind(this);
	}

	render() {

		var tracks = null;
		var channelInfo = null;

		tracks = this.props.mediaState.device.devices.map(a => a.deviceLabel);
		channelInfo = tracks[(this.props.mediaState.device.deviceNo)];

		var refreshButton;
		if (this.props.refreshFunction) {
			refreshButton = (
				<View style={styles.button}>
					<Touchable
						onPress={async () => {

							this.setState({ mediaState: await this.props.refreshFunction() });

							return true;
						}}
						style={[styles.touchableStyle]}
						background={Touchable.Ripple("blue")}>
						<Text style={styles.rowTextCenter}> Refresh BT Devices
						</Text>
					</Touchable>
				</View>
			);
		}
		else
			refreshButton = (<Text></Text>);

		return (

			<View style={{ margin: 10, backgroundColor: "skyblue", height: 80 }}>
				<View style={{
					flex: 1,
					flexDirection: "row",
				}}>
					<View style={{ height: 40 }}>
						<Text style={styles.rowText}>Devices</Text></View>
				</View>
				<View style={{ height: 40 }}>
					<ModalDropdown options={tracks}
						defaultValue={channelInfo}
						style={styles.PStyle}
						dropdownStyle={styles.DDStyle}
						textStyle={styles.rowText}
						dropdownTextStyle={styles.rowText}
						dropdownTextHighlightStyle={styles.rowText}
						onSelect={this.onSelectTrack.bind(this)}
					/>
				</View>
				{refreshButton}
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

const styles = StyleSheet.create({
	PStyle: {
		backgroundColor: "skyblue", 
	},
	DDStyle: {
		backgroundColor: "skyblue", 
	},
	rowText: {
		margin: 5,
		fontSize: 14,
		padding: 5,
	},
	rowTextCenter: {
		margin: 5,
		fontSize: 14,
		textAlign: "center",
		padding: 10,
	},
});