import React, {Component} from "react";
import { View, Text, WebView, Button, TouchableHighlight, Slider } from "react-native";
import ModalDropdown from 'react-native-modal-dropdown';
import BleManager from 'react-native-ble-manager';
import BLEIDs from './BLEIDs';

const DEMO_OPTIONS_1 = ['option 1', 'option 2', 'option 3', 'option 4', 'option 5', 'option 6', 'option 7', 'option 8', 'option 9'];


export default class TrackController extends Component {
	constructor(props) {
		super(props);

		this.BLEIDs = new BLEIDs();
		this.audioChannelSelection = { channel: '' };
        console.log("initializing state...");

		this.state = {
            scannerIsRunning: false,
			peripheral: props.peripheral,
			channelNo: 0,
            maxChannel: 0,
			audioChannels: [],
			haveAllChannels: false,
		};
	}

	componentWillReceiveProps(nextProps) {

		//console.log("TrackController component received props:" + this.state.peripheral);
		console.log("TrackController component received props:" + nextProps);
		//console.log(nextProps);

		if (nextProps.peripheral == null) {
            console.log("clearing state...");
            this.setState({
		    	peripheral: null,
		    	haveAllChannels: false
			    });
		} else {
            console.log("setting state...");
			this.setState({
				peripheral: nextProps.peripheral,
				haveAllChannels: false,
			});
		}
	}

    componentDidMount() {
        console.log("DidMount");
        console.log(this.state);
        // Will make this a board-side notify and we can get rid of this
        if (!this.scannerIsRunning) {
            this.setState({scannerIsRunning: true});
    		this.readTrackListingFromBLE();
        }
    }

    componentWillUnmount() {
        console.log("Unmounting TrackController...");
        console.log(this.state.backgroundLoop);
        // how to stop the timer?
        clearInterval(this.state.backgroundLoop);
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


    readTrackFromBLE(peripheral) {

		console.log("TrackController Read current/track counts:" + peripheral);

		var myComponent = this;

		if (peripheral) {

            console.log("TrackController doing Read current/track counts:" + peripheral);
			BleManager.read(peripheral.id,
				this.BLEIDs.AudioService,
				this.BLEIDs.AudioChannelCharacteristic)
				.then((readData) => {
		            console.log("TrackController complted Read current/track counts:" + peripheral);
					myComponent.setState({
						channelNo: readData[1],
						maxChannel: readData[0]
					});
					console.log('TrackController Channel No: ' + this.state.channelNo);
					console.log('TrackController Max Channel: ' + this.state.maxChannel);
				})
				.catch((error) => {
					// Failure code
					console.log("TrackController read track: " + error);
				});
        }
    }

	readTrackListingFromBLE() {

		console.log("TrackController Read Listing:" + this.state.peripheral);
		var audioChannels = [];
		var haveAllChannels = false;
        var channelsCollected = 0;

	    var backgroundTimer = setInterval(() => {
	        if (this.state.peripheral &&  !this.state.haveAllChannels) {

                if (this.state.maxChannel == 0) {
                    setTimeout(() => {
                        this.readTrackFromBLE(this.state.peripheral);
                    }, 600);
                }

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
                                var tracks = audioChannels.map(a => a.channelInfo);
								console.log("Demo: " + tracks);
								haveAllChannels = true;
								this.setState({ audioChannels: audioChannels});
							}
						} else {
							audioChannels[channelNo] = { channelNo: channelNo, channelInfo: channelInfo };
                            channelsCollected += 1;
							console.log('TrackController Add Info channel: ' + channelNo + ", name = " + channelInfo);
        					console.log('TrackController Max Channel: ' + this.state.maxChannel);
					        console.log('TrackController Channel No: ' + this.state.channelNo);
					        console.log('TrackController Channel count: ' + channelsCollected);
						}

					}

				})
				.catch((error) => {
						// Failure code
						console.log("TrackController r2: " + error);
						haveAllChannels = true;
				});
			}

		}, 500);
        this.setState({backgroundLoop: backgroundTimer});
	}

	setTrack(idx) {

        var trackNo = parseInt(idx);

		console.log("TrackController: submitted value: " + trackNo);

		if (this.state.peripheral) {
			BleManager.write(this.state.peripheral.id,
				this.BLEIDs.AudioService,
				this.BLEIDs.AudioChannelCharacteristic,
				[trackNo])
				.then(() => {
					console.log("TrackController Update:  " + [trackNo]);
					this.readTrackFromBLE(this.state.peripheral);
				})
				.catch(error => {
					console.log("TrackController: " + error);
				});
		}
	}

    onSelect(idx) {
        console.log("Selected: " + idx);
        console.log("Selected track: " + this.state.audioChannels[idx].channelInfo);
        this.setTrack(idx);
    }

	render() {

        var tracks = this.state.audioChannels.map(a => a.channelInfo);


        return (
            <View>
                    <ModalDropdown  options={tracks}
                                    onSelect={this.onSelect.bind(this)}
                    />
            </View>
        );
	}
}
