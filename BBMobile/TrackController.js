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

		if (this.props.mediaType == "Audio") {
			this.state = {
				tracks: [null, { channelNo: 1, channelInfo: "loading..." }],
				selectedTrack: props.mediaState.state.audioChannelNo,
			};
		}
		else if (this.props.mediaType == "Video") {
			this.state = {
				tracks: [null, { channelNo: 1, channelInfo: "loading..." }],
				selectedTrack: props.mediaState.state.videoChannelNo,
			};
		}
	}

	UNSAFE_componentWillReceiveProps(nextProps) {
		if (this.props.mediaType == "Audio") {
			this.setState({
				selectedTrack: nextProps.mediaState.state.audioChannelNo
			});
		}
		else if (this.props.mediaType == "Video") {
			this.setState({
				selectedTrack: nextProps.mediaState.state.videoChannelNo
			});
		}
	}

	findTrackNo(tracks, val) {
		return tracks.find(function(item, i) {
			if(item.localName === val){
			index = i;
			return i;
			}
		});
	}

	render() {
		var tracks = null;

		if (this.props.mediaType == "Audio")
			tracks = this.props.mediaState.audio;
		else if (this.props.mediaType == "Video")
			tracks = this.props.mediaState.video;

		//if (tracks.length > 1)
	//		tracks = tracks.localName.slice(1, tracks.length);
		console.log("hello");

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
							selectedValue={this.state.selectedTrack}
							itemStyle={{ color: "black", fontWeight: "bold", fontSize: 26, height: 140 }}
							onValueChange={async (value) => {

								if (tracks[0] == "loading...") {
									console.log("TrackController: dont call update if its a component load");
									return;
								}
								if (this.state.selectedTrack == value) {
									console.log("TrackController: dont call update if its not a real change");
									return;
								}

								this.setState({ selectedTrack: value });
								await this.props.onSelectTrack(value);

							}}>

							{tracks.map((elem, index) => (
								<PickerItem label={elem.localName} value={index} key={index} />
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
};

TrackController.propTypes = {
	mediaType: PropTypes.string,
	mediaState: PropTypes.object,
	onSelectTrack: PropTypes.func,
	refreshFunction: PropTypes.func,
	displayRefreshButton: PropTypes.bool,
};
