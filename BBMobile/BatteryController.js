import React from "react";
import { View, Text,  Button, StyleSheet } from "react-native";
import BleManager from "react-native-ble-manager";
import BLEIDs from "./BLEIDs";
import PropTypes from "prop-types";

export default class BatteryController extends React.Component {
	constructor(props) {
		super(props);

		this.BLEIDs = new BLEIDs();

		this.state = {
			peripheral: props.peripheral,
			battery: null
		};
	}


	async componentDidMount() {
		console.log("BatteryController " + this.state.mediaType + " DidMount");
		await this.readBatteryFromBLE();
	}

	componentWillReceiveProps(nextProps) {
		if (nextProps.peripheral == null) this.setState(
			{
				peripheral: null,
				battery: null,
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

	async readBatteryFromBLE() {
		if (this.state.peripheral) {
			try {
				var readData = await BleManager.read(this.state.peripheral.id, this.BLEIDs.BatteryService, this.BLEIDs.BatteryCharacteristic);
				console.log("BatteryController Read Battery: " + readData[0]);
				this.setState({ battery: readData[0] });
			}
			catch (error) {
				console.log("BatteryController: " + error);
			}
		}
	}

	render() {

		return (
			<View style={{ margin: 10, backgroundColor: "skyblue", height: 100 }}>
				<View style={{
					flex: 1,
					flexDirection: "row",
					justifyContent: "space-between",

				}}>
					<View style={{ height: 50 }}><Text style={styles.rowText}>Battery</Text></View>
					<View style={{ height: 50 }}><Text style={styles.rowText}>{this.state.battery}</Text></View>
				</View>
				<Button
					title="Load"
					onPress={async () => {
						if (!this.state.refreshButtonClicked) {
							this.setState({ refreshButtonClicked: true });
							await this.readBatteryFromBLE();
							this.setState({ refreshButtonClicked: false });
						}
					}
					} />
			</View>
		);
	}
}

BatteryController.propTypes = {
	peripheral: PropTypes.object,
};

const styles = StyleSheet.create({
	rowText: {
		margin: 5,
		fontSize: 16,
		padding: 10,
	},
});