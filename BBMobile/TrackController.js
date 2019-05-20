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
				selectedTrack: props.mediaState.state.audioChannelNo,
			};
		}
		else if (this.props.mediaType == "Video") {
			this.state = {
				tracks: [{ localname: "loading..." }],
				selectedTrack: props.mediaState.state.videoChannelNo,
			};
		}
	}

	UNSAFE_componentWillReceiveProps(nextProps) {
		//console.log("trackcontroller props: " + JSON.stringify(this.props.mediaState.connectedPeripheral));
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
		return tracks.find(function (item, i) {
			if (item.localName === val) {
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
		//console.log("TrackController: " + this.props.mediaType);
		//tracks.map((elem, index) => { console.log(index + ": " + (elem.algorithm?elem.algorithm:elem.localName))});

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
								//console.log("TrackController: onValueChange: " + value);

								if (tracks[0] == "loading...") {
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

							{tracks.map((elem, index) => (
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
};
