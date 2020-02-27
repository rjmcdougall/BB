import React from "react";
import { View, Text } from "react-native";
import PropTypes from "prop-types";
import StyleSheet from "./StyleSheet";
import Slider from "@react-native-community/slider";

export default class VolumeController extends React.Component {
	constructor(props) {
		super(props);
	}

	render() {

		return (
			<View style={{ flex: 1, margin: 2, backgroundColor: "skyblue", height: 80 }}>
				<View style={{
					flex: 1,
					flexDirection: "row",
					justifyContent: "space-between",

				}}>
					<View style={{ height: 30 }}><Text style={StyleSheet.rowText}>Volume</Text></View>
					<View style={{ height: 30 }}><Text style={StyleSheet.rowText}>{this.props.boardState.v}</Text></View>
				</View>
				<View style={{ flex: 1, height: 40, margin: 20 }}>
					<Slider value={this.props.boardState.v}
						trackStyle={StyleSheet.sliderTrack}
						thumbStyle={StyleSheet.sliderThumb}
						minimumTrackTintColor="blue"
						onSlidingComplete={async (volume) => {
							try {
								await this.props.sendCommand("Volume", volume);
							}
							catch (error) {
								console.log("VolumeController Error: " + error);
							}
						}}
						minimumValue={0} maximumValue={100} step={10} />
				</View>
			</View>
		);
	}
}

VolumeController.propTypes = {
	boardState: PropTypes.object,
	sendCommand: PropTypes.func,
};

