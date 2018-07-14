import React, { Component } from "react";
import { View, Text, StyleSheet } from "react-native";
import PropTypes from "prop-types"; 
import StateBuilder from "./StateBuilder";
import Picker from "react-native-wheel-picker";
var PickerItem = Picker.Item;

export default class TrackController extends Component {
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
		var channelNo = null;

		if (this.props.mediaType == "Audio") {
			tracks = this.props.mediaState.audio.channels.map((a) => {
				return a.channelInfo;
			});
			channelNo = this.props.mediaState.audio.channelNo;
		}
		else {
			tracks = this.props.mediaState.video.channels.map((a) => {
				return a.channelInfo;
			});
			channelNo = this.props.mediaState.video.channelNo;
		}

		if (tracks.length > 1)
			tracks = tracks.slice(1, tracks.length);

		return (

			<View style={{ margin: 10, backgroundColor: "skyblue", }}>
				<View style={{
					flex: 1,
					flexDirection: "row",
				}}>
					<View>
						<Text style={styles.rowText}>{this.props.mediaType} Track</Text>
					</View>
				</View>
				<View style={styles.container}>
					<View style={styles.contentContainer}>

						<Picker style={{ height: 160 }}
							selectedValue={this.state.selectedValue} 
							itemStyle={{ color: "black", fontWeight: 'bold', fontSize: 26, height: 160 }}
							onValueChange={async (index) => {

								this.setState({ selectedValue: index });

								if (tracks[0] == "loading...") {
									console.log("dont call update if its a component load");
									return;
								}
								if ((channelNo - 1) == index) {
									console.log("dont call update if its not a real change");
									return;
								}

								var selected = null;

								if (this.props.mediaType == "Audio") {
									selected = this.props.mediaState.audio.channels.filter((a) => {
										return a.channelInfo == tracks[index];
									});
									this.onSelectTrack(selected[0].channelNo);
								}
								else {
									selected = this.props.mediaState.video.channels.filter((a) => {
										return a.channelInfo == tracks[index];
									});
									this.onSelectTrack(selected[0].channelNo);
								}
							}}>

							{tracks.map((value, i) => (
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
};

TrackController.propTypes = {
	mediaType: PropTypes.string,
	mediaState: PropTypes.object,
	onSelectTrack: PropTypes.func,
	refreshFunction: PropTypes.func,
	displayRefreshButton: PropTypes.bool,
};

const styles = StyleSheet.create({
	rowText: {
		margin: 5,
		fontSize: 14,
		padding: 5,
	},
});
