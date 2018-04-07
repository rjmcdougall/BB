import React from "react";
import { View, Text, WebView, Button, TouchableHighlight, Slider, StyleSheet } from "react-native";
import BleManager from 'react-native-ble-manager';
//import BleManager from './BLEManagerFake';
import BLEIDs from './BLEIDs';

export default class VolumeController extends React.Component {
	constructor(props) {
		super(props);

		this.BLEIDs = new BLEIDs();

		this.state = { peripheral: props.peripheral, 
						volume: null }; 
	}


	async componentDidMount() {
		console.log("VolumeController " + this.state.mediaType + " DidMount");
		await this.readVolumeFromBLE();;
	}

	componentWillReceiveProps(nextProps) {
		if (nextProps.peripheral == null) this.setState(
			{
				peripheral: null,
				volume: null,
				refreshButtonClicked: false,
			}
		);
		else {
			this.setState({
				peripheral: nextProps.peripheral,
				refreshButtonClicked: false,
			});
		  
			
		}
	}

	async onUpdateVolume(event) {
		console.log("VolumeController: submitted value: " + JSON.stringify(event.value));
		var newVolume = [event.value];

		if (this.state.peripheral) {
			try {
				await BleManager.write(this.state.peripheral.id,
					this.BLEIDs.AudioService,
					this.BLEIDs.AudioVolumeCharacteristic,
					newVolume);

				await this.readVolumeFromBLE();

			}
			catch (error) {
				console.log("VolumeController: " + error);
			}
		}
	}

	async readVolumeFromBLE() {
		if (this.state.peripheral) {
			try {
				var readData = await BleManager.read(this.state.peripheral.id, this.BLEIDs.AudioService, this.BLEIDs.AudioVolumeCharacteristic);
				console.log("VolumeController Read Volume: " + readData[0]);
				this.setState({ volume: readData[0] });
			}
			catch (error){
				console.log("VolumeController: " + error);
			}
		}
	}

	render() {
 
		return (
			<View style={{ margin: 10, backgroundColor: 'skyblue', height: 120 }}>
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
				<Button
				title="Load"
				onPress={async () => {
					if (!this.state.refreshButtonClicked) {
						this.setState({ refreshButtonClicked: true });
						await this.readVolumeFromBLE();
					}
				}
				} />
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