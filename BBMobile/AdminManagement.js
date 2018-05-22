import React, {
	Component
} from "react";
import {
	StyleSheet,
	View,
	ScrollView,
	Text,
} from "react-native";

import TrackController from "./TrackController";
import BLEBoardData from "./BLEBoardData";
import Touchable from "react-native-platform-touchable";
import PropTypes from "prop-types";
//import GoogleSignIn from "react-native-google-sign-in";

export default class AdminManagement extends Component {
	constructor(props) {
		super(props);

		this.state = {
			mediaState: BLEBoardData.emptyMediaState,
		};

		this.onSelectDevice = this.onSelectDevice.bind(this);

	}

	async onSelectDevice(idx) {
		this.setState({ mediaState: await BLEBoardData.setTrack(this.state.mediaState, "Device", idx) });
		console.log("Media Management: Set Media State After Update.");
	}


	// later in your code...
	async login() {

		// try {

		// 	await GoogleSignIn.configure({
		// 		// iOS
		// 		clientID: "878807026650-lbdnhcf6q25d4aquh3gujk9bstdkno7d.apps.googleusercontent.com",

		// 		// iOS, Android
		// 		// https://developers.google.com/identity/protocols/googlescopes
		// 		scopes: ["https://www.googleapis.com/auth/drive.readonly"],

		// 		// iOS, Android
		// 		// Whether to request email and basic profile.
		// 		// [Default: true]
		// 		// https://developers.google.com/identity/sign-in/ios/api/interface_g_i_d_sign_in.html#a06bf16b507496b126d25ea909d366ba4
		// 		shouldFetchBasicProfile: true,

		// 		// iOS
		// 		// https://developers.google.com/identity/sign-in/ios/api/interface_g_i_d_sign_in.html#a486c8df263ca799bea18ebe5430dbdf7
		// 		//language: string,

		// 		// iOS
		// 		// https://developers.google.com/identity/sign-in/ios/api/interface_g_i_d_sign_in.html#a0a68c7504c31ab0b728432565f6e33fd
		// 		//loginHint: string,

		// 		// iOS, Android
		// 		// https://developers.google.com/identity/sign-in/ios/api/interface_g_i_d_sign_in.html#ae214ed831bb93a06d8d9c3692d5b35f9
		// 		serverClientID: "845419422405-4e826kofd0al1npjaq6tijn1f3imk43p.apps.googleusercontent.com",

		// 		// Android
		// 		// Whether to request server auth code. Make sure to provide `serverClientID`.
		// 		// https://developers.google.com/android/reference/com/google/android/gms/auth/api/signin/GoogleSignInOptions.Builder.html#requestServerAuthCode(java.lang.String, boolean)
		// 		offlineAccess: true,

		// 		// Android
		// 		// Whether to force code for refresh token.
		// 		// https://developers.google.com/android/reference/com/google/android/gms/auth/api/signin/GoogleSignInOptions.Builder.html#requestServerAuthCode(java.lang.String, boolean)
		// 		//forceCodeForRefreshToken: boolean,

		// 		// iOS
		// 		// https://developers.google.com/identity/sign-in/ios/api/interface_g_i_d_sign_in.html#a211c074872cd542eda53f696c5eef871
		// 		//openIDRealm: string,

		// 		// Android
		// 		// https://developers.google.com/android/reference/com/google/android/gms/auth/api/signin/GoogleSignInOptions.Builder.html#setAccountName(java.lang.String)
		// 		//accountName: "yourServerAccountName",

		// 		// iOS, Android
		// 		// https://developers.google.com/identity/sign-in/ios/api/interface_g_i_d_sign_in.html#a6d85d14588e8bf21a4fcf63e869e3be3
		// 		//hostedDomain: "yourHostedDomain",
		// 	});

		// 	const user = await GoogleSignIn.signInPromise();

		// 	return user.idToken;
		// }
		// catch (error) {
		// 	console.log("AdminManagement: Error " + error );
		// }
	}

	async OLD_componentWillReceiveProps(nextProps) {

		if (nextProps.mediaState) {
			if (this.state.mediaState.peripheral.id != nextProps.mediaState.peripheral.id) {
				this.setState({
					mediaState: nextProps.mediaState,
				});
			}
		}
		else
			console.log("MediaManagement: Null NextProps");
	}

	render() {

		const { navigate } = this.props.navigation;

		return (


			<View style={styles.container}  >
				<View style={styles.contentContainer}>

					<ScrollView style={styles.scroll}>
						<TrackController onSelectTrack={this.onSelectDevice} mediaState={this.state.mediaState} mediaType="Device" />
					</ScrollView>

					<View style={styles.footer}>
						<View style={styles.button}>
							<Touchable 
								onPress={async () => {

									return true;
									
									// try {
									// 	await BleManager.disconnect(this.state.selectedPeripheral.id);
									// }
									// catch (error) {
									// 	console.log("BoardManager: Pressed BBcom: " + error);
									// }

									if (this.state.backgroundLoop)
										clearInterval(this.state.backgroundLoop);

									this.props.navigation.setParams({ title: "Search For Boards" });

									this.setState({
										peripherals: new Map(),
										appState: "",
										selectedPeripheral: BLEBoardData.emptyMediaState.peripheral,
										mediaState: BLEBoardData.emptyMediaState,
										showDiscoverScreen: true,
										showAdminScreen: false,
										discoveryState: "Connect To Board",
										backgroundLoop: null,
									});

									var JWT = await this.login();

									navigate("BBCom",
										{ JWT: JWT});


								}}
								style={styles.touchableStyle}
								background={Touchable.Ripple("blue")}>
								<Text style={styles.rowText}>Go To BB.Com</Text>
							</Touchable>
						</View>
					</View>
				</View>
			</View>
		);
	}
}
AdminManagement.propTypes = {
	mediaState: PropTypes.object,
	locationState: PropTypes.object,
	pointerEvents: PropTypes.string,
	navigation: PropTypes.object,
};

const styles = StyleSheet.create({
	container: {
		flex: 1,
		backgroundColor: "#FFF",
	},
	contentContainer: {
		flex: 1 // pushes the footer to the end of the screen
	},
	rowText: {
		margin: 5,
		fontSize: 14,
		textAlign: "center",
		padding: 10,
	},
	touchableStyle: {
		backgroundColor: "lightblue",
		margin: 5,
		height: 50,
	},
	footer: {
		height: 50,
		flexDirection: "row",
		justifyContent: "space-between"
	},
	button: {
		width: "100%",
		height: 50
	}
});