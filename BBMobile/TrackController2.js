import React, { Component } from "react";
import { View, Text, WebView, Button, TouchableHighlight, Slider, StyleSheet, AsyncStorage } from "react-native";
import ModalDropdown from 'react-native-modal-dropdown';
import BleManager from 'react-native-ble-manager';
import BLEIDs from './BLEIDs';

export default class TrackController extends Component {
	constructor(props) {
		super(props);

		this.BLEIDs = new BLEIDs();

		if(props.mediaType=="Audio"){
			this.state = {
				peripheral: props.peripheral,
				mediaType: props.mediaType,
				channelNo: 0,
				channelInfo: "Loading (~60 seconds)",
				maxChannel: 0,
				channels: [{ channelInfo: "loading..." }], 
				service: this.BLEIDs.AudioService,
				channelCharacteristic: this.BLEIDs.AudioChannelCharacteristic,
				infoCharacteristic: this.BLEIDs.AudioInfoCharacteristic,
			};
		}
		else if(props.mediaType=="Video"){
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
			};
		} 
	}
 
	async loadTracksFromStorage(){
		
		console.log("loading from storage");

		try{
			var channels = [];

			console.log("get item");

			var channelsLength = await AsyncStorage.getItem(this.state.mediaType + ":length",(error) => {
				 console.log(error + "hi");
			});
			
			if(channelsLength){
				console.log("channelslength: " + channelsLength);
	
				for (var i = 1; i < channelsLength; i++) {
					var i = 1;
	
					var channelInfo = await AsyncStorage.getItem(this.state.mediaType + ":" + i,(error) => {
						if(error) console.log(error);
					});
					
					channels[i] = { channelNo: i, channelInfo: channelInfo };
				}
				console.log("found array in asyncstorage: " + JSON.stringify(channels));
		
				this.setState({ channelInfo: channels });
			}
			else {

			}

	
		}
		catch (error) {
			console.log(error);
		}

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
 
	componentWillMount(){

		this.loadTracksFromStorage();

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
						//defaultValue={this.state.channelInfo}
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
