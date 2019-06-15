import React, { Component } from "react";
import { Text, View } from "react-native";
import Mapbox from "@react-native-mapbox-gl/maps";
import StateBuilder from "./StateBuilder";
import PropTypes from "prop-types";
import Touchable from "react-native-platform-touchable";
import StyleSheet from "./StyleSheet";
import Constants from "./Constants";
import Bubble from './Bubble';

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
			showBubble: false,
		}
 
		this.onPressCircle = this.onPressCircle.bind(this);
		this.onCloseBubble = this.onCloseBubble.bind(this);

		//	this.onRegionDidChange = this.onRegionDidChange.bind(this);
		//	this.onUserLocationUpdate = this.onUserLocationUpdate.bind(this);
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

	// onUserLocationUpdate(location) {
	// 	this.props.setMap({
	// 		center: this.props.map.center,
	// 		zoom: this.props.map.zoom,
	// 		bounds: this.props.map.bounds,
	// 		userLocation: [location.coords.longitude, location.coords.latitude]
	// 	});
	// }

	makeLineCollection(board) {

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

		//locations.locations :) ascending order
		var locationHistory = board.locations.sort((a, b) => a.lastHeardDate - b.lastHeardDate);

		locationHistory.map((location) => {
			route.geometry.coordinates.push([location.longitude + (Math.random() * .01), location.latitude + (Math.random() * .01)]);
			//route.geometry.coordinates.push([location.longitude, location.latitude]);
		})

		featureCollection.features.push(route)
		return featureCollection;
	}

	makePoint(board) {

		//locations.locations :) ascending order
		var locationHistory = board.locations.sort((a, b) => a.lastHeardDate - b.lastHeardDate);
		var lastLocation = locationHistory[locationHistory.length - 1];

		var featureCollection = {
			'type': 'FeatureCollection',
			'features': []
		}
		var point = {
			"type": "Feature",
			"geometry": {
				"type": "Point",
				"coordinates": [lastLocation.longitude, lastLocation.latitude]
			},
			"properties": {
				"title": board.board,
			}
		}

		featureCollection.features.push(point);
		return featureCollection;
	}

	async sleep(ms) {
		await this._sleep(ms);
	}

	_sleep(ms) {
		return new Promise((resolve) => setTimeout(resolve, ms));
	}

	async onPressCircle(e) {

		if (e.nativeEvent.payload.geometry.type != "Point")
			return;

		this.setState({
			bubbleText: e.nativeEvent.payload.properties.title,
			showBubble: true
		})
		await this.sleep(3000);
		this.setState({
			bubbleText: "",
			showBubble: false
		})	
	}
 
	buildMap() {
		var a = new Array();
		var MP = this;
		this.props.mediaState.locations.map((board) => {

			var shapeSource = (
				<Mapbox.ShapeSource id={"SS" + board.board} key={"SS" + board.board} shape={this.makeLineCollection(board)}>
					<Mapbox.LineLayer id={"LL" + board.board} key={"LL" + board.board} style={{
						lineColor: StateBuilder.boardColor(board.board, this.props.boardData),
						lineWidth: 5,
						lineOpacity: 1,
						lineJoin: "round",
						lineCap: "round",
					}} />
				</Mapbox.ShapeSource>);

			a.push(shapeSource);

			if (board.locations.length > 0) {

				var shapeSource = (
					<Mapbox.ShapeSource id={"C" + board.board} key={"C" + board.board}
						shape={this.makePoint(board)}
						onPress={MP.onPressCircle}>
						<Mapbox.CircleLayer id={"CL" + board.board} key={"CL" + board.board}
							style={{
								circleRadius: 8,
								circleColor: StateBuilder.boardColor(board.board, this.props.boardData),
							}} />
					</Mapbox.ShapeSource>);

				a.push(shapeSource);
			}
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
					style={StyleSheet.container}
				>
					<Mapbox.Camera
						zoomLevel={MP.props.map.zoom}
						animationMode={'flyTo'}
						animationDuration={3000}
						centerCoordinate={MP.props.map.center}
					/>
					<Mapbox.UserLocation />
					{this.buildMap()}
				</Mapbox.MapView>

				{(this.state.showBubble) ? (
					<Bubble>
						<Text>{this.state.bubbleText}</Text>
					</Bubble>
				) : <View />}

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
										center: [-122.4194, 37.7749], // hack until i figure out user location
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
									if (locations.length > 0) {
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
	setMap: PropTypes.func,
	map: PropTypes.object,
	boardData: PropTypes.any,
};
