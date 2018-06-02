import React, { Component } from "react";
import { View, Text, StyleSheet } from "react-native";
import ModalDropdown from "react-native-modal-dropdown"; 
import PropTypes from "prop-types"; 
export default class TrackController extends Component {
	constructor(props) {
		super(props); 
		this.state = { 
			refreshButtonClicked: false,
		};

		this.onSelectTrack = this.props.onSelectTrack.bind(this);
	}
 
	render() {

		var tracks = null;
		var channelInfo = null;

		if (this.props.mediaType == "Audio") {
			tracks = this.props.mediaState.audio.channels.map(a => a.channelInfo);
			channelInfo =  tracks[(this.props.mediaState.audio.channelNo)];
		}
		else if (this.props.mediaType == "Device") {
			tracks = this.props.mediaState.device.devices.map(a => a.deviceLabel);
			channelInfo =  tracks[(this.props.mediaState.device.deviceNo)];
		}
		else {
			tracks = this.props.mediaState.video.channels.map(a => a.channelInfo);
			channelInfo =  tracks[(this.props.mediaState.video.channelNo)];
		} 

		return (

			<View style={{ margin: 10, backgroundColor: "skyblue", height: 80 }}>
				<View style={{
					flex: 1,
					flexDirection: "row",
				}}>
					<View style={{ height: 40 }}>
						<Text style={styles.rowText}>{this.props.mediaType} Track</Text></View>
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
			</View>
		);
	}
}

TrackController.propTypes = { 
	mediaType: PropTypes.string,
	mediaState: PropTypes.object,
	onSelectTrack: PropTypes.func,
};

const styles = StyleSheet.create({
	PStyle: {
		backgroundColor: "skyblue",
		width: 280,
	},
	DDStyle: {
		backgroundColor: "skyblue",
		width: 280,
	},
	rowText: {
		margin: 5,
		fontSize: 14,
		padding: 5,
	},
});
