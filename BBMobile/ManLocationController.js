import React, { Component } from "react";
import { View, Text, TextInput } from "react-native";
import PropTypes from "prop-types";
import Touchable from "react-native-platform-touchable";
import StyleSheet from "./StyleSheet";

export default class ManLocationController extends Component {
	constructor(props) {
		super(props);
	}

	async getPhonePosition() {
		return new Promise(function (resolve, reject) {
			navigator.geolocation.getCurrentPosition(
				(position) => {
					resolve([position.coords.latitude, position.coords.longitude]);
				},
				(error) => {
					reject(error);
				},
				{ enableHighAccuracy: true, timeout: 20000, maximumAge: 1000 }, );
		});
	}

	render() {

		return (
			<View>
				<View style={{
					flex: 1,
					flexDirection: "row",
				}}>
					<View style={{ height: 40 }}>
						<Text style={StyleSheet.rowText}>Man Lon</Text>
					</View>
					<View style={{ height: 40 }}>
						<TextInput
							style={{ height: 40, width: 200, borderColor: "gray", borderWidth: 1 }}
							onChangeText={async (text) => {
								this.props.userPrefs.man.longitude = parseFloat(text);
								await this.props.setUserPrefs(this.props.userPrefs);
							}}
							value={"" + this.props.userPrefs.man.longitude}
							keyboardType='numeric'
						/>
					</View>
				</View>
				<View style={{
					flex: 1,
					flexDirection: "row",
				}}>
					<View style={{ height: 40 }}>
						<Text style={StyleSheet.rowText}>Man Lat</Text>
					</View>
					<View style={{ height: 40 }}>
						<TextInput
							style={{ height: 40, width: 200, borderColor: "gray", borderWidth: 1 }}
							onChangeText={async (text) => {
								this.props.userPrefs.man.latitude = parseFloat(text);
								await this.props.setUserPrefs(this.props.userPrefs);
							}}
							value={"" + this.props.userPrefs.man.latitude}
							keyboardType='numeric'
						/>
					</View>
				</View>
				<View style={StyleSheet.button}>
					<Touchable
						onPress={async () => {
							var latLon = await this.getPhonePosition();
							this.props.userPrefs.man.latitude = parseFloat(latLon[0]);
							this.props.userPrefs.man.longitude = parseFloat(latLon[1]);
							await this.props.setUserPrefs(this.props.userPrefs);
						}}
						background={Touchable.Ripple("blue")}>
						<Text style={StyleSheet.buttonTextCenter}>My Location</Text>
					</Touchable>
				</View>
			</View>
		);
	}
}
ManLocationController.propTypes = {
	userPrefs: PropTypes.object,
	setUserPrefs: PropTypes.func,
};