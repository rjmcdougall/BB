import React from "react";
import { Text, View } from "react-native";
import MapView from "react-native-maps";
import PropTypes from "prop-types";
import Touchable from "react-native-platform-touchable";
import StateBuilder from "./StateBuilder";
import StyleSheet from "./StyleSheet";
import GeoJSON from "./GeoJSON";
import Fence from "./geo/fence";
//import Outline from "./geo/outline";
import Points from "./geo/points";
import Streets from "./geo/streets";
import Toilets from "./geo/toilets";

export default class MapController extends React.Component {
	constructor(props) {
		super(props);
		this.state = {
			autoZoom: false,
		};
	}

	render() {

		try {

			var locations = new Array();
			var region;

			locations = StateBuilder.getLocations(this.props.mediaState, this.props.userPrefs.wifiLocations);

			if(this.props.userPrefs.includeMeOnMap)
				locations = [...locations,this.props.mediaState.phoneLocation];

			if (this.state.autoZoom == true && (locations.length > 0))
				region = StateBuilder.getRegionForCoordinates(locations);
			else
				region = null;

			return (
				<View style={StyleSheet.mapView}>
					<MapView style={StyleSheet.map} region={region} >
						{locations.map(marker => {

							var ONE_DAY = 24 * 60 * 60 * 1000; /* ms */
							var THIRTY_MIN = 30 * 60 * 1000; /* ms */
							var FIVE_MIN = 5 * 60 * 1000;
							var pinColor;

							if (((new Date()) - new Date(marker.dateTime)) < FIVE_MIN)
								pinColor = "green";
							else if (((new Date()) - new Date(marker.dateTime)) < THIRTY_MIN)
								pinColor = "blue";
							else if (((new Date()) - new Date(marker.dateTime)) < ONE_DAY)
								pinColor = "red";

							return (
								<MapView.Marker
									key={marker.board}
									coordinate={{
										latitude: marker.latitude,
										longitude: marker.longitude
									}}
									title={marker.board}
									pinColor={pinColor}
								/>
							);
						})}
						{(this.props.userPrefs.isBurnerMode) ? <GeoJSON geojson={Streets.streets} userPrefs={this.props.userPrefs} /> : <View />}
						{(this.props.userPrefs.isBurnerMode) ? <GeoJSON geojson={Fence.fence} userPrefs={this.props.userPrefs} />  : <View />}
						{(this.props.userPrefs.isBurnerMode && this.props.userPrefs.mapPoints) ? <GeoJSON geojson={Points.points} pinColor="black" userPrefs={this.props.userPrefs} /> : <View />}
						{(this.props.userPrefs.isBurnerMode && this.props.userPrefs.mapPoints) ? <GeoJSON geojson={Toilets.toilets} pinColor="brown" userPrefs={this.props.userPrefs} /> : <View />}
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
				</View>
			);
		}
		catch (error) {
			console.log("MapController: Error:" + error);
		}

	}
}

MapController.propTypes = {
	mediaState: PropTypes.object,
	userPrefs: PropTypes.object,
};
