import React, { Component } from "react";
import { Text, View } from "react-native";
import Mapbox from "@mapbox/react-native-mapbox-gl";
import StateBuilder from "./StateBuilder";
import PropTypes from "prop-types";
import Touchable from "react-native-platform-touchable";
import StyleSheet from "./StyleSheet";
import Constants from "./Constants";

Mapbox.setAccessToken(
	"sk.eyJ1IjoiZGFuaWVsa2VpdGh3IiwiYSI6ImNqdzhlbHUwZTJvdmUzenFramFmMTQ4bXIifQ.9EXJnBcsrsKyS-veb_dlNg"
);
export default class MapController extends Component {

	constructor(props) {
		super(props);
		this.state = {
			autoZoom: false, 
		};

		this.onRegionDidChange = this.onRegionDidChange.bind(this);


	}
 
	async onRegionDidChange() {
		try{
			var center = await this._map.getCenter();
			var zoom = await this._map.getZoom();
			var bounds = await this._map.getVisibleBounds();
	
			console.log("change center" + center[0] + " " + center[1])
	
			this.props.setMap({
				center: center,
				zoom: zoom,
				bounds: bounds
			});
		}
		catch (error) {
			console.log(error);
		}
	}

	render() {
	
		var locations = new Array();
		// var bound;

		locations = StateBuilder.getLocations(this.props.mediaState, this.props.userPrefs.wifiLocations);
 
		// //		var boundsArray = Constants.PLAYA_BOUNDS();
		// if (this.state.autoZoom == true && (locations.length > 0))
		// 	bound = StateBuilder.getBoundsForCoordinates(locations);
		// else
		// 	bound = StateBuilder.getBoundsForCoordinates([this.props.mediaState.phoneLocation]);

		// MP = this;

		return (
			<View style={StyleSheet.container}>
				<Mapbox.MapView
					showUserLocation={true}
					userTrackingMode={Mapbox.UserTrackingModes.FollowWithHeading}
					styleURL={Mapbox.StyleURL.Street}
					zoomLevel={this.props.map.zoom}
					centerCoordinate={this.props.map.center}
					onRegionDidChange={this.onRegionDidChange}
					bounds={this.props.map.bounds}
					ref={c => (this._map = c)}
					style={StyleSheet.container}>
					{locations.map(marker => {
						var bgColor = StateBuilder.boardColor(marker.board, MP.props.mediaState.boards);
						return (
							<Mapbox.PointAnnotation
								key={marker.board}
								id={marker.board}
								title={marker.board}
								coordinate={[marker.longitude, marker.latitude]}>
								<View style={StyleSheet.annotationContainer}>
									<View style={[StyleSheet.annotationFill, { backgroundColor: bgColor }]} />
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
	setMapCenter: PropTypes.func,
	map: PropTypes.object,
};