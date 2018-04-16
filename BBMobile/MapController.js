import React from "react";
import { StyleSheet } from "react-native";
import MapView from "react-native-maps"; 
import BLEIDs from "./BLEIDs";
import PropTypes from "prop-types";
//import Marker from 'react-native-maps';

export default class MapController extends React.Component {
	constructor(props) {
		super(props);

		this.BLEIDs = new BLEIDs();
		console.log("initializing state...");

		this.state = {
			scannerIsRunning: false,
			peripheral: props.peripheral,
			region: {
				latitude: 37.78825,
				longitude: -122.4324,
				latitudeDelta: 0.0922,
				longitudeDelta: 0.0922,
			}
		};
	}

	componentWillReceiveProps(nextProps) {

		//console.log("MapController component received props:" + this.state.peripheral);
		console.log("MapController component received props:" + nextProps);
		//console.log(nextProps);

		if (nextProps.peripheral == null) {
			console.log("clearing state...");
			this.setState({
				peripheral: null,
			});
		} else {
			console.log("setting state...");
			this.setState({
				peripheral: nextProps.peripheral,
			});
		}
	}

	onRegionChange(region) {
		this.setState({ region: region });
	}

	render() {

		return (
			<MapView
				style={styles.map}
				initialRegion={this.state.region}
				region={this.state.region}
				onRegionChange={this.onRegionChange}
			>
			</MapView>
		);

	}
}

const styles = StyleSheet.create({
	map: {
		height: 200,
		marginVertical: 50,
	},
});

MapController.propTypes = {
	peripheral: PropTypes.object,
};

//{this.state.markers.map(marker => (
//<Marker
//coordinate={marker.latlng}
//title={marker.title}
//description={marker.description}
///>
//))}
//</MapView>
