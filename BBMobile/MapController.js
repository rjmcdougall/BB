import React from "react";
import { StyleSheet, Text, View } from "react-native";
import MapView from "react-native-maps";
import PropTypes from "prop-types";
import Touchable from "react-native-platform-touchable";
import StateBuilder from "./StateBuilder";

export default class MapController extends React.Component {
	constructor(props) {
		super(props);
		this.state = {
			autoZoom: false,
			wifiLocations: false,
		};
	}

	render() {


		try {
			var locations = StateBuilder.getLocations(this.props.mediaState, this.state.wifiLocations);
			var region;

			if (this.state.autoZoom == true)
				region = this.props.mediaState.region;
			else
				region = null;

			return (
				<View style={styles.container}>
					<View style={styles.mapView}>
						<MapView
							style={styles.map}
							region={region}
						>
							{locations.map(marker => {
								return (
									<MapView.Marker
										key={marker.title}
										coordinate={{
											latitude: marker.latitude,
											longitude: marker.longitude
										}}
										title={marker.title}
									/>
								);
							})}
						</MapView>

						<Touchable
							onPress={() => {
								if (this.state.autoZoom == true)
									this.setState({
										autoZoom: false
									});
								else
									this.setState({
										autoZoom: true
									});
							}
							}
							style={[styles.container, { backgroundColor: this.state.autoZoom ? "green" : "lightblue" }]}
							background={Touchable.Ripple("blue")}>
							<Text style={styles.rowText}>Auto Zoom</Text>
						</Touchable>
						<Touchable
							onPress={async () => {
								if (this.state.wifiLocations == true){
									this.setState({
										wifiLocations: false
									});
								}
								else{
									await this.props.onLoadAPILocations();
									this.setState({
										wifiLocations: true
									});
								}
							}
							}
							style={[styles.container, { backgroundColor: this.state.wifiLocations ? "green" : "lightblue" }]}
							background={Touchable.Ripple("blue")}>
							<Text style={styles.rowText}>Wifi Locations</Text>
						</Touchable>
					</View>
				</View>
			);
		}
		catch (error) {
			console.log("Error:" + error);
		}

	}
}

MapController.propTypes = {
	mediaState: PropTypes.object,
	onLoadAPILocations: PropTypes.func,
};

const styles = StyleSheet.create({
	map: {
		height: 200,
	},
	container: {
		flex: 1,
		backgroundColor: "#FFF",
	},
	rowText: {
		fontSize: 14,
		textAlign: "center",
		padding: 10,
	},
	mapView: {
		padding: 10,
	},
});
