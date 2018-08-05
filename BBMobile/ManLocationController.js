import React, { Component } from "react";
import { View, Text, TextInput } from "react-native";
import PropTypes from "prop-types";
import Touchable from "react-native-platform-touchable";
import StyleSheet from "./StyleSheet";
import StateBuilder from "./StateBuilder";

export default class ManLocationController extends Component {
	constructor(props) {
		super(props);
	}

	render() {

		return (
			<View style={{
				margin: 10,
				padding: 10,
				borderColor: "black",
				borderWidth: 2
			}}>
				<Text style={StyleSheet.smallButtonTextCenter}>
					Man Location
				</Text>
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
							var phoneLocation = await StateBuilder.getLocationForMan();
							this.props.userPrefs.man.longitude = parseFloat(phoneLocation.longitude);
							this.props.userPrefs.man.latitude = parseFloat(phoneLocation.latitude);
							await this.props.setUserPrefs(this.props.userPrefs);
						}}
						background={Touchable.Ripple("blue")}>
						<Text style={StyleSheet.smallButtonTextCenter}>My Location</Text>
					</Touchable>
				</View>
				<View style={StyleSheet.button}>
					<Touchable
						onPress={async () => {
							this.props.userPrefs.man.latitude = 40.7866;
							this.props.userPrefs.man.longitude = -119.20660000000001;
							await this.props.setUserPrefs(this.props.userPrefs);
						}}
						background={Touchable.Ripple("blue")}>
						<Text style={StyleSheet.smallButtonTextCenter}>Defult Location</Text>
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