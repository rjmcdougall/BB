import React, { Component } from "react";
import { Text, View } from "react-native";
import Mapbox from "@mapbox/react-native-mapbox-gl";
import StateBuilder from "./StateBuilder";
import PropTypes from "prop-types";
import Touchable from "react-native-platform-touchable";
import StyleSheet from "./StyleSheet";

Mapbox.setAccessToken(
	"sk.eyJ1IjoiZGFuaWVsa2VpdGh3IiwiYSI6ImNqdzhlbHUwZTJvdmUzenFramFmMTQ4bXIifQ.9EXJnBcsrsKyS-veb_dlNg"
);
export default class MapController extends Component {

	constructor(props) {
		super(props);
		this.state = {
			autoZoom: false,
		};
	}

	render() {

		var locations = new Array();
		var bound;

		locations = StateBuilder.getLocations(this.props.mediaState, this.props.userPrefs.wifiLocations);

		if (this.props.userPrefs.includeMeOnMap)
			locations = [...locations, this.props.mediaState.phoneLocation];


		if (this.state.autoZoom == true && (locations.length > 0))
			bound = StateBuilder.getBoundsForCoordinates(locations);
		else
			bound = StateBuilder.getBoundsForCoordinates([this.props.mediaState.phoneLocation]);

		return (
			<View style={StyleSheet.container}>
				<Mapbox.MapView
					styleURL={Mapbox.StyleURL.Street}
					zoomLevel={15}
					style={StyleSheet.container}
					visibleCoordinateBounds={bound}>
					{locations.map(marker => {
						return (
							<Mapbox.PointAnnotation
								key={marker.board}
								id={marker.board}
								title={marker.board}
								coordinate={[marker.longitude, marker.latitude]}>
								<View style={StyleSheet.annotationContainer}>
									<View style={StyleSheet.annotationFill} />
								</View>
								<Mapbox.Callout title={marker.board} />
							</Mapbox.PointAnnotation>
						);
					})
					}
				</Mapbox.MapView>
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
			</View>
		);
	}
}

MapController.propTypes = {
	mediaState: PropTypes.object,
	userPrefs: PropTypes.object,
};