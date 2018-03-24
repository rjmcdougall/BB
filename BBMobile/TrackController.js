import React from "react";
import { View, Text, WebView, Button, TouchableHighlight, Slider, Picker } from "react-native";
import BleManager from 'react-native-ble-manager';
import BLEIDs from './BLEIDs';

export default class TrackController extends React.Component {
	constructor(props) {
		super(props);

		this.BLEIDs = new BLEIDs();
		this.state = {
			peripheral: null,
			channelNo: 0,
			maxChannel: 0,
			audioChannels: [{
				channelNo: 0,
				channelInfo: "",
			}],
		}
	}

	componentWillReceiveProps(nextProps) {


		console.log("TrackController component received props:" + this.state.peripheral);

		if (nextProps.peripheral == null) this.setState(
			{
				peripheral: null,
			}
		);
		else {
			this.readTrackFromBLE(nextProps.peripheral);
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

	onUpdateTrack() {

		var newTrack;
		if (this.state.channelNo < this.state.maxChannel)
			newTrack = this.state.channelNo += 1;
		else{
			newTrack = 1;
		}

		console.log("TrackController: submitted value: " + [newTrack]);

		if (this.state.peripheral) {
			BleManager.write(this.state.peripheral.id,
				this.BLEIDs.AudioService,
				this.BLEIDs.AudioChannelCharacteristic,
				[newTrack])
				.then(() => {
					console.log("TrackController Update:  " + [newTrack]);
					this.readVolumeFromBLE(this.state.peripheral);
				})
				.catch(error => {
					console.log("TrackController: " + error);
				});
		}
	}

	readTrackFromBLE(peripheral) {

		console.log("TrackController Read Listing:" + peripheral);

		var myComponent = this;

		var audioChannels = [];

		if (peripheral) {

			BleManager.read(peripheral.id,
				this.BLEIDs.AudioService,
				this.BLEIDs.AudioChannelCharacteristic)
				.then((readData) => {
					myComponent.setState({
						channelNo: readData[1],
						maxChannel: readData[0]
					});
					console.log('TrackController Channel No: ' + this.state.channelNo);
					console.log('TrackController Max Channel: ' + this.state.maxChannel);
				})
				.catch((error) => {
					// Failure code
					console.log("TrackController r2: " + error);
					haveAllChannels = true;
				});

			// BleManager.read(peripheral.id,
			// 	this.BLEIDs.AudioService,
			// 	this.BLEIDs.AudioInfoCharacteristic)
			// 	.then((readData) => {

			// 		console.log('TrackController Read Info: ' + readData);
			// 		var channelNo = readData[0];
			// 		var channelInfo = "";
			// 		for (var i = 1; i < readData.length; i++) {
			// 			channelInfo += String.fromCharCode(readData[i]);
			// 		}
			// 		if (channelInfo && 0 != channelInfo.length) {
			// 			audioChannels[channelNo] = { channelNo: channelNo, channelInfo: channelInfo };
			// 		}

			// 		// this.setState({
			// 		// 	peripheral: , 
			// 		// });
			// 		console.log('TrackController Add Info channel: ' + channelNo + ", name = " + channelInfo);
			// 	})
			// 	.catch((error) => {
			// 		// Failure code
			// 		console.log("TrackController r2: " + error);
			// 		haveAllChannels = true;
			// 	});
		}
	}
	
	render() {

		return (
			<View>
				<TouchableHighlight style={{ marginTop: 40, margin: 20, padding: 20, backgroundColor: '#ccc' }}
					onPress={() => this.onUpdateTrack()}
				>
					<Text>Track {this.state.channelNo} of {this.state.maxChannel}</Text>
				</TouchableHighlight>
			</View>
		);

	}
}
