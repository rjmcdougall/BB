import React from "react";
import { View, Text, WebView, Button, TouchableHighlight, Slider } from "react-native";
import BleManager from 'react-native-ble-manager';
import BLEIDs from './BLEIDs';

export default class VolumeController extends React.Component {
	constructor(props) {
		super(props);

		this.BLEIDs = new BLEIDs();

		this.state = { peripheral: props.peripheral, volume: 0 };
	}

	componentWillReceiveProps(nextProps) {
		if (nextProps.peripheral == null) this.setState(
			{
				peripheral: null,
				volume: 0
			}
		);
		else {
			this.setState({
				peripheral: nextProps.peripheral
			});
			this.readVolumeFromBLE();
		}
	}

	onUpdateVolume(event) {
		console.log("VolumeController: submitted value: " + JSON.stringify(event.value));

		var newVolume = [event.value];

		if (this.state.peripheral) {
			BleManager.write(this.state.peripheral.id, this.BLEIDs.AudioService, this.BLEIDs.AudioVolumeCharacteristic, newVolume)
				.then(() => {
					this.readVolumeFromBLE();
				})
				.catch(error => {
					console.log("VolumeController: " + error);
				});
		}
	}

	readVolumeFromBLE() {
		
		if (this.state.peripheral) {
			BleManager.read(this.state.peripheral.id, this.BLEIDs.AudioService, this.BLEIDs.AudioVolumeCharacteristic)
				.then(readData => {
					console.log("VolumeController Read Volume: " + readData[0]);
					this.setState({ volume: readData[0] });
				})
				.catch(error => {
					// Failure code
					console.log("VolumeController: " + error);
				});
		}
	}

	render() {
		console.log("VolumeController peripheral:" + this.state.peripheral);

		return <View>
			<Text>{this.state.volume}</Text>
			<Slider value={this.state.volume} //onValueChange={(value) => this.onUpdateVolume({value})}
				onSlidingComplete={value => this.onUpdateVolume(
					{ value }
				)} minimumValue={0} maximumValue={100} step={10} />

		</View>;
	}
}
