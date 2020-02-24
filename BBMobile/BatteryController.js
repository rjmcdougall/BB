import React from "react";
import { View, Text, StyleSheet } from "react-native";
import PropTypes from "prop-types";
import AnimatedBar from "./AnimatedBar";
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

		var b = 0;
		
		if (this.props.b <= 100) 
			b = this.props.b ;
		else
			b = 0;
		return (
			<View style={styles.container}>
				<AnimatedBar
					progress={b / 100.0}
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
							{Math.round(b)}%
						</Text>
					</View>
				</AnimatedBar>
			</View>
		);
	}
}

BatteryController.propTypes = {
	b: PropTypes.number,
	id: PropTypes.string,
};

BatteryController.defaultProps = {
	b: 0,
};

const styles = StyleSheet.create({
	container: {
		paddingTop: 5,
		paddingBottom: 5,
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
