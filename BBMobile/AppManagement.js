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
import Mapbox from "@mapbox/react-native-mapbox-gl";
import Constants from "./Constants"
export default class AppManagement extends Component {
	constructor(props) {
		super(props);

		this.state = {
			isActive: false
		};

		this.progressListener = this.progressListener.bind(this);
		this.errorListener = this.errorListener.bind(this);
	}

	async sleep(ms) {
		await this._sleep(ms);
	}

	_sleep(ms) {
		return new Promise((resolve) => setTimeout(resolve, ms));
	}

	async clearCache() {
		this.setState({ cacheButton: "green" });
		await Cache.clear();
		await this.sleep(500);
		this.setState({ cacheButton: "skyblue" });
	}

	async componentDidMount() {
		console.log(await Mapbox.offlineManager.getPacks());
		this.setState({ downloaded: await Mapbox.offlineManager.getPack("Playa") });
	}

	progressListener(offlineRegion, offlineRegionStatus) {
		this.setState({
			name: offlineRegion.name,
			downloadPercentage: offlineRegionStatus.percentage,
			buttonDisabled: true,
		});
	}

	errorListener(error) {
		console.log(error);
	}

	async bbCom() {
		this.setState({ bbComButton: "green" });
		var supported = await Linking.canOpenURL("https://burnerboard.com");
		if (supported) {
			await this.sleep(500);
			Linking.openURL("https://burnerboard.com");
		} else {
			console.log("Don't know how to open URI: " + "https://burnerboard.com");
		}
		this.setState({ bbComButton: "skyblue" });
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

		var downloadText

		if (this.state.downloaded)
			downloadText = "Playa Map Downloaded";
		else if (this.state.downloadPercentage)
			downloadText = "Downloading " + round(this.state.downloadPercentage);
		else
			downloadText = "Download Playa Map";

		var downloadBackgroundColor;
		if (this.state.downloaded)
			downloadBackgroundColor = "green";
		else if (this.state.downloadPercentage == 100)
			downloadBackgroundColor = "green";
		else
			downloadBackgroundColor = "skyblue";

		var AM = this;

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
							<Text style={StyleSheet.buttonTextCenter}> Cloud Locations </Text>
						</Touchable>
					</View>
					<View style={{ height: 10 }}></View>
					<View style={StyleSheet.button}>
						<Touchable
							onPress={async () => {
								await this.clearCache();
							}}
							style={[{ backgroundColor: this.state.cacheButton }]}
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
					<View style={StyleSheet.button} pointerEvents={this.state.downloaded||this.state.buttonDisabled ? "none" : "auto"}>
						<Touchable
							onPress={() => {

								Mapbox.offlineManager.createPack({
									name: "Playa",
									styleURL: Mapbox.StyleURL.Street,
									minZoom: 5,
									maxZoom: 20,
									bounds: Constants.PLAYA_BOUNDS()
								}, AM.progressListener, AM.errorListener)
							}}
							style={[{ backgroundColor: downloadBackgroundColor }]}
							background={Touchable.Ripple("blue")}
							 >
							<Text style={StyleSheet.buttonTextCenter}>{downloadText}</Text>
						</Touchable>
					</View>
					<View style={{ height: 10 }}></View>
					<View style={StyleSheet.button}>
						<Touchable
							onPress={async () => {
								await this.bbCom();
							}}
							style={[{ backgroundColor: this.state.bbComButton }]}
							background={Touchable.Ripple("blue")}>
							<Text style={StyleSheet.buttonTextCenter}>Go To BB.Com</Text>
						</Touchable>
					</View>
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

