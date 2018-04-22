import React from "react";
import { StyleSheet } from "react-native";
import MapView from "react-native-maps"; 
import PropTypes from "prop-types";
//import Marker from 'react-native-maps';

export default class MapController extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			mediaState: props.mediaState,
			locationState: props.locationState,
			region: {
				latitude: 37.78825,
				longitude: -122.4324,
				latitudeDelta: 0.0922,
				longitudeDelta: 0.0922,
			}
		};

	}

	componentWillReceiveProps(nextProps) {
		this.setState({
			mediaState: nextProps.mediaState,
			locationState: nextProps.locationState,
		});
	}

	onRegionChange(region) {
		return region;
		//this.setState({ region: region });
	}

	render() {

		return (
			<MapView
				style={styles.map}
				initialRegion={this.state.region}
				region={this.state.locationState.location}
				onRegionChange={this.onRegionChange}
			>
			</MapView>
		);
	}
}

MapController.propTypes = {
	mediaState: PropTypes.object,
};

const styles = StyleSheet.create({
	map: {
		height: 200,
		marginVertical: 50,
	},
});

MapController.propTypes = {
	mediaState: PropTypes.object,
	locationState: PropTypes.object,
};

//{this.state.markers.map(marker => (
//<Marker
//coordinate={marker.latlng}
//title={marker.title}
//description={marker.description}
///>
//))}
//</MapView>
