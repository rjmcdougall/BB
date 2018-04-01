import React, { Component } from "react";
import { View, Text, WebView, Button, TouchableHighlight, Slider, StyleSheet } from "react-native";
import ModalDropdown from 'react-native-modal-dropdown';
import BleManager from 'react-native-ble-manager';
import BLEIDs from './BLEIDs';

export default class TrackController extends Component {
	constructor(props) {
		super(props);

		this.BLEIDs = new BLEIDs();

		if(props.mediaType=="Audio"){
			this.state = {
				scannerIsRunning: false,
				peripheral: props.peripheral,
				mediaType: props.mediaType,
				channelNo: 0,
				channelInfo: "Loading (~60 seconds)",
				maxChannel: 0,
				channels: [{ channelInfo: "loading..." }],
				haveAllChannels: false,
				service: this.BLEIDs.AudioService,
				channelCharacteristic: this.BLEIDs.AudioChannelCharacteristic,
				infoCharacteristic: this.BLEIDs.AudioInfoCharacteristic,
			};
		}
		else if(props.mediaType=="Video"){
			this.state = {
				scannerIsRunning: false,
				peripheral: props.peripheral,
				mediaType: props.mediaType,
				channelNo: 0,
				channelInfo: "Loading (~60 seconds)",
				maxChannel: 0,
				channels: [{ channelInfo: "loading..." }],
				haveAllChannels: false,
				service: this.BLEIDs.VideoService,
				channelCharacteristic: this.BLEIDs.VideoChannelCharacteristic,
				infoCharacteristic: this.BLEIDs.VideoInfoCharacteristic,
			};
		} 
	}

	componentWillReceiveProps(nextProps) {

		console.log("TrackController " + this.state.mediaType + " component received props:" + nextProps);

		if (nextProps.peripheral == null) {
			console.log("TrackController " + this.state.mediaType + " clearing state...");
			this.setState({
				peripheral: null,
				haveAllChannels: false,
				channels: [{ channelInfo: "Please Select..." }],
				channelNo: 0,
				maxChannel: 0,
			});
		} else {
			console.log("TrackController " + this.state.mediaType + " setting state...");
			this.setState({
				peripheral: nextProps.peripheral,
				haveAllChannels: false,
			});
		}
	}

	componentDidMount() {
		console.log("TrackController " + this.state.mediaType + " DidMount");
		console.log("TrackController " + this.state.mediaType + " state: " + this.state);
		// Will make this a board-side notify and we can get rid of this
		if (!this.scannerIsRunning) {
			this.setState({ scannerIsRunning: true });
			this.readTrackListingFromBLE();
		}
	}

	componentWillUnmount() {
		console.log("TrackController " + this.state.mediaType + " Unmounting ");
		console.log("TrackController " + this.state.mediaType + " background loop: " + this.state.backgroundLoop);
		// how to stop the timer?
		clearInterval(this.state.backgroundLoop);
	}

	readTrackFromBLE(peripheral) {

		var myComponent = this;

		if (peripheral) {

			console.log("TrackController " + this.state.mediaType + " doing Read current/track counts:" + peripheral);
			BleManager.read(peripheral.id,
				this.state.service,
				this.state.channelCharacteristic)
				.then((readData) => {
					myComponent.setState({
						channelNo: readData[1],
						maxChannel: readData[0]
					});
					console.log("TrackController " + this.state.mediaType + " Selected Channel No: " + this.state.channelNo);
					console.log("TrackController " + this.state.mediaType + " Max Channel: " + this.state.maxChannel);
				})
				.catch((error) => {
					// Failure code
					console.log("TrackController " + this.state.mediaType + " read track: " + error);
				});
		}
	}

	readTrackListingFromBLE() {

		console.log("TrackController " + this.state.mediaType + " Read Listing:" + this.state.peripheral);
		var channels = [];
		var stopLooking = false;
		var channelsCollected = 0;

		if (this.state.maxChannel == 0) {
			setTimeout(() => {
				this.readTrackFromBLE(this.state.peripheral);
			}, 10000);
		}

		var backgroundTimer = setInterval(() => {
			if (this.state.peripheral && !this.state.haveAllChannels) {

				BleManager.read(this.state.peripheral.id, this.state.service, this.state.infoCharacteristic).then((readData) => {
					console.log("TrackController " + this.state.mediaType + " Read Info: " + readData);
					var channelNo = readData[0];
					var channelInfo = "";
					for (var i = 1; i < readData.length; i++) {
						channelInfo += String.fromCharCode(readData[i]);
					}
					if (channelInfo && 0 != channelInfo.length) {
						if (channels[channelNo] != null) {
							console.log("TrackController " + this.state.mediaType + " " + channelNo + " already exists.");
							console.log("TrackController " + this.state.mediaType + " Max Channel: " + this.state.maxChannel);

                           if(channelsCollected == this.state.maxChannel){
                                var tracks = channels.map(a => a.channelInfo);
                                console.log("TrackController " + this.state.mediaType + " Tracks: " + tracks);
                                stopLooking = true;
								this.setState({ channels: channels,
												haveAllChannels: true});
                            }

						} else {
							channels[channelNo] = { channelNo: channelNo, channelInfo: channelInfo };

							if(channelNo==this.state.channelNo)
								this.setState({channelInfo: channels[channelNo].channelInfo});

							channelsCollected += 1;
							console.log("TrackController " + this.state.mediaType + " Add Info channel: " + channelNo + ", name = " + channelInfo);
							console.log("TrackController " + this.state.mediaType + " Max Channel: " + this.state.maxChannel);
							console.log("TrackController " + this.state.mediaType + " Channel count: " + channelsCollected);
						}
					}
				})
					.catch((error) => {
						// Failure code
						console.log("TrackController " + this.state.mediaType + " r2: " + error);
						stopLooking = true;
					});
			}

		}, 500);
		this.setState({ backgroundLoop: backgroundTimer });
	}

	setTrack(idx) {

		var trackNo = parseInt(idx);

		console.log("TrackController " + this.state.mediaType + " submitted value: " + trackNo);

		if (this.state.peripheral) {
			BleManager.write(this.state.peripheral.id,
				this.state.service,
				this.state.channelCharacteristic,
				[trackNo])
				.then(() => {
					console.log("TrackController " + this.state.mediaType + " Update:  " + [trackNo]);
					this.readTrackFromBLE(this.state.peripheral);
				})
				.catch(error => {
					console.log("TrackController " + this.state.mediaType + " " + error);
				});
		}
	}

	onSelect(idx) {
		console.log("TrackController " + this.state.mediaType + " Selected: " + idx);
		console.log("TrackController " + this.state.mediaType + " Selected track: " + this.state.channels[idx].channelInfo);
		this.setTrack(idx);
	}

	render() {

		var tracks = this.state.channels.map(a => a.channelInfo);
		return (

			<View style={{ margin: 20, backgroundColor: 'skyblue', height: 100 }}>
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
