import React, { Component } from "react";
import { View, Text, WebView, Button, TouchableHighlight, Slider, StyleSheet, AsyncStorage } from "react-native";
import BleManager from 'react-native-ble-manager';
import BLEIDs from './BLEIDs';

export default class TrackRefresher extends Component {
	constructor(props) {
		super(props);

		this.BLEIDs = new BLEIDs();

		if (props.mediaType == "Audio") {
			this.state = {
				refreshButtonClicked: false,
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
		else if (props.mediaType == "Video") {
			this.state = {
				refreshButtonClicked: false,
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

	componentWillUnmount() {
		console.log("RefreshController " + this.state.mediaType + " Unmounting ");
		console.log("RefreshController " + this.state.mediaType + " background loop: " + this.state.backgroundLoop);
		// how to stop the timer?
		clearInterval(this.state.backgroundLoop);
	}

	async readTrackFromBLE(peripheral) {

		var myComponent = this;

		if (peripheral) {

			console.log("RefreshController " + this.state.mediaType + " doing Read current/track counts:" + peripheral);

			try {
				var readData = await BleManager.read(peripheral.id,
					this.state.service,
					this.state.channelCharacteristic);

				await myComponent.setState({
					channelNo: readData[1],
					maxChannel: readData[0]
				});

				console.log("RefreshController " + this.state.mediaType + " Selected Channel No: " + this.state.channelNo);
				console.log("RefreshController " + this.state.mediaType + " Max Channel: " + this.state.maxChannel);

			}
			catch (error) {
				console.log("RefreshController " + this.state.mediaType + " read track: " + error);
			}
		}
	}

	async refresh() {

		console.log("RefreshController " + this.state.mediaType + " Read Listing:" + this.state.peripheral);
		var channels = [];
		var stopLooking = false;
		var channelsCollected = 0;

		if (this.state.peripheral) {
			if (!this.state.backgroundLoop) {

				if (this.state.maxChannel == 0) {
					setTimeout(async () => {
						await this.readTrackFromBLE(this.state.peripheral);
					}, 5000);
				}

				var backgroundTimer = setInterval(async () => {

					if (!this.state.haveAllChannels) {
						try {
							var readData = await BleManager.read(this.state.peripheral.id, this.state.service, this.state.infoCharacteristic);

							var channelNo = readData[0];
							var channelInfo = "";
							for (var i = 1; i < readData.length; i++) {
								channelInfo += String.fromCharCode(readData[i]);
							}
							if (channelInfo && 0 != channelInfo.length) {
								if (channels[channelNo] != null) {
									console.log("RefreshController " + this.state.mediaType + " " + channelNo + " already exists.");
									console.log("RefreshController " + this.state.mediaType + " Max Channel: " + this.state.maxChannel);

									if (channelsCollected == this.state.maxChannel) {
										
										console.log("RefreshController " + this.state.mediaType + " Tracks: " + JSON.stringify(channels));
 
										for (var i = 1; i < channels.length; i++) {
											
											await AsyncStorage.setItem(this.state.mediaType + ":" + channels[i].channelNo,channels[i].channelInfo,(error) => {
												if(error) console.log(error);
											});
											var res = await AsyncStorage.getItem(this.state.mediaType + ":" + channels[i].channelNo,(error) => {
												if(error) console.log(error);
											});
											console.log("***** RefreshController Stored " + res);
										}

										this.setState({
											channels: channels,
											haveAllChannels: true
										});
									}

								} else {
									channels[channelNo] = { channelNo: channelNo, channelInfo: channelInfo };

									if (channelNo == this.state.channelNo)
										this.setState({ channelInfo: channels[channelNo].channelInfo });

									channelsCollected += 1;
									console.log("RefreshController " + this.state.mediaType + " Add Info channel: " + channelNo + ", name = " + channelInfo);
									console.log("RefreshController " + this.state.mediaType + " Max Channel: " + this.state.maxChannel);
									console.log("RefreshController " + this.state.mediaType + " Channel count: " + channelsCollected);
								}
							}
						}
						catch (error) {
							console.log("RefreshController " + this.state.mediaType + " r2: " + error);
							stopLooking = true;
						}
					}


				}, 500);

				this.setState({ backgroundLoop: backgroundTimer });
			}
		}
	}

	render() {

		var tracks = this.state.channels.map(a => a.channelInfo);
		return (

			<View style={{ margin: 20, backgroundColor: 'skyblue', height: 100 }}>

				<Button
					title="Refresh"
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

