import React from "react";
import PropTypes from "prop-types";
import { View, StyleSheet, TouchableOpacity } from "react-native";

const styles = StyleSheet.create({
	container: {
		borderRadius: 30,
		position: "absolute",
		bottom: 48,
		left: 48,
		right: 48,
		paddingVertical: 16,
		minHeight: 40,
		alignItems: "center",
		justifyContent: "center",
		backgroundColor: "white",
	},
});

class Bubble extends React.PureComponent {
	static propTypes = {
		onPress: PropTypes.func,
	};

	render() {
		let innerChildView = this.props.children;

		if (this.props.onPress) {
			innerChildView = (
				<TouchableOpacity onPress={this.props.onPress}>
					{this.props.children}
				</TouchableOpacity>
			);
		}

		return (
			<View style={[styles.container, this.props.style]}>{innerChildView}</View>
		);
	}
}

Bubble.propTypes = {
	children: PropTypes.array,
	style: PropTypes.object,
};

export default Bubble;
