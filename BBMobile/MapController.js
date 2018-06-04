import React from "react";
import { StyleSheet, Text, View } from "react-native";
import MapView from "react-native-maps";
import PropTypes from "prop-types";
import Touchable from "react-native-platform-touchable";
export default class MapController extends React.Component {
	constructor(props) {
		super(props);
		this.state = {
			backgroundColor: "lightblue",
		};
	}

	render() {

		try {
			var locations = this.props.mediaState.locations;
			var region;

			if (this.state.backgroundColor == "green")
				region = this.props.mediaState.region;
			else
				region = null;

			return (
				<View style={styles.container}>
					<View style={styles.mapView}>
						<MapView
							style={styles.map}
							region={region}
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

						<Touchable
							onPress={() => {
								if (this.state.backgroundColor == "lightblue")
									this.setState({
										backgroundColor: "green"
									});
								else
									this.setState({
										backgroundColor: "lightblue"
									});
							}
							}
							style={[styles.container, { backgroundColor: this.state.backgroundColor }]}
							background={Touchable.Ripple("blue")}>
							<Text style={styles.rowText}>Auto Zoom</Text>
						</Touchable>
					</View>
				</View>
			);
		}
		catch (error) {
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
	},
	container: {
		flex: 1,
		backgroundColor: "#FFF",
	},
	rowText: {
		fontSize: 14,
		textAlign: "center",
		padding: 10,
	},
	mapView: {
		padding: 10,
	},
});
