import React from "react";
import { StyleSheet } from "react-native";
import MapView from "react-native-maps";
import PropTypes from "prop-types";

export default class MapController extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			mediaState: props.mediaState,
		};

	}

	UNSAFE_componentWillReceiveProps(nextProps) {
		this.setState({
			mediaState: nextProps.mediaState,
		});
	}

 

	render() {

		try{
			var locations = this.state.mediaState.locations;

			console.log("locations: " + this.state.locations);
			console.log("region: " + JSON.stringify(this.state.mediaState.region));
			

			return (
				<MapView
					style={styles.map}
					region={this.state.mediaState.region}
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
