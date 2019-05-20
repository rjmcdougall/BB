import React, { Component } from "react";
import {
	View,
	ScrollView,
	Text,
	Linking,
} from "react-native";

import DeviceController from "./DeviceController";
import Touchable from "react-native-platform-touchable";
import PropTypes from "prop-types";
import StyleSheet from "./StyleSheet";
import FlipToggle from "react-native-flip-toggle-button";
import ManLocationController from "./ManLocationController";

export default class AdminManagement extends Component {
	constructor(props) {
		super(props);

		this.state = {
			isActive: false
		};
	}

	render() {

		var masterText;
		var backgroundColor;

		if (this.props.mediaState.state.audioMaster == 0) {
			masterText = "Enable Master";
			backgroundColor = "skyblue";
		}
		else {
			masterText = "Disable Master";
			backgroundColor = "green";
		}

		if (this.props.mediaState.state.GTFO == 0) {
			GTFOText = "Enable GTFO";
			GTFOBackgroundColor = "skyblue";
		}
		else {
			GTFOText = "Disable GTFO";
			GTFOBackgroundColor = "green";
		}

		//
		return (
			<View style={StyleSheet.container}>
				<ScrollView>
					<DeviceController onSelectTrack={(value) => this.props.sendCommand(this.props.mediaState, "Device", value)} mediaState={this.props.mediaState} mediaType="Device" sendCommand={this.props.sendCommand} />
					<View style={{ height: 10 }}></View>
					<View style={StyleSheet.button}>
						<Touchable
							onPress={async () => {
								this.props.sendCommand(this.props.mediaState, "EnableGTFO", !this.props.mediaState.state.GTFO);
								return true;
							}}
							style={[{ backgroundColor: GTFOBackgroundColor }]}
							background={Touchable.Ripple("blue")}>
							<Text style={StyleSheet.buttonTextCenter}> {GTFOText} </Text>
						</Touchable>
					</View>
					<View style={StyleSheet.button}>
						<Touchable
							onPress={async () => {
								this.props.sendCommand(this.props.mediaState,  "EnableMaster", !this.props.mediaState.state.audioMaster);
								return true;
							}}
							style={[{ backgroundColor: backgroundColor }]}
							background={Touchable.Ripple("blue")}>
							<Text style={StyleSheet.buttonTextCenter}> {masterText}
							</Text>
						</Touchable>
					</View>
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
							onToggle={async (value) => {
								this.props.userPrefs.isDevilsHand = value;
								await this.props.setUserPrefs(this.props.userPrefs);
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
							onToggle={async (value) => {
								this.props.userPrefs.includeMeOnMap = value;
								await this.props.setUserPrefs(this.props.userPrefs);
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
							onToggle={async (value) => {
								this.props.userPrefs.isBurnerMode = value;
								if (value == true) {
									this.props.userPrefs.wifiLocations = false; // turn off wifi also
									await this.props.onLoadAPILocations();
								}
								await this.props.setUserPrefs(this.props.userPrefs);
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
								onToggle={async (value) => {
									this.props.userPrefs.mapPoints = value;
									await this.props.setUserPrefs(this.props.userPrefs);
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
								onToggle={async (value) => {
									this.props.userPrefs.wifiLocations = value;
									await this.props.onLoadAPILocations();
									await this.props.setUserPrefs(this.props.userPrefs);
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
										await Linking.openURL("https://burnerboard.com");
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
					<View style={{ height: 200 }}></View>
				</ScrollView>
			</View>
		);
	}
}

AdminManagement.propTypes = {
	mediaState: PropTypes.object,
	locationState: PropTypes.object,
	pointerEvents: PropTypes.string,
	userPrefs: PropTypes.object,
	setUserPrefs: PropTypes.func,
	onLoadAPILocations: PropTypes.func,
	omEnableMaster: PropTypes.func,
	sendCommand: PropTypes.func
};

