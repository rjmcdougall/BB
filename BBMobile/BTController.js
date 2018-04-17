import React, { Component } from "react";
import { View, Text, WebView, Button, TouchableHighlight, Slider, StyleSheet } from "react-native";
import ModalDropdown from 'react-native-modal-dropdown';
import { bin } from 'charenc';
import BleManager from 'react-native-ble-manager';
import BLEIDs from './BLEIDs';

export default class BTController extends Component {
	constructor(props) {
		super(props);

//		this.BLEIDs = new BLEIDs();

		this.state = {
			peripheral: props.peripheral,
			mediaType: props.mediaType,
			deviceNo: 0,
			deviceInfo: "Please Load",
			maxDevice: 0,
			devices: [{ deviceInfo: "loading..." }],
			service: BLEIDs.BTDeviceService,
			deviceCharacteristic: BLEIDs.BTDeviceSelectCharacteristic,
			infoCharacteristic: BLEIDs.BTDeviceInfoCharacteristic,
			refreshButtonClicked: false,
		};
	}

	componentWillReceiveProps(nextProps) {

		console.log("BTController " + this.state.mediaType + " component received props:" + nextProps);

		if (nextProps.peripheral == null) {
			console.log("BTController " + this.state.mediaType + " clearing state...");
			this.setState({
				peripheral: null,
				devices: [{ deviceInfo: "Please Select..." }],
				deviceNo: 0,
				maxDevice: 0,
			});
		} else {
			console.log("BTController " + this.state.mediaType + " setting state...");
			this.setState({
				peripheral: nextProps.peripheral,
			});
		}
	}

	async componentDidMount() {
		console.log("BTController " + this.state.mediaType + " DidMount");
		await this.refresh();
	}

	async readDeviceFromBLE(peripheral) {
		if (peripheral) {
			try {
				console.log("BTController " + this.state.mediaType + " doing Read current/track counts:" + peripheral);
				var readData = await BleManager.read(peripheral.id,
					this.state.service,
					this.state.deviceCharacteristic);

				this.setState({
					deviceNo: readData[1],
					maxDevice: readData[0]
				});
				console.log("BTController " + this.state.mediaType + " Selected Device No: " + this.state.deviceNo);
				console.log("BTController " + this.state.mediaType + " Max Device: " + this.state.maxDevice);

			}
			catch (error) {
				console.log("BTController " + this.state.mediaType + " read device: " + error);
			}
		}
	}

	async refresh() {

		console.log("BTController " + this.state.mediaType + " Read Listing:" + this.state.peripheral);
		try {
			await BleManager.write(this.state.peripheral.id,
				this.state.service,
				this.state.infoCharacteristic,
				[1]);

			console.log("BTController " + this.state.mediaType + " request scan  ");
		}
		catch (error) {
			console.log("BTController " + this.state.mediaType + " " + error);
		}
		await this.readDeviceFromBLE(this.state.peripheral);
		var devices = [];

		try {
			if (this.state.peripheral) {
				for (var n = 1; n <= this.state.maxDevice; n++) {

					var readData = await BleManager.read(this.state.peripheral.id, this.state.service, this.state.infoCharacteristic);
   					var deviceInfo = "";
                    if (readData.length > 3) {
    					var deviceNo = readData[0];
    					var deviceMax = readData[1];
    					var deviceStatus = readData[2];
    					for (var i = 3; i < readData.length; i++) {
    						deviceInfo += String.fromCharCode(readData[i]);
    					}
                        if (deviceStatus == 80) {
                            var deviceLabel = deviceInfo + " (Paired)";
                            var isPaired = true;
                        } else {
                            var deviceLabel = deviceInfo;
                            var isPaired = false;
                        }
                    }
					if (deviceInfo && 0 != deviceInfo.length) {
						devices[deviceNo] = { deviceNo: deviceNo, deviceInfo: deviceInfo, deviceLabel: deviceLabel, isPaired: isPaired };
						if (deviceNo == this.state.deviceNo)
							this.setState({ deviceInfo: devices[deviceNo].deviceInfo });

						console.log("BTController " + this.state.mediaType + " Add Info device: " + deviceNo + " of " + deviceMax + ", name = " + deviceLabel);
					}
				}
				console.log("BTController " + this.state.mediaType + " found devices: " + JSON.stringify(devices));
				this.setState({
					devices: devices,
                    //maxDevice: deviceMax
				});
			}
		}
		catch (error) {
			console.log("BTController " + this.state.mediaType + " r2: " + error);
		}
	}

	async setDevice(idx) {

		var deviceAddress = this.state.devices[idx].deviceInfo
		console.log("BTController " + this.state.mediaType + " submitted value: " + deviceAddress);
		if (this.state.peripheral) {

			try {
				await BleManager.write(this.state.peripheral.id,
					this.state.service,
					this.state.deviceCharacteristic,
					bin.stringToBytes(deviceAddress));

				console.log("BTController " + this.state.mediaType + " Update:  " + deviceAddress);
				await this.readDeviceFromBLE(this.state.peripheral);
			}
			catch (error) {
				console.log("BTController " + this.state.mediaType + " " + error);
			}
		}
	}

	onSelect(idx) {
		console.log("BTController " + this.state.mediaType + " Selected device: " + this.state.devices[idx].deviceInfo);
		this.setDevice(idx);
	}

	render() {

		var devices = this.state.devices.map(a => a.deviceLabel);
		return (

			<View style={{ margin: 10, backgroundColor: 'skyblue', height: 120 }}>
				<View style={{
					flex: 1,
					flexDirection: 'row',
				}}>
					<View style={{ height: 50 }}>
						<Text style={styles.rowText}>{this.state.mediaType} Device</Text></View>
				</View>
				<View style={{ height: 50 }}>
					<ModalDropdown options={devices}
						defaultValue={this.state.deviceInfo}
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
					//if (!this.state.refreshButtonClicked) {
				//		this.setState({ refreshButtonClicked: true });
						await this.refresh();
				//	}
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
