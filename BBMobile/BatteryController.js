import React from "react";
import { View, Text, StyleSheet } from "react-native";
import PropTypes from "prop-types";
import StateBuilder from "./StateBuilder";
import AnimatedBar from "react-native-animated-bar";

export default class BatteryController extends React.Component {
	constructor(props) {
		super(props);
	}
	render() {

		var battery = this.props.mediaState.battery/100.0;
		var barColor;
		if (battery <= .2)
			barColor = "red";
		else if (battery <= .3)
			barColor = "yellow";
		else
			barColor = "green";


		return (
			<View style={styles.container}>
				<AnimatedBar
					progress={battery}
					height={null}
					borderColor="#DDD"
					barColor={barColor}
					borderRadius={5}
					borderWidth={5}
					duration={100}
				>
					<View style={[styles.row, styles.center]}>
						<Text style={[styles.barText, { fontSize: 30 }]}>
							{battery*100}%
						</Text>
					</View>
				</AnimatedBar>

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
	container: {
		paddingTop: 15,
		paddingHorizontal: 0,
		justifyContent: "space-around",
	},
	row: {
		flexDirection: "row",
	},
	center: {
		justifyContent: "center",
		alignItems: "center",
	},
	barText: {
		backgroundColor: "transparent",
		color: "#FFF",
	},
});
