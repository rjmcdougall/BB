import React from "react";
import { StyleSheet } from "react-native";
import MapView from "react-native-maps";
import PropTypes from "prop-types";
import BLEBoardData from "./BLEBoardData";

export default class MapController extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			mediaState: props.mediaState,
		};

	}

	componentWillReceiveProps(nextProps) {
		this.setState({
			mediaState: nextProps.mediaState,
		});
	}

	onRegionChange(region) {
		return region;
		//this.setState({ region: region });
	}



	render() {

		var locations = BLEBoardData.emptyMediaState.locations;

		return (
			<MapView
				style={styles.map}
				initialRegion={{
					latitude: 39.7684,
					longitude: -86.1581,
					latitudeDelta: 0.0922,
					longitudeDelta: 0.0922,
				}}
				region={this.state.mediaState.region}
				onRegionChange={this.onRegionChange}
			>
				{locations.map(marker => {
					return (
						<MapView.Marker
							key={marker.title}
							coordinate={{
								latitude: marker.latitude,
								longitude: marker.longitude
							}}
							title={marker.title}
						/>
					);
				})}
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
