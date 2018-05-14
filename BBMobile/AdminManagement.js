import React, {
	Component
} from "react";
import {
	StyleSheet,
	View,
	ScrollView,
	Text,
} from "react-native";

import TrackController from "./TrackController";
import BLEBoardData from "./BLEBoardData";
import Touchable from "react-native-platform-touchable";
import PropTypes from "prop-types";

export default class AdminManagement extends Component {
	constructor(props) {
		super(props);

		this.state = {
			mediaState: BLEBoardData.emptyMediaState,
		};

		this.onSelectDevice = this.onSelectDevice.bind(this);

	}

	async onSelectDevice(idx) {
		this.setState({ mediaState: await BLEBoardData.setTrack(this.state.mediaState, "Device", idx) });
		console.log("Media Management: Set Media State After Update.");
	}

	async OLD_componentWillReceiveProps(nextProps) {

		if (nextProps.mediaState) {
			if (this.state.mediaState.peripheral.id != nextProps.mediaState.peripheral.id) {
				this.setState({
					mediaState: nextProps.mediaState,
				});
			}
		}
		else
			console.log("MediaManagement: Null NextProps");
	}

	render() {

		const { navigate } = this.props.navigation;

		return (


			<View style={styles.container} pointerEvents={this.props.pointerEvents}>
				<View style={styles.contentContainer}>

					<ScrollView style={styles.scroll}>
						<TrackController onSelectTrack={this.onSelectDevice} mediaState={this.state.mediaState} mediaType="Device" />
					</ScrollView>

					<View style={styles.footer}>
						<View style={styles.button}>
							<Touchable
								onPress={async () => {
									// try {
									// 	await BleManager.disconnect(this.state.selectedPeripheral.id);
									// }
									// catch (error) {
									// 	console.log("BoardManager: Pressed BBcom: " + error);
									// }

									if (this.state.backgroundLoop)
										clearInterval(this.state.backgroundLoop);

									this.props.navigation.setParams({ title: "Search For Boards" });

									this.setState({
										peripherals: new Map(),
										appState: "",
										selectedPeripheral: BLEBoardData.emptyMediaState.peripheral,
										mediaState: BLEBoardData.emptyMediaState,
										showDiscoverScreen: true,
										showAdminScreen: false,
										discoveryState: "Connect To Board",
										backgroundLoop: null,
									});

									navigate("BBCom");

								}}
								style={styles.touchableStyle}
								background={Touchable.Ripple("blue")}>
								<Text style={styles.rowText}>Go To BB.Com</Text>
							</Touchable>
						</View>
					</View>
				</View>
			</View>
		);
	}
}
AdminManagement.propTypes = {
	mediaState: PropTypes.object,
	locationState: PropTypes.object,
	pointerEvents: PropTypes.string,
	navigation: PropTypes.object,
};

const styles = StyleSheet.create({
	container: {
		flex: 1,
		backgroundColor: "#FFF",
	},
	contentContainer: {
		flex: 1 // pushes the footer to the end of the screen
	},
	rowText: {
		margin: 5,
		fontSize: 14,
		textAlign: "center",
		padding: 10,
	},
	touchableStyle: {
		backgroundColor: "lightblue",
		margin: 5,
		height: 50,
	},
	footer: {
		height: 50,
		flexDirection: "row",
		justifyContent: "space-between"
	},
	button: {
		width: "100%",
		height: 50
	}
});