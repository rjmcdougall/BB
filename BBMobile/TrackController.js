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
				tracks: [{ localName: "loading..." }],
				selectedTrack: 29999,
			};
		}
		else if (this.props.mediaType == "Video") {
			this.state = {
				tracks: [{ localName: "loading..." }],
				selectedTrack: 29999,
			};
		}
	}

	static getDerivedStateFromProps(props, state) {
		if (props.mediaType == "Audio") {
			// if the state track has never been set before.
			if(state.selectedTrack==29999 || state.selectedTrack == 9999){
				// if the props track is a real track.
				if (props.mediaState.state.audioChannelNo<9999){
					console.log(props.mediaState.state.audioChannelNo + "Found Audio for the first time!!!");
					return {
						selectedTrack: props.mediaState.state.audioChannelNo,
						tracks: props.mediaState.audio,
					};
				}
				else {
					return state;
				}				
			}
			else {
				return state;
			}
		}
		else if (props.mediaType == "Video") {
			// if the state track has never been set before.
			if(state.selectedTrack==29999 || state.selectedTrack == 9999){
				// if the props track is a real track.
				if (props.mediaState.state.videoChannelNo<9999){
					console.log(props.mediaState.state.videoChannelNo + "Found video for the first time!!!");
					return {
						selectedTrack: props.mediaState.state.videoChannelNo,
						tracks: props.mediaState.video,
					};
				}
				else {
					return state;
				}				
			}
			else {
				return state;
			}
		}
	}

	findTrackNo(tracks, val) {
		return tracks.find(function (item, i) {
			if (item.localName === val) {
				return i;
			}
		});
	}

	render() {

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

								if (this.state.tracks[0] == "loading...") {
									console.log("TrackController: dont call update if its a component load");
									return;
								}
								if (this.state.selectedTrack == value) {
									console.log("TrackController: dont call update if its not a real change");
									return;
								}

								this.setState({ selectedTrack: value });
								this.props.sendCommand(this.props.mediaState, this.props.mediaType, value);

							}}>

							{this.state.tracks.map((elem, index) => (
								<PickerItem label={elem.algorithm ? elem.algorithm : elem.localName} value={index} key={index} />
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
	sendCommand: PropTypes.func,
};
