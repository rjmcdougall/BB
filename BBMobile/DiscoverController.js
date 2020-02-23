import React from "react";
import { View, Text, ScrollView} from "react-native";
import ListView from "deprecated-react-native-listview";
import PropTypes from "prop-types";
import Touchable from "react-native-platform-touchable";
import StyleSheet from "./StyleSheet";
import StateBuilder from "./StateBuilder";

const ds = new ListView.DataSource({
	rowHasChanged: (r1, r2) => r1 !== r2
});

export default class DiscoverController extends React.Component {
	constructor(props) {
		super(props);

	}
	render() {

		try {
			const list = Array.from(this.props.boardBleDevices.values());
			const dataSource = ds.cloneWithRows(list);

			var DC = this;

			return (
				<View style={{ flex: 1, margin: 30 }}>
					<Touchable
						onPress={async () => {
							await this.props.startScan(false);
						}}
						style={StyleSheet.button}
						background={Touchable.Ripple("blue")}>
						<Text style={StyleSheet.connectButtonTextCenter}>Scan for Burner Boards ({this.props.scanning ? "scanning" : "paused"})</Text>
					</Touchable>
					<ScrollView>
						{(list.length == 0) &&
							<Text style={StyleSheet.connectButtonTextCenter}>No Boards Found</Text>
						}
						<ListView
							enableEmptySections={true}
							dataSource={dataSource}
							renderRow={(item) => {

								if (item) {
									if (item.name) {
										try {
 
											var color = StateBuilder.boardColor(item.name, DC.props.boardData);

											return (
												<Touchable
													onPress={async () => {
														try {
															await this.props.onSelectPeripheral(item);
														}
														catch (error) {
															console.log(error);
														}
													}
													}
													style={[StyleSheet.button, { height: 50, backgroundColor: color }]}

													background={Touchable.Ripple("blue")}>
													<Text style={StyleSheet.connectButtonTextCenter}>{item.name}</Text>
												</Touchable>
											);
										}
										catch (error) {
											console.log(error);
										}
									}
								}
							}}
						/>
					</ScrollView>
				</View>
			);
		}
		catch (error) {
			console.log(error);
		}

	}
}

DiscoverController.propTypes = {
	peripherals: PropTypes.object,
	scanning: PropTypes.bool,
	boardData: PropTypes.array,
	onSelectPeripheral: PropTypes.func,
	startScan: PropTypes.func,
	boardBleDevices: PropTypes.object,
};

