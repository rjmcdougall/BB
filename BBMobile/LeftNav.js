import React from "react";
import { StyleSheet, Text, View } from "react-native"; 
import PropTypes from "prop-types";
import Touchable from "react-native-platform-touchable"; 
import Constants from "./Constants.js";

export default class LeftNav extends React.Component {
	constructor(props) {
		super(props);
	}

	render() {
		return (
			<View style={{ width: 50, backgroundColor: "powderblue" }}>
				<Touchable
					onPress={async () => {
						this.props.onNavigate(Constants.MEDIA_MANAGEMENT)
					}}
					style={{ height: 50, }}
					background={Touchable.Ripple("blue")}>
					<Text style={styles.rowText}>
						Media Management
					</Text>
				</Touchable>
				<Touchable
					onPress={async () => {
						this.props.onNavigate(Constants.ADMINISTRATION)
					}}
					style={{ height: 50, }}
					background={Touchable.Ripple("blue")}>
					<Text style={styles.rowText}>
						Administration
					</Text>
				</Touchable>
				<Touchable
					onPress={async () => {
						this.props.onNavigate(Constants.DIAGNOSTIC)
					}}
					style={{ height: 50 }}
					background={Touchable.Ripple("blue")}>
					<Text style={styles.rowText}>Diagnostic</Text>
				</Touchable>
				<Touchable
					onPress={async () => {
						await this.props.onPressSearchForBoards();
					}}
					style={{
						height: 50,
					}}
					background={Touchable.Ripple("blue")}>
					<Text style={styles.rowText}>Search for Boards</Text>
				</Touchable>
			</View>
		);
	}
}

LeftNav.propTypes = {
	onPressSearchForBoards: PropTypes.func,
	onNavigate: PropTypes.func,
};

const styles = StyleSheet.create({
 
	rowText: {
		margin: 5,
		fontSize: 14,
		textAlign: "center",
		padding: 10,
	},
 
});