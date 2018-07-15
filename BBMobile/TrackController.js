import React, { Component } from "react";
import { View, Text } from "react-native";
import PropTypes from "prop-types";
import StateBuilder from "./StateBuilder";
import Picker from "react-native-wheel-picker";
import StyleSheet from "./StyleSheet";

var PickerItem = Picker.Item;
export default class TrackController extends Component {
	constructor(props) {
		super(props);
	}

	render() {

		var channelNo;
		var channels;
		if(this.props.mediaType == "Audio"){
			channelNo = this.props.mediaState.audio.channelNo;
			channels = this.props.mediaState.audio.channels;
		}
		else {
			channelNo = this.props.mediaState.video.channelNo;
			channels = this.props.mediaState.video.channels;
		}
		
		return (

			<View style={{ margin: 2, backgroundColor: "skyblue" }}>
				<View style={{
					flex: 1,
					flexDirection: "row",
				}}>
					<View>
						<Text style={StyleSheet.rowText}>{this.props.mediaType} Track</Text>
					</View>
				</View>
				<View style={StyleSheet.container}>
					<View style={StyleSheet.container}>

						<Picker style={{ height: 150 }}
							selectedValue={channelNo}
							itemStyle={{ color: "black", fontWeight: "bold", fontSize: 26, height: 140 }}
							onValueChange={async (index) => {

								if (channels[0] == "loading...") {
									console.log("dont call update if its a component load");
									return;
								}
								if ((channelNo) == index) {
									console.log("dont call update if its not a real change");
									return;
								}

								console.log("index " + index)
								await this.props.onSelectTrack(index);

							}}>

							{channels.map((value, i) => (
								<PickerItem label={value} value={i} key={"money" + value} />
							))}

						</Picker>
					</View>
				</View>
			</View>
		);
	}
}

TrackController.defaultProps = {
	mediaState: StateBuilder.blankMediaState(),
	mediaType: "Audio",
};

TrackController.propTypes = {
	mediaType: PropTypes.string,
	mediaState: PropTypes.object,
	onSelectTrack: PropTypes.func, 
};
