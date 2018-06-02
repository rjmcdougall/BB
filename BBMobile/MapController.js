import React from "react";
import { StyleSheet } from "react-native";
import MapView from "react-native-maps";
import PropTypes from "prop-types";

export default class MapController extends React.Component {
	constructor(props) {
		super(props);

	}
 
	render() {

		try{
			var locations = this.props.mediaState.locations;

			console.log("locations: " + locations);
			console.log("region: " + JSON.stringify(this.props.mediaState.region));
			

			return (
				<MapView
					style={styles.map}
					region={this.props.mediaState.region}
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
		catch(error){
			console.log("Error:" + error);
		}
	
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
