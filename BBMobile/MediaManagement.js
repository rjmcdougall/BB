import React, {
	Component
} from "react";
import { 
	StyleSheet,
	Text,
	View,  
	Dimensions,
	ScrollView,
} from "react-native"; 
import VolumeController from "./VolumeController";
import TrackController from "./TrackController"; 
import BatteryController from "./BatteryController"; 
const window = Dimensions.get("window");
 
export default class MediaManagement extends Component {
	constructor(props) {
		super(props);

		const { params } = this.props.navigation.state;
		const peripheral = params.peripheral;

		this.state = {
			peripheral: peripheral,
		};

	}

	static navigationOptions = {
		title: "Media Management",
	};

	render() {

		var boardConnected;

		if (this.state.peripheral.connected) {
			boardConnected = (
				<Text style={[styles.rowText, { backgroundColor: "#fff" }]}>Connected to {this.state.peripheral.name}</Text>
			);
		} else {
			boardConnected = (
				<Text style={[styles.rowText, { backgroundColor: "#ff0000" }]}>NOT connected to {this.state.peripheral.name}</Text>

			);
		}

		return (
			<View style={styles.container}>
				<ScrollView style={styles.scroll}>
					<BatteryController peripheral={this.state.peripheral} />
					<VolumeController peripheral={this.state.peripheral} />
					<TrackController peripheral={this.state.peripheral} mediaType="Audio" />
					<TrackController peripheral={this.state.peripheral} mediaType="Video" />
					{boardConnected}
				</ScrollView>
			</View>
		);
	}

}

const styles = StyleSheet.create({
	container: {
		flex: 1,
		backgroundColor: "#FFF",
		width: window.width,
		height: window.height
	},  
	rowText: {
		margin: 5,
		fontSize: 12,
		textAlign: "center",
		padding: 10,
	},
});
