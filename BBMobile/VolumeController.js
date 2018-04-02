import React from "react";
import { View, Text, WebView, Button, TouchableHighlight, Slider, StyleSheet } from "react-native";
import BleManager from 'react-native-ble-manager';
//import BleManager from './BLEManagerFake';
import BLEIDs from './BLEIDs';

export default class VolumeController extends React.Component {
	constructor(props) {
		super(props);

		this.BLEIDs = new BLEIDs();

		this.state = { peripheral: props.peripheral, volume: null };
		this.readVolumeFromBLE = this.readVolumeFromBLE.bind(this);
 
	}

	componentWillReceiveProps(nextProps) {
		if (nextProps.peripheral == null) this.setState(
			{
				peripheral: null,
				volume: null,
			}
		);
		else {
			this.setState({
				peripheral: nextProps.peripheral
			});
		  
			
		}
	}

	onUpdateVolume(event) {
		console.log("VolumeController: submitted value: " + JSON.stringify(event.value));

		var newVolume = [event.value];

		if (this.state.peripheral) {
			BleManager.write(this.state.peripheral.id,
				this.BLEIDs.AudioService,
				this.BLEIDs.AudioVolumeCharacteristic,
				newVolume)
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

		if(!this.state.volume){
			this.readVolumeFromBLE();
		}
		
		return (
			<View style={{ margin: 10, backgroundColor: 'skyblue', height: 100 }}>
				<View style={{
					flex: 1,
					flexDirection: 'row',
					justifyContent: 'space-between',

				}}>
					<View style={{ height: 50 }}><Text style={styles.rowText}>Volume</Text></View>
					<View style={{ height: 50 }}><Text style={styles.rowText}>{this.state.volume}</Text></View>
				</View>
				<View style={{ height: 50 }}><Slider value={this.state.volume}
					onSlidingComplete={value => this.onUpdateVolume(
						{ value }
					)} minimumValue={0} maximumValue={100} step={10} />
				</View>
			</View>
		);
	}
}
const styles = StyleSheet.create({
	rowText: {
		margin: 5,
		fontSize: 16,
		padding: 10,
	},
});