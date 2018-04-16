import React from "react";
import { View, Text, StyleSheet } from "react-native";
import PropTypes from "prop-types";

export default class BatteryController extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			mediaState: props.mediaState,
			battery: null
		};
	}

	componentWillReceiveProps(nextProps) {
		this.setState({
			mediaState: nextProps.mediaState,
		});
	}

	render() {

		return (
			<View style={{ margin: 10, backgroundColor: "skyblue", height: 40 }}>
				<View style={{
					flex: 1,
					flexDirection: "row",
					justifyContent: "space-between",

				}}>
					<View style={{ height: 50 }}><Text style={styles.rowText}>Battery</Text></View>
					<View style={{ height: 50 }}><Text style={styles.rowText}>{this.state.mediaState.battery}</Text></View>
				</View>
			</View>
		);
	}
}

BatteryController.propTypes = {
	mediaState: PropTypes.object,
};

const styles = StyleSheet.create({
	rowText: {
		margin: 5,
		fontSize: 14,
		padding: 5,
	},
});