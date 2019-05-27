import React, { Component } from "react";
import {
	View,
	ScrollView,
	Text,
	Linking,
} from "react-native";

import Touchable from "react-native-platform-touchable";
import PropTypes from "prop-types";
import StyleSheet from "./StyleSheet";
import ManLocationController from "./ManLocationController";
import Cache from "./Cache";

export default class AppManagement extends Component {
	constructor(props) {
		super(props);

		this.state = {
			isActive: false
		};
	}

	async sleep(ms) {
		await this._sleep(ms);
	}

	_sleep(ms) {
		return new Promise((resolve) => setTimeout(resolve, ms));
	}

	async clearCache() {
		this.setState({cacheButton: "green"});
		await Cache.clear();
		await this.sleep(500);
		this.setState({cacheButton: "skyblue"});
	}

	
	async bbCom(){
		this.setState({bbComButton: "green"});
		var supported = await Linking.canOpenURL("https://burnerboard.com");
		if (supported) {
			await this.sleep(500);
			Linking.openURL("https://burnerboard.com");
		} else {
			console.log("Don't know how to open URI: " + "https://burnerboard.com");
		} 
		this.setState({bbComButton: "skyblue"});
	}
	render() {

		var wifiBackgroundColor;
		if (this.props.userPrefs.wifiLocations == 0) {
			wifiBackgroundColor = "skyblue";
		}
		else {
			wifiBackgroundColor = "green";
		}

		var mapPointsBackgroundColor;
		if (this.props.userPrefs.mapPoints == 0) {
			mapPointsBackgroundColor = "skyblue";
		}
		else {
			mapPointsBackgroundColor = "green";
		}

		var burnerModeBackgroundColor;
		if (this.props.userPrefs.isBurnerMode == 0) {
			burnerModeBackgroundColor = "skyblue";
		}
		else {
			burnerModeBackgroundColor = "green";
		}


		var includeMeOnMapBackgroundColor;
		if (this.props.userPrefs.includeMeOnMap == 0) {
			includeMeOnMapBackgroundColor = "skyblue";
		}
		else {
			includeMeOnMapBackgroundColor = "green";
		}

		var leftHandBackgroundColor;
		if (this.props.userPrefs.isDevilsHand == 0) {
			leftHandBackgroundColor = "skyblue";
		}
		else {
			leftHandBackgroundColor = "green";
		}

		return (
			<View style={StyleSheet.container}>
				<ScrollView>
					<View style={{ height: 10 }}></View>
					<View style={StyleSheet.button}>
						<Touchable
							onPress={() => {
								this.props.userPrefs.includeMeOnMap = !this.props.userPrefs.includeMeOnMap;
								this.props.setUserPrefs(this.props.userPrefs);
							}}
							style={[{ backgroundColor: includeMeOnMapBackgroundColor }]}
							background={Touchable.Ripple("blue")}>
							<Text style={StyleSheet.buttonTextCenter}> Map My Phone </Text>
						</Touchable>
					</View>
					<View style={{ height: 10 }}></View>
					<View style={StyleSheet.button}>
						<Touchable
							onPress={() => {
								this.props.userPrefs.wifiLocations = !this.props.userPrefs.wifiLocations;
								this.props.onLoadAPILocations();
								this.props.setUserPrefs(this.props.userPrefs);
							}}
							style={[{ backgroundColor: wifiBackgroundColor }]}
							background={Touchable.Ripple("blue")}>
							<Text style={StyleSheet.buttonTextCenter}> Radio Locations </Text>
						</Touchable>
					</View>
					<View style={{ height: 10 }}></View>
					<View style={StyleSheet.button}>
						<Touchable
							onPress={async () => {
								await this.bbCom();
							}}
							style={[{ backgroundColor: this.state.bbComButton}]}
							background={Touchable.Ripple("blue")}>
							<Text style={StyleSheet.buttonTextCenter}>Go To BB.Com</Text>
						</Touchable>
					</View>
					<View style={{ height: 10 }}></View>
					<View style={StyleSheet.button}>
						<Touchable
							onPress={async () => {
								await this.clearCache();
							}}
							style={[{ backgroundColor: this.state.cacheButton}]}
							background={Touchable.Ripple("blue")}>
							<Text style={StyleSheet.buttonTextCenter}> Clear Cache </Text>
						</Touchable>
					</View>
					<View style={{ height: 10 }}></View>
					<View style={StyleSheet.button}>
						<Touchable
							onPress={async () => {
								this.props.userPrefs.isDevilsHand = !this.props.userPrefs.isDevilsHand;
								this.props.setUserPrefs(this.props.userPrefs);
							}}
							style={[{ backgroundColor: leftHandBackgroundColor }]}
							background={Touchable.Ripple("blue")}>
							<Text style={StyleSheet.buttonTextCenter}> Left Handed</Text>
						</Touchable>
					</View>
					<View style={{ height: 10 }}></View>
					<View style={StyleSheet.button}>
						<Touchable
							onPress={() => {
								this.props.userPrefs.isBurnerMode = !this.props.userPrefs.isBurnerMode;
								if (this.props.userPrefs.isBurnerMode == true) {
									this.props.userPrefs.wifiLocations = false; // turn off wifi also
									this.props.onLoadAPILocations();
								}
								this.props.setUserPrefs(this.props.userPrefs);
							}}
							style={[{ backgroundColor: burnerModeBackgroundColor }]}
							background={Touchable.Ripple("blue")}>
							<Text style={StyleSheet.buttonTextCenter}> Burner Mode </Text>
						</Touchable>
					</View>
					<View style={{ height: 10 }}></View>
					{(this.props.userPrefs.isBurnerMode) ?
						<View style={StyleSheet.button}>
							<Touchable
								onPress={() => {
									this.props.userPrefs.mapPoints = !this.props.userPrefs.mapPoints;
									this.props.setUserPrefs(this.props.userPrefs);
								}}
								style={[{ backgroundColor: mapPointsBackgroundColor }]}
								background={Touchable.Ripple("blue")}>
								<Text style={StyleSheet.buttonTextCenter}> Map Details </Text>
							</Touchable>
						</View>
						: <View></View>}
					<View style={{ height: 10 }}></View>
					{(this.props.userPrefs.isBurnerMode) ?
						<View>
							<ManLocationController setUserPrefs={this.props.setUserPrefs} userPrefs={this.props.userPrefs} />
						</View>
						: <View></View>}
					<View style={{ height: 10 }}></View>
				</ScrollView>
			</View>
		);
	}
}

AppManagement.propTypes = {
	userPrefs: PropTypes.object,
	setUserPrefs: PropTypes.func,
	onLoadAPILocations: PropTypes.func,
};

