import React from "react";
import { View, Text, ScrollView, ListView } from "react-native";
import PropTypes from "prop-types";
import Touchable from "react-native-platform-touchable";
import StyleSheet from "./StyleSheet";
import { Client } from 'bugsnag-react-native';
const bugsnag = new Client("905bfbccb8f9a7e3749038ca1900b1b4");

const ds = new ListView.DataSource({
	rowHasChanged: (r1, r2) => r1 !== r2
});

export default class DiscoverController extends React.Component {
	constructor(props) {
		super(props);

	}
	render() {

		try {
			const list = Array.from(this.props.peripherals.values());
			const dataSource = ds.cloneWithRows(list);

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

								console.log("item name" + item.name);
								console.log(this.props.boardData)

								if (item) {
									if (item.name) {
										try {
											var foundBoard = this.props.boardData.filter((board) => {
												if(board.name)
													return board.name == item.name;
												else if (board.bootName)
													return board.bootName == item.name;
												else
													return false;
											});

											var color = "whitesmoke";
											if (foundBoard[0]) {
												if (foundBoard[0].color) {
													color = foundBoard[0].color;
												}
											}

											return (
												<Touchable
													onPress={async () => await this.props.onSelectPeripheral(item)}
													style={[StyleSheet.button, { height: 50, backgroundColor: color }]}

													background={Touchable.Ripple("blue")}>
													<Text style={StyleSheet.connectButtonTextCenter}>{item.name}</Text>
												</Touchable>
											);
										}
										catch (error) {
											bugsnag.notify(error);
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
			bugsnag.notify(error);
		}

	}
}

DiscoverController.propTypes = {
	peripherals: PropTypes.object,
	scanning: PropTypes.bool,
	boardData: PropTypes.array,
	onSelectPeripheral: PropTypes.func,
	startScan: PropTypes.func,
};

