import React from "react";
import { View, Text,  Slider } from "react-native";
import PropTypes from "prop-types";
import StyleSheet from "./StyleSheet";

export default class VolumeController extends React.Component {
	constructor(props) {
		super(props);
 
		this.onUpdateVolume = this.props.onUpdateVolume.bind(this);

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
					<View style={{ height: 30 }}><Text style={StyleSheet.rowText}>{this.props.mediaState.audio.volume}</Text></View>
				</View>
				<View style={{ flex: 1,  height: 40, margin: 10 }}><Slider value={this.props.mediaState.audio.volume}
					onSlidingComplete={async (value) => {
						try {
							await this.onUpdateVolume(
								{ value }
							);
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
	mediaState: PropTypes.object,
	onUpdateVolume: PropTypes.func,
};
 