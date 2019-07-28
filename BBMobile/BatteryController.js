import React from "react";
import { View, Text, StyleSheet } from "react-native";
import PropTypes from "prop-types";
import AnimatedBar from "react-native-animated-bar";
import Constants from "./Constants";

export default class BatteryController extends React.Component {
	constructor(props) {
		super(props);
	}
	render() {
		var barColor;
		if (this.props.b <= Constants.BATTERY_RED)
			barColor = "red";
		else if (this.props.b <= Constants.BATTERY_YELLOW)
			barColor = "yellow";
		else
			barColor = "green";

		return (
			<View style={styles.container}>
				<AnimatedBar
					progress={this.props.boardState.b/100.0}
					height={null}
					borderColor="#DDD"
					barColor={barColor}
					borderRadius={5}
					borderWidth={5}
					duration={100}
					key={this.props.id + "bar"}
				>
					<View style={[styles.row, styles.center]}>
						<Text key={this.props.id + "t"} style={[styles.barText, { fontSize: 30 }]}>
							{Math.round(this.props.b)}%
						</Text>
					</View>
				</AnimatedBar>
			</View>
		);
	}
}

BatteryController.propTypes = {
	boardState: PropTypes.object,
	id: PropTypes.string,
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
