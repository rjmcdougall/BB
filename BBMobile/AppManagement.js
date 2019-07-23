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
import Cache from "./Cache";
import Mapbox from "@react-native-mapbox-gl/maps";
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
		await this.props.clearCache();
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
		var downloadText

		if (this.state.downloaded)
			downloadText = "Playa Map Downloaded";
		else if (this.state.downloadPercentage)
			downloadText = "Downloading " + Math.round(this.state.downloadPercentage) + "%";
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
							style={[{ backgroundColor: (this.props.userPrefs.isDevilsHand) ? "green" : "skyblue" }]}
							background={Touchable.Ripple("blue")}>
							<Text style={StyleSheet.buttonTextCenter}> Left Handed</Text>
						</Touchable>
					</View>
					{(Constants.IS_ANDROID) ?
						<View>
							<View style={{ height: 10 }}></View>
							<View style={StyleSheet.button}>
								<Touchable
									onPress={async () => {
										this.props.userPrefs.isMonitor = !this.props.userPrefs.isMonitor;
										this.props.setUserPrefs(this.props.userPrefs);
									}}
									style={[{ backgroundColor: (this.props.userPrefs.isMonitor) ? "green" : "skyblue" }]}
									background={Touchable.Ripple("blue")}>
									<Text style={StyleSheet.buttonTextCenter}>Monitor Mode</Text>
								</Touchable>
							</View>
						</View>
						: <View />}
					<View style={{ height: 10 }}></View>
					<View style={StyleSheet.button} pointerEvents={this.state.downloaded || this.state.buttonDisabled ? "none" : "auto"}>
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
	clearCache: PropTypes.func,
};

