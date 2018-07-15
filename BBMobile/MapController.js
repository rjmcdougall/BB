import React from "react";
import { Text, View } from "react-native";
import MapView from "react-native-maps";
import PropTypes from "prop-types";
import Touchable from "react-native-platform-touchable";
import StateBuilder from "./StateBuilder";
import StyleSheet from "./StyleSheet";

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
				<View style={StyleSheet.mapView}>
					<MapView style={StyleSheet.map} region={region} >
						{locations.map(marker => {

							var ONE_HOUR = 60 * 60 * 1000; /* ms */
							var pinColor;

							if (((new Date()) - new Date(marker.dateTime)) > ONE_HOUR)
								pinColor = "red";

							else
								pinColor = "blue";

							return (
								<MapView.Marker
									key={marker.title}
									coordinate={{
										latitude: marker.latitude,
										longitude: marker.longitude
									}}
									title={marker.title}
									pinColor={pinColor}
								/>
							);
						})}
					</MapView>
					<View style={StyleSheet.button}>
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
							style={[{ backgroundColor: this.state.autoZoom ? "green" : "skyblue" }]}
							background={Touchable.Ripple("blue")}>
							<Text style={StyleSheet.buttonTextCenter}>Auto Zoom</Text>
						</Touchable>
					</View>
					<View style={StyleSheet.button}>
						<Touchable
							onPress={async () => {
								if (this.state.wifiLocations == true) {
									this.setState({
										wifiLocations: false
									});
								}
								else {
									await this.props.onLoadAPILocations();
									this.setState({
										wifiLocations: true
									});
								}
							}
							}
							style={[{ backgroundColor: this.state.wifiLocations ? "green" : "skyblue" }]}
							background={Touchable.Ripple("blue")}>
							<Text style={StyleSheet.buttonTextCenter}>Wifi Locations</Text>
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

