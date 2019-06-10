import React, { Component } from "react";
import { Text, View } from "react-native";
import Mapbox from "@react-native-mapbox-gl/maps";
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
		// try {
		// 	var center = await this._map.getCenter();
		// 	var zoom = await this._map.getZoom();

		// 	this.props.setMap({
		// 		center: center,
		// 		zoom: zoom,
		// 		userLocation: this.props.map.userLocation
		// 	});
		// }
		// catch (error) {
		// 	console.log(error);
		// }
	}

	onUserLocationUpdate(location) {
		//this.props.setMap({
		// 	center: this.props.map.center,
		// 	zoom: this.props.map.zoom,
		// 	bounds: this.props.map.bounds,
		// 	userLocation: [location.coords.longitude, location.coords.latitude]
		// });
	}


	getMostRecent(board) {
		var locations = board.locations.sort((a, b) => {
			if (a.lastHeardDate < b.lastHeardDate) {
				return -1;
			}
			if (a.lastHeardDate > b.lastHeardDate) {
				return 1;
			}
			return 0;
		});
		return [locations[locations.length - 1].longitude, locations[locations.length - 1].latitude];

	}
	makeFeatureCollection(board) {

		var featureCollection = {
			'type': 'FeatureCollection',
			'features': []
		}
		var route = {
			"type": "Feature",
			"geometry": {
				"type": "LineString",
				"coordinates": []
			}
		}

		board.locations.map((location) => {
			//	route.geometry.coordinates.push([location.longitude + (Math.random() * .01), location.latitude + (Math.random() * .01)]);
			route.geometry.coordinates.push([location.longitude, location.latitude]);
		})

		featureCollection.features.push(route)
		return featureCollection;
	}

	buildMap() {
		var a = new Array();

		this.props.mediaState.locations.map((board) => {
			var recentLocation = this.getMostRecent(board);

			var shapeSource = (
				<Mapbox.ShapeSource id={"SS" + board.board} key={"SS" + board.board} shape={this.makeFeatureCollection(board)}>
					<Mapbox.LineLayer id={"LL" + board.board} key={"LL" + board.board} style={{
						lineColor: StateBuilder.boardColor(board.board, this.props.boardData),
						lineWidth: 5,
						lineOpacity: 1,
						lineJoin: "round",
						lineCap: "round",
					}} />
				</Mapbox.ShapeSource>);
			a.push(shapeSource);
			// var annotationSource = (
			// 	<Mapbox.PointAnnotation
			// 		key={board.board + "anno"}
			// 		id={board.board + "anno"}
			// 		title={board.board + "1"}
			// 		coordinate={recentLocation}>
			// 		<View style={StyleSheet.annotationContainer}>
			// 			<View style={[StyleSheet.annotationFill, { backgroundColor: StateBuilder.boardColor(board.board, this.props.boardData) }]} />
			// 		</View>
			// 		<Mapbox.Callout title={board.board + "2"} />
			// 	</Mapbox.PointAnnotation>
			// );
			// a.push(annotationSource);
		});
		return a;
	}

	render() {

		var MP = this;

		return (
			<View style={StyleSheet.container}>
				<Mapbox.MapView
					styleURL={Mapbox.StyleURL.Street}
					ref={c => (this._map = c)}
					onRegionDidChange={this.onRegionDidChange}
					style={StyleSheet.container}>
					<Mapbox.Camera
						zoomLevel={this.props.map.zoom}
						animationMode={'flyTo'}
						animationDuration={3000}
						centerCoordinate={this.props.map.center}
					/>
					<Mapbox.UserLocation />
					{this.buildMap()}
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
									this.props.setMap({
										center: MP.props.map.userLocation,
										zoom: MP.props.map.zoom,
										userLocation: MP.props.map.userLocation
									});  
									this.setState({ meButtonColor: "skyblue" });
								}
								catch (error) {
									console.log(error);
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
									if (locations.length > 0){
										this.props.setMap({
											center: StateBuilder.getBoundsForCoordinates(locations),
											zoom: this.props.map.zoom,
											userLocation: this.props.map.userLocation
										}); 
									} 
									this.setState({ boardsButtonColor: "skyblue" });
								}
								catch (error) {
									console.log(error);
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
									this.props.setMap({
										center: Constants.MAN_LOCATION,
										zoom: this.props.map.zoom,
										userLocation: this.props.map.userLocation
									}); 
									this.setState({ manButtonColor: "skyblue" });
								}
								catch (error) {
									console.log(error);
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