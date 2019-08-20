import React, { Component } from "react";
import { Text, View } from "react-native";
import Mapbox from "@react-native-mapbox-gl/maps";
import StateBuilder from "./StateBuilder";
import PropTypes from "prop-types";
import Touchable from "react-native-platform-touchable";
import StyleSheet from "./StyleSheet";
import Constants from "./Constants";
import Bubble from "./Bubble";

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
			followUserLocation: false,
			isFetchingAndroidPermission: Constants.IS_ANDROID,
			isAndroidPermissionGranted: false,
		};

		this.onPressCircle = this.onPressCircle.bind(this);
		this.lastHeardBoardDate = this.lastHeardBoardDate.bind(this);


		this.onUserLocationUpdate = this.onUserLocationUpdate.bind(this);
	}

	async componentDidMount() {
		if (Constants.IS_ANDROID) {
			const isGranted = await Mapbox.requestAndroidLocationPermissions();
			this.setState({
				isAndroidPermissionGranted: isGranted,
				isFetchingAndroidPermission: false,
			});
		}
	}

	onUserLocationUpdate(location) {

		if (location) {
			this.props.setMap({
				center: this.props.map.center,
				zoom: this.props.map.zoom,
				userLocation: [location.coords.longitude, location.coords.latitude]
			});
		}
	}

	makeLineCollection(board) {

		var featureCollection = {
			"type": "FeatureCollection",
			"features": []
		};
		var route = {
			"type": "Feature",
			"geometry": {
				"type": "LineString",
				"coordinates": []
			}
		};

		//locations.locations :) ascending order
		var locationHistory = board.locations.sort((a, b) => a.d - b.d);

		locationHistory.map((location) => {
			route.geometry.coordinates.push([location.o, location.a]);
		});

		// debug
		if (Constants.debug)
			for (var i = 0; i < route.geometry.coordinates.length - 1; i++) {
				route.geometry.coordinates[i] = [route.geometry.coordinates[i][0] + (Math.random() * .01), route.geometry.coordinates[i][1] + (Math.random() * .01)];
			}

		featureCollection.features.push(route);
		return featureCollection;
	}

	makePoint(board) {

		//locations.locations :) ascending order
		var locationHistory = board.locations.sort((a, b) => a.d - b.d);
		var lastLocation = locationHistory[locationHistory.length - 1];

		var featureCollection = {
			"type": "FeatureCollection",
			"features": []
		};
		var point = {
			"type": "Feature",
			"geometry": {
				"type": "Point",
				"coordinates": [lastLocation.o, lastLocation.a]
			},
			"properties": {
				"board": board.board
			}
		};

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

		var boardPicked = this.props.locations.filter((board) => {
			return board.board == e.nativeEvent.payload.properties.board;
		})[0];

		this.setState({
			boardPicked: boardPicked
		});
		await this.sleep(3000);
		this.setState({
			boardPicked: null
		});
	}

	lastHeardBoardDate() {
		var locationHistory = this.state.boardPicked.locations.sort((a, b) => a.d - b.d);
		var lastLocation = locationHistory[locationHistory.length - 1];
		return new Date(lastLocation.d).toLocaleString();

	}
	buildMap() {
		try {
			var a = new Array();
			var MP = this;
			var shapeSource;

			this.props.locations.map((board) => {

				if (board.locations.length > 1) {
					shapeSource = (
						<Mapbox.ShapeSource id={"SS" + board.board} key={"SS" + board.board} shape={this.makeLineCollection(board)}>
							<Mapbox.LineLayer id={"LL" + board.board} key={"LL" + board.board} style={{
								lineColor: StateBuilder.boardColor(board.board, this.props.boardData),
								lineWidth: 5,
								lineOpacity: .7,
								lineJoin: "round",
								lineCap: "round",
							}} />
						</Mapbox.ShapeSource>);

					a.push(shapeSource);
				}
				if (board.locations.length > 0) {

					shapeSource = (
						<Mapbox.ShapeSource id={"C" + board.board} key={"C" + board.board}
							shape={this.makePoint(board)}
							onPress={MP.onPressCircle}>
							<Mapbox.CircleLayer id={"CL" + board.board} key={"CL" + board.board}
								style={{
									circleRadius: 8,
									circleColor: StateBuilder.boardColor(board.board, this.props.boardData),
									circleStrokeColor: "black",
									circleStrokeWidth: 2
								}} />
						</Mapbox.ShapeSource>);

					a.push(shapeSource);
				}
			});
			return a;
		}
		catch (error) {
			console.log(error);
		}

	}

	render() {

		if (Constants.IS_ANDROID && !this.state.isAndroidPermissionGranted) {
			if (this.state.isFetchingAndroidPermission) {
				return null;
			}
		}

		var MP = this;

		return (
			<View style={StyleSheet.container}>
				<Mapbox.MapView
					styleURL={Mapbox.StyleURL.Street}
					ref={c => (this._map = c)}
					style={StyleSheet.container}>
					<Mapbox.Camera
						zoomLevel={MP.props.map.zoom}
						animationMode={"flyTo"}
						animationDuration={1000}
						centerCoordinate={MP.props.map.center}
						followUserLocation={this.state.followUserLocation}
						followUserMode="compass"
						followZoomLevel={MP.props.map.zoom}
						showUserLocation={true} />
					{(!this.props.isMonitor) ? (<Mapbox.UserLocation onUpdate={this.onUserLocationUpdate} />) : <View></View>}
					{this.buildMap()}
				</Mapbox.MapView>

				{(this.state.boardPicked) ? (
					<Bubble>
						<Text>{this.state.boardPicked.board}</Text>
						<Text>last heard: {this.lastHeardBoardDate()}</Text>
						<Text>battery: {this.state.boardPicked.b}%</Text>
					</Bubble>
				) : <View />}
				{(!this.props.isMonitor) ?
					<View style={StyleSheet.horizontalButtonBar}>
						<View style={StyleSheet.horizonralButton}>
							<Touchable
								onPress={async () => {
									try {
										var followUserLocation = !this.state.followUserLocation;
										this.props.setMap({
											center: this.props.map.userLocation,
											zoom: 13,
											userLocation: this.props.map.userLocation
										});
										this.setState({
											meButtonColor: (followUserLocation) ? "green" : "skyblue",
											followUserLocation: followUserLocation,
										});
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
											manButtonColor: "green",
										});
										this.props.setMap({
											center: Constants.MAN_LOCATION,
											zoom: 13,
											userLocation: this.props.map.userLocation
										});
										await this.sleep(1000);
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
					: <View />}

			</View>
		);
	}
}

MapController.propTypes = {
	locations: PropTypes.array,
	userPrefs: PropTypes.object,
	setMap: PropTypes.func,
	map: PropTypes.object,
	boardData: PropTypes.any,
	setUserPrefs: PropTypes.func,
	isMonitor: PropTypes.bool,
	updateMonitor: PropTypes.func,
};
