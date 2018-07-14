import React from "react";
import { View, Text, StyleSheet } from "react-native";
import PropTypes from "prop-types";
import StateBuilder from "./StateBuilder";

export default class BatteryController extends React.Component {
	constructor(props) {
		super(props);
	}
 
	render() {

		return (
			<View style={{backgroundColor: "skyblue", height: 40 }}>
				<View style={{
					flex: 1,
					flexDirection: "row",
					justifyContent: "space-between",

				}}>
					<View style={{ height: 40 }}><Text style={styles.rowText}>Battery</Text></View>
					<View style={{ height: 40 }}><Text style={styles.rowText}>{this.props.mediaState.battery}</Text></View>
				</View>
			</View>
		);
	}
}

BatteryController.propTypes = {
	mediaState: PropTypes.object,
};

BatteryController.defaultProps = {
	mediaState: StateBuilder.blankMediaState(),
};

const styles = StyleSheet.create({
	rowText: {
		margin: 5,
		fontSize: 14,
		padding: 5,
	},
});