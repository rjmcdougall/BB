import React from "react";
import { StyleSheet, View } from "react-native";
import PropTypes from "prop-types";
import Touchable from "react-native-platform-touchable";
import Constants from "./Constants.js";
import Icon from "react-native-vector-icons/MaterialCommunityIcons";


export default class LeftNav extends React.Component {
	constructor(props) {
		super(props);
		this.state = { selected: Constants.MEDIA_MANAGEMENT };
	}

	render() {
		return (
			<View style={{ width: 50, backgroundColor: "powderblue" }}>
				<View style={[{ backgroundColor: this.state.selected == Constants.MEDIA_MANAGEMENT ? "green" : "lightblue" }]}>
					<Touchable
						onPress={async () => {
							this.props.onNavigate(Constants.MEDIA_MANAGEMENT);
							this.setState({ selected: Constants.MEDIA_MANAGEMENT });
						}}
						style={styles.rowText}
						background={Touchable.Ripple("blue")}>
						<Icon name="music-box-outline" size={40} color="#000" />
					</Touchable>
				</View>
				<View style={[{ backgroundColor: this.state.selected == Constants.MAP ? "green" : "lightblue" }]}>
					<Touchable
						onPress={async () => {
							this.props.onNavigate(Constants.MAP);
							this.setState({ selected: Constants.MAP });
						}}
						style={styles.rowText}
						background={Touchable.Ripple("blue")}>
						<Icon name="map-marker-multiple" size={40} coolor="#000" />
					</Touchable>
				</View>
				<View style={[{ backgroundColor: this.state.selected == Constants.ADMINISTRATION ? "green" : "lightblue" }]}>
					<Touchable
						onPress={async () => {
							this.props.onNavigate(Constants.ADMINISTRATION);
							this.setState({ selected: Constants.ADMINISTRATION });
						}}
						style={styles.rowText}
						background={Touchable.Ripple("blue")}>
						<Icon name="settings" size={40} color="#000" />
					</Touchable>
				</View>
				<View style={[{ backgroundColor: this.state.selected == Constants.DIAGNOSTIC ? "green" : "lightblue" }]}>
					<Touchable
						onPress={async () => {
							this.props.onNavigate(Constants.DIAGNOSTIC);
							this.setState({ selected: Constants.DIAGNOSTIC });
						}}
						style={styles.rowText}
						background={Touchable.Ripple("blue")}>
						<Icon name="help-network" size={40} color="#000" />
					</Touchable>
				</View>
				<View style={[{ backgroundColor: this.state.selected == Constants.DISCOVER ? "green" : "lightblue" }]}>
					<Touchable
						onPress={async () => {
							await this.props.onPressSearchForBoards();
							this.setState({ selected: Constants.DISCOVER });
						}}
						style={styles.rowText}
						background={Touchable.Ripple("blue")}>
						<Icon name="magnify" size={40} color="#000" />
					</Touchable>
				</View>
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
		margin: 5
	},
});