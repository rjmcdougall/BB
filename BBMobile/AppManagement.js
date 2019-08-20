import React, { Component } from "react";
import {
	View,
	ScrollView,
	Text,
	Linking,
	TextInput,
} from "react-native";

import Touchable from "react-native-platform-touchable";
import PropTypes from "prop-types";
import StyleSheet from "./StyleSheet";
import Mapbox from "@react-native-mapbox-gl/maps";
import Constants from "./Constants";

Mapbox.setAccessToken(
	"sk.eyJ1IjoiZGFuaWVsa2VpdGh3IiwiYSI6ImNqdzhlbHUwZTJvdmUzenFramFmMTQ4bXIifQ.9EXJnBcsrsKyS-veb_dlNg"
);

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
		var userPrefs = this.props.userPrefs;
		userPrefs.offlineMapPercentage = offlineRegionStatus.percentage;
		this.props.setUserPrefs(userPrefs);
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
		var downloadText;
		var downloadBackgroundColor;
		var pointerEvents;

		if (this.props.userPrefs.offlineMapPercentage==100){
			downloadText = "Playa Map Downloaded";
			downloadBackgroundColor = "green";
			pointerEvents = "none";
		}
		else if (this.props.userPrefs.offlineMapPercentage > 0 && this.props.userPrefs.offlineMapPercentage<100){
			downloadText = "Downloading " + Math.round(this.props.userPrefs.offlineMapPercentage) + "%";
			downloadBackgroundColor = "yellow";
			pointerEvents = "auto";
		}
		else {
			downloadText = "Download Playa Map";
			downloadBackgroundColor = "skyblue";
			pointerEvents = "auto";
		}

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
										this.props.updateMonitor(!this.props.isMonitor);
									}}
									style={[{ backgroundColor: (this.state.isMonitor) ? "green" : "skyblue" }]}
									background={Touchable.Ripple("blue")}>
									<Text style={StyleSheet.buttonTextCenter}>Monitor Mode</Text>
								</Touchable>
							</View>
						</View>
						: <View />}
					<View style={{ height: 10 }}></View>
					<View style={StyleSheet.button} pointerEvents={pointerEvents}>
						<Touchable
							onPress={async () => {

								await Mapbox.offlineManager.deletePack("Playa");

								await Mapbox.offlineManager.createPack({
									name: "Playa",
									styleURL: Mapbox.StyleURL.Street,
									minZoom: 5,
									maxZoom: 20,
									bounds: Constants.PLAYA_BOUNDS()
								}, AM.progressListener, AM.errorListener);
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
					<View style={{
						margin: 10,
						padding: 10,
						borderColor: "black",
						borderWidth: 2
					}}>
						<View style={{ height: 40 }}>
							<Text style={StyleSheet.rowText}>Location History Minutes (max 15)</Text>
						</View>
						<View style={{ height: 40 }}>
							<TextInput keyboardType="number-pad"
								style={{ height: 40, width: 200, borderColor: "gray", borderWidth: 1 }}
								onChangeText={async (p) => {
									this.props.userPrefs.locationHistoryMinutes = p;
									this.props.setUserPrefs(this.props.userPrefs);

									this.setState({ p: p });
								}}
								value={this.props.userPrefs.locationHistoryMinutes}
							/>
						</View>
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
	clearCache: PropTypes.func,
	updateMonitor: PropTypes.func,
	isMonitor: PropTypes.bool,
};

