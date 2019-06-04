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
			flyTo: 0,
			meButtonColor: "skyblue",
			boardsButtonColor: "skyblue",
			manButtonColor: "skyblue",
		}
		this.onRegionDidChange = this.onRegionDidChange.bind(this);
		this.onUserLocationUpdate = this.onUserLocationUpdate.bind(this);
	}
 
	async onRegionDidChange() {
		try {
			var center = await this._map.getCenter();
			var zoom = await this._map.getZoom();

			this.props.setMap({
				center: center,
				zoom: zoom,
				userLocation: this.props.map.userLocation
			});
		}
		catch (error) {
			console.log(error);
		}
	}

	onUserLocationUpdate(location) {
		this.props.setMap({
			center: this.props.map.center,
			zoom: this.props.map.zoom,
			bounds: this.props.map.bounds,
			userLocation: [location.coords.longitude, location.coords.latitude]
		});
	}

	render() {

		var locations = new Array();
		locations = StateBuilder.getLocations(this.props.mediaState, this.props.userPrefs.wifiLocations);

		var MP = this;
		// todo you should be able to see the board colors even if you are not connected. look in the board cache.
		return (
			<View style={StyleSheet.container}>
				<Mapbox.MapView
					showUserLocation={true}
					onUserLocationUpdate={this.onUserLocationUpdate}
					userTrackingMode={Mapbox.UserTrackingModes.FollowWithHeading}
					styleURL={Mapbox.StyleURL.Street}
					zoomLevel={this.props.map.zoom}
					centerCoordinate={this.props.map.center}
					onRegionDidChange={this.onRegionDidChange}
					ref={c => (this._map = c)}
					style={StyleSheet.container}>
					{locations.map(marker => {
						var bgColor = StateBuilder.boardColor(marker.board, MP.props.boardData);
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
				<View style={StyleSheet.horizontalButtonBar}>
					<View style={StyleSheet.horizonralButton}>
						<Touchable
							onPress={async () => {
								try {
									this.setState({
										meButtonColor: "green",
										boardsButtonColor: "skyblue",
										manButtonColor: "skyblue",
									});
									await this._map.flyTo(this.props.map.userLocation, 3000);
									this.setState({ meButtonColor: "skyblue" });
								}
								catch (error) {
									console, log(error);
								}
							}}
							style={[{ backgroundColor: this.state.meButtonColor }]}
							background={Touchable.Ripple("blue")}>
							<Text style={StyleSheet.buttonTextCenter}>Me</Text>
						</Touchable>
					</View>
					<View style={StyleSheet.horizonralButton}>
						<Touchable
							onPress={async () => {
								try {
									this.setState({
										meButtonColor: "skyblue",
										boardsButtonColor: "green",
										manButtonColor: "skyblue",
									});
									var locations = new Array();
									locations = StateBuilder.getLocations(this.props.mediaState, this.props.userPrefs.wifiLocations);
									if (locations.length > 0)
										await this._map.flyTo(StateBuilder.getBoundsForCoordinates(locations), 3000);
									this.setState({ boardsButtonColor: "skyblue" });
								}
								catch (error) {
									console, log(error);
								}
							}}
							style={[{ backgroundColor: this.state.boardsButtonColor }]}
							background={Touchable.Ripple("blue")}>
							<Text style={StyleSheet.buttonTextCenter}>Boards</Text>
						</Touchable>
					</View>
					<View style={StyleSheet.horizonralButton}>
						<Touchable
							onPress={async () => {
								try {
									this.setState({
										meButtonColor: "skyblue",
										boardsButtonColor: "skyblue",
										manButtonColor: "green",
									});
									await this._map.flyTo(Constants.MAN_LOCATION, 3000);
									this.setState({ manButtonColor: "skyblue" });
								}
								catch (error) {
									console, log(error);
								}
							}}
							style={[{ backgroundColor: this.state.manButtonColor }]}
							background={Touchable.Ripple("blue")}>
							<Text style={StyleSheet.buttonTextCenter}>Playa</Text>
						</Touchable>
					</View>
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
	boardData: PropTypes.any,
};