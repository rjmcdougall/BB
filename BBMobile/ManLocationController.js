import React, { Component } from "react";
import { View, Text, TextInput } from "react-native";
import PropTypes from "prop-types";
import Touchable from "react-native-platform-touchable";
import StyleSheet from "./StyleSheet";

export default class ManLocationController extends Component {
	constructor(props) {
		super(props);
		this.state = {
			latitude: this.props.userPrefs.man.latitude,
			longitude: this.props.userPrefs.man.longitude,
		};
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
							value={"" + this.state.longitude}
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
							value={"" + this.state.latitude}
							keyboardType='numeric'
						/>
					</View>
				</View>
				<View style={StyleSheet.button}>
					<Touchable
						onPress={async () => {
							navigator.geolocation.getCurrentPosition(
								(position) => {
									this.setState({
										latitude: position.coords.latitude,
										longitude: position.coords.longitude,
										error: null,
									});
								},
								(error) => this.setState({ error: error.message }),
								{ enableHighAccuracy: true, timeout: 20000, maximumAge: 1000 }, );
						}}
						background={Touchable.Ripple("blue")}>
						<Text style={StyleSheet.buttonTextCenter}>My Location</Text>
					</Touchable>
				</View>
				<View style={StyleSheet.button}>
					<Touchable
						onPress={async () => {
							this.props.userPrefs.man.latitude = this.state.latitude;
							this.props.userPrefs.man.longitude = this.state.longitude;
							await this.props.setUserPrefs(this.props.userPrefs);
							
						}}
						background={Touchable.Ripple("blue")}>
						<Text style={StyleSheet.buttonTextCenter}>Save Man Location</Text>
					</Touchable>
				</View>
			</View >
		);
	}
}
ManLocationController.propTypes = {
	userPrefs: PropTypes.object,
	setUserPrefs: PropTypes.func,
};