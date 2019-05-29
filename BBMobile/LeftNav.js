import React from "react";
import { View } from "react-native";
import PropTypes from "prop-types";
import Touchable from "react-native-platform-touchable";
import Constants from "./Constants.js";
import Icon from "react-native-vector-icons/MaterialCommunityIcons";
import StyleSheet from "./StyleSheet";

export default class LeftNav extends React.Component {
	constructor(props) {
		super(props);
	}

	render() {
		return (
			<View style={{ width: 50, backgroundColor: "powderblue", margin: 2 }}>
				<View style={[{ backgroundColor: this.props.showScreen == Constants.MEDIA_MANAGEMENT ? "green" : "lightblue" }]}>
					<Touchable
						onPress={() => {
							this.props.onNavigate(Constants.MEDIA_MANAGEMENT);
						}}
						style={StyleSheet.icon}>
						<Icon name="music-box-outline" size={40} color="black" />
					</Touchable>
				</View>
				<View style={[{ backgroundColor: this.props.showScreen == Constants.MAP ? "green" : "lightblue" }]}>
					<Touchable
						onPress={() => {
							this.props.onNavigate(Constants.MAP);
						}}
						style={StyleSheet.icon}>
						<Icon name="map-marker-multiple" size={40} color="black" />
					</Touchable>
				</View>
				<View style={[{ backgroundColor: this.props.showScreen == Constants.ADMINISTRATION ? "green" : "lightblue" }]}>
					<Touchable
						onPress={() => {
							this.props.onNavigate(Constants.ADMINISTRATION);
						}}
						style={StyleSheet.icon}>
						<Icon name="settings" size={40} color="black" />
					</Touchable>
				</View>
				<View style={[{ backgroundColor: this.props.showScreen == Constants.APP_MANAGEMENT  ? "green" : "lightblue" }]}>
					<Touchable
						onPress={() => {
							this.props.onNavigate(Constants.APP_MANAGEMENT);
						}}
						style={StyleSheet.icon}>
						<Icon name="cellphone" size={40} color="black" />
					</Touchable>
				</View>
				<View style={[{ backgroundColor: this.props.showScreen == Constants.DIAGNOSTIC ? "green" : "lightblue" }]}>
					<Touchable
						onPress={() => {
							this.props.onNavigate(Constants.DIAGNOSTIC);
						}}
						style={StyleSheet.icon}>
						<Icon name="help-network" size={40} color="black" />
					</Touchable>
				</View>
				<View style={[{ backgroundColor: this.props.showScreen == Constants.DISCOVER ? "green" : "lightblue" }]}>
					<Touchable
						onPress={ () => {
							this.props.onNavigate(Constants.DISCOVER);
						}}
						style={StyleSheet.icon}>
						<Icon name="magnify" size={40} color="black" />
					</Touchable>
				</View>
			</View>
		);
	}
}

LeftNav.propTypes = {
	onPressSearchForBoards: PropTypes.func,
	onNavigate: PropTypes.func,
	showScreen: PropTypes.string,
};
