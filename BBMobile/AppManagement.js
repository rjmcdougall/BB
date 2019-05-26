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
import FlipToggle from "react-native-flip-toggle-button";
import ManLocationController from "./ManLocationController";
import Cache from "./Cache";

export default class AppManagement extends Component {
	constructor(props) {
		super(props);

		this.state = {
			isActive: false
		};
	}

	render() {

		return (
			<View style={StyleSheet.container}>
				<ScrollView>
					<View style={{ height: 10 }}></View>
					<View style={StyleSheet.switch}>
						<FlipToggle
							value={this.props.userPrefs.isDevilsHand}
							buttonWidth={290}
							buttonHeight={40}
							buttonRadius={40}
							labelStyle={{
								fontSize: 20,
								fontWeight: "bold",
								textAlign: "center",
							}}
							onLabel={"Devils Hand!"}
							offLabel={"Rightie"}
							sliderOnColor="black"
							sliderOffColor="black"
							buttonOnColor="lightblue"
							buttonOffColor="lightblue"
							onToggle={(value) => {
								this.props.userPrefs.isDevilsHand = value;
								this.props.setUserPrefs(this.props.userPrefs);
							}}
						/>
					</View>
					<View style={StyleSheet.switch}>
						<FlipToggle
							value={this.props.userPrefs.includeMeOnMap}
							buttonWidth={290}
							buttonHeight={40}
							buttonRadius={40}
							labelStyle={{
								fontSize: 20,
								fontWeight: "bold",
								textAlign: "center",
							}}
							onLabel={"Map My Phone"}
							offLabel={"Don't Map My Phone"}
							sliderOnColor="black"
							sliderOffColor="black"
							buttonOnColor="lightblue"
							buttonOffColor="lightblue"
							onToggle={(value) => {
								this.props.userPrefs.includeMeOnMap = value;
								this.props.setUserPrefs(this.props.userPrefs);
							}}
						/>
					</View>
					<View style={StyleSheet.switch}>

						<FlipToggle
							value={this.props.userPrefs.isBurnerMode}
							buttonWidth={290}
							buttonHeight={40}
							buttonRadius={40}
							labelStyle={{
								fontSize: 20,
								fontWeight: "bold",
								textAlign: "center",
							}}
							onLabel={"Burner Mode!!!"}
							offLabel={"Off the Playa"}
							sliderOnColor="black"
							sliderOffColor="black"
							buttonOnColor="lightblue"
							buttonOffColor="lightblue"
							onToggle={(value) => {
								this.props.userPrefs.isBurnerMode = value;
								if (value == true) {
									this.props.userPrefs.wifiLocations = false; // turn off wifi also
									this.props.onLoadAPILocations();
								}
								this.props.setUserPrefs(this.props.userPrefs);
							}}
						/>
					</View>
					{(this.props.userPrefs.isBurnerMode) ?
						<View style={StyleSheet.switch}>
							<FlipToggle
								value={this.props.userPrefs.mapPoints}
								buttonWidth={290}
								buttonHeight={40}
								buttonRadius={40}
								labelStyle={{
									fontSize: 20,
									fontWeight: "bold",
									textAlign: "center",
								}}
								onLabel={"Detailed Map"}
								offLabel={"Minimal Map"}
								sliderOnColor="black"
								sliderOffColor="black"
								buttonOnColor="lightblue"
								buttonOffColor="lightblue"
								onToggle={(value) => {
									this.props.userPrefs.mapPoints = value;
									this.props.setUserPrefs(this.props.userPrefs);
								}}
							/>
						</View>
						: <View></View>}
					{(this.props.userPrefs.isBurnerMode) ?
						<View>
							<ManLocationController setUserPrefs={this.props.setUserPrefs} userPrefs={this.props.userPrefs} />
						</View>
						: <View></View>}
					{(!this.props.userPrefs.isBurnerMode) ?
						<View style={StyleSheet.switch}>
							<FlipToggle
								value={this.props.userPrefs.wifiLocations}
								buttonWidth={290}
								buttonHeight={40}
								buttonRadius={40}
								labelStyle={{
									fontSize: 20,
									fontWeight: "bold",
									textAlign: "center",
								}}
								onLabel={"Wifi + Radio Locations"}
								offLabel={"Radio Locations Only"}
								sliderOnColor="black"
								sliderOffColor="black"
								buttonOnColor="lightblue"
								buttonOffColor="lightblue"
								onToggle={(value) => {
									this.props.userPrefs.wifiLocations = value;
									this.props.onLoadAPILocations();
									this.props.setUserPrefs(this.props.userPrefs);
								}}
							/>
						</View>
						: <View></View>}
					{(!this.props.userPrefs.isBurnerMode) ?
						<View style={StyleSheet.button}>
							<Touchable
								onPress={async () => {
									var supported = await Linking.canOpenURL("https://burnerboard.com");
									if (supported) {
										Linking.openURL("https://burnerboard.com");
									} else {
										console.log("Don't know how to open URI: " + "https://burnerboard.com");
									}
									return true;
								}}
								background={Touchable.Ripple("blue")}>
								<Text style={StyleSheet.buttonTextCenter}>Go To BB.Com</Text>
							</Touchable>
						</View>
						: <View></View>}
					<View style={StyleSheet.button}>
						<Touchable
							onPress={async () => {
								await Cache.clear();
								return true;
							}}
							style={[{ backgroundColor: "skyblue" }]}
							background={Touchable.Ripple("blue")}>
							<Text style={StyleSheet.buttonTextCenter}> Clear Cache </Text>
						</Touchable>
					</View>
					<View style={{ height: 200 }}></View>
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

