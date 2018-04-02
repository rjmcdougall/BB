import React, { Component } from "react";
import { View, Text, WebView, Button, TouchableHighlight, Slider, StyleSheet } from "react-native";
import ModalDropdown from 'react-native-modal-dropdown';
import BleManager from 'react-native-ble-manager';
import BLEIDs from './BLEIDs';

export default class TrackController extends Component {
	constructor(props) {
		super(props);

		this.BLEIDs = new BLEIDs();

		if (props.mediaType == "Audio") {
			this.state = {
				peripheral: props.peripheral,
				mediaType: props.mediaType,
				channelNo: 0,
				channelInfo: "Please Load",
				maxChannel: 0,
				channels: [{ channelInfo: "loading..." }],
				service: this.BLEIDs.AudioService,
				channelCharacteristic: this.BLEIDs.AudioChannelCharacteristic,
				infoCharacteristic: this.BLEIDs.AudioInfoCharacteristic,
				refreshButtonClicked: false,
			};
		}
		else if (props.mediaType == "Video") {
			this.state = {
				peripheral: props.peripheral,
				mediaType: props.mediaType,
				channelNo: 0,
				channelInfo: "Loading (~60 seconds)",
				maxChannel: 0,
				channels: [{ channelInfo: "loading..." }],
				service: this.BLEIDs.VideoService,
				channelCharacteristic: this.BLEIDs.VideoChannelCharacteristic,
				infoCharacteristic: this.BLEIDs.VideoInfoCharacteristic,
				refreshButtonClicked: false,
			};
		}
	}

	componentWillReceiveProps(nextProps) {

		console.log("TrackController " + this.state.mediaType + " component received props:" + nextProps);

		if (nextProps.peripheral == null) {
			console.log("TrackController " + this.state.mediaType + " clearing state...");
			this.setState({
				peripheral: null,
				channels: [{ channelInfo: "Please Select..." }],
				channelNo: 0,
				maxChannel: 0,
			});
		} else {
			console.log("TrackController " + this.state.mediaType + " setting state...");
			this.setState({
				peripheral: nextProps.peripheral,
			});
		}
	}

	async componentDidMount() {
		console.log("TrackController " + this.state.mediaType + " DidMount");
		await this.refresh();
	}

	async readTrackFromBLE(peripheral) {
		if (peripheral) {
			try {
				console.log("TrackController " + this.state.mediaType + " doing Read current/track counts:" + peripheral);
				var readData = await BleManager.read(peripheral.id,
					this.state.service,
					this.state.channelCharacteristic);

				this.setState({
					channelNo: readData[1],
					maxChannel: readData[0]
				});
				console.log("TrackController " + this.state.mediaType + " Selected Channel No: " + this.state.channelNo);
				console.log("TrackController " + this.state.mediaType + " Max Channel: " + this.state.maxChannel);

			}
			catch (error) {
				console.log("TrackController " + this.state.mediaType + " read track: " + error);
			}
		}
	}

	async refresh() {

		console.log("TrackController " + this.state.mediaType + " Read Listing:" + this.state.peripheral);
		var channels = [];
		await this.readTrackFromBLE(this.state.peripheral);

		try {
			if (this.state.peripheral) {
				for (var n = 1; n <= this.state.maxChannel; n++) {

					var readData = await BleManager.read(this.state.peripheral.id, this.state.service, this.state.infoCharacteristic);
					var channelNo = readData[0];
					var channelInfo = "";
					for (var i = 1; i < readData.length; i++) {
						channelInfo += String.fromCharCode(readData[i]);
					}
					if (channelInfo && 0 != channelInfo.length) {
						channels[channelNo] = { channelNo: channelNo, channelInfo: channelInfo };
						if (channelNo == this.state.channelNo)
							this.setState({ channelInfo: channels[channelNo].channelInfo });

						console.log("TrackController " + this.state.mediaType + " Add Info channel: " + channelNo + ", name = " + channelInfo);
					}
				}
				console.log("TrackController " + this.state.mediaType + " found channels: " + JSON.stringify(channels));
				this.setState({
					channels: channels,
				});
			}
		}
		catch (error) {
			console.log("TrackController " + this.state.mediaType + " r2: " + error);
		}
	}

	async setTrack(idx) {

		var trackNo = parseInt(idx);
		console.log("TrackController " + this.state.mediaType + " submitted value: " + trackNo);
		if (this.state.peripheral) {

			try {
				await BleManager.write(this.state.peripheral.id,
					this.state.service,
					this.state.channelCharacteristic,
					[trackNo]);

				console.log("TrackController " + this.state.mediaType + " Update:  " + [trackNo]);
				await this.readTrackFromBLE(this.state.peripheral);
			}
			catch (error) {
				console.log("TrackController " + this.state.mediaType + " " + error);
			}
		}
	}

	onSelect(idx) {
		console.log("TrackController " + this.state.mediaType + " Selected track: " + this.state.channels[idx].channelInfo);
		this.setTrack(idx);
	}

	render() {

		var tracks = this.state.channels.map(a => a.channelInfo);
		return (

			<View style={{ margin: 10, backgroundColor: 'skyblue', height: 120 }}>
				<View style={{
					flex: 1,
					flexDirection: 'row',
				}}>
					<View style={{ height: 50 }}>
						<Text style={styles.rowText}>{this.state.mediaType} Track</Text></View>
				</View>
				<View style={{ height: 50 }}>
					<ModalDropdown options={tracks}
						defaultValue={this.state.channelInfo}
						style={styles.PStyle}
						dropdownStyle={styles.DDStyle}
						textStyle={styles.rowText}
						dropdownTextStyle={styles.rowText}
						dropdownTextHighlightStyle={styles.rowText}
						onSelect={this.onSelect.bind(this)}
					/>
				</View>
				<Button
				title="Load"
				onPress={async () => {
					if (!this.state.refreshButtonClicked) {
						this.setState({ refreshButtonClicked: true });
						await this.refresh();
					}
				}
				} />
			</View>
		);
	}
}

const styles = StyleSheet.create({
	container: {
		flex: 1,
		backgroundColor: 'skyblue',
		width: window.width,
		height: window.height
	},
	PStyle: {
		backgroundColor: 'skyblue',
		width: 280,
	},
	DDStyle: {
		backgroundColor: 'skyblue',
		width: 280,
	},
	rowText: {
		margin: 5,
		fontSize: 16,
		padding: 10,
	},
});
