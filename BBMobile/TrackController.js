import React from "react";
import { View, Text, WebView, Button, TouchableHighlight, Slider, Picker } from "react-native";
import BleManager from 'react-native-ble-manager';
import BLEIDs from './BLEIDs';

export default class TrackController extends React.Component {
	constructor(props) {
		super(props);

		this.BLEIDs = new BLEIDs();
		this.audioChannelSelection = { channel: '' };

		this.state = {
			peripheral: null,
			channelNo: 0,
			audioChannels: [{
				channelNo: 0,
				channelInfo: ""
			}],
			haveAllChannels: false,
		}
	}

	componentWillReceiveProps(nextProps) {


		console.log("TrackController component received props:" + this.state.peripheral);

		if (nextProps.peripheral == null) this.setState(
			{
				peripheral: null,
				haveAllChannels: false,
			}
		);
		else {
			this.setState({
				peripheral: nextProps.peripheral,
				haveAllChannels: false,
			});
			this.readTrackListingFromBLE();
		}
	}

	// onUpdateVolume(event) {
	// 	console.log("VolumeController: submitted value: " + JSON.stringify(event.value));

	// 	var newVolume = [event.value];

	// 	if (this.state.peripheral) {
	// 		BleManager.write(this.state.peripheral.id, this.BLEIDs.AudioService, this.BLEIDs.AudioVolumeCharacteristic, newVolume)
	// 			.then(() => {
	// 				this.readVolumeFromBLE();
	// 			})
	// 			.catch(error => {
	// 				console.log("VolumeController: " + error);
	// 			});
	// 	}
	// }

	readTrackListingFromBLE() {

		console.log("TrackController Read Listing:" + this.state.peripheral);

		if (this.state.peripheral) {

			var audioChannels = [];
			var haveAllChannels = false;

			setInterval(() => {
				if (!this.state.haveAllChannels) {
					BleManager.read(this.state.peripheral.id, this.BLEIDs.AudioService, this.BLEIDs.AudioInfoCharacteristic).then((readData) => {
						console.log('TrackController Read Info: ' + readData);
						var channelNo = readData[0];
						var channelInfo = "";
						for (var i = 1; i < readData.length; i++) {
							channelInfo += String.fromCharCode(readData[i]);
						}
						if (channelInfo && 0 != channelInfo.length) {
							if (audioChannels[channelNo] != null) {
								console.log("TrackController " + channelNo + " already exists.");

								var audioArrayFull=true;
								// for(var n=0;n<audioChannels.length;n++){
								// 	if(audioChannels[n] == null)
								// 		audioArrayFull=false;
								// }

								if(audioArrayFull){
									this.state.haveAllChannels = true;
									console.log("TrackController Audio Array: " + audioChannels);
									haveAllChannels = true;
									this.setState({ audioChannels: audioChannels});
								}
							}
							else{
								audioChannels[channelNo] = { channelNo: channelNo, channelInfo: channelInfo };
								console.log('TrackController Add Info channel: ' + channelNo + ", name = " + channelInfo);
							}

						}

					})
						.catch((error) => {
							// Failure code
							console.log("TrackController r2: " + error);
							haveAllChannels = true;
						});
				}

			}, 1000);

		}
	}



	render() {

		// selectedValue={this.state.language}
		// onValueChange={(itemValue, itemIndex) => this.setState({ language: itemValue })}>

		return <View>


			{this.state.audioChannels.map((data) => {
				if (data != null) {
					return (
						<Text key={data.channelInfo}>{data.channelInfo}</Text>

					)
				}
			})}



		</View>;
	}
}
