import React from "react";
import { WebView, View, Text, StyleSheet } from "react-native";
import PropTypes from "prop-types";
import Touchable from "react-native-platform-touchable";
import GoogleSignIn from "react-native-google-sign-in";

export default class BBComView extends React.Component {

	constructor(props) {
		super(props);

		this.state = {
			JWT: props.JWT,
		};

	}

	// later in your code...
	async login() {
		await GoogleSignIn.configure({
			// iOS
			clientID: "878807026650-lbdnhcf6q25d4aquh3gujk9bstdkno7d.apps.googleusercontent.com",

			// iOS, Android
			// https://developers.google.com/identity/protocols/googlescopes
			scopes: ["https://www.googleapis.com/auth/drive.readonly"],

			// iOS, Android
			// Whether to request email and basic profile.
			// [Default: true]
			// https://developers.google.com/identity/sign-in/ios/api/interface_g_i_d_sign_in.html#a06bf16b507496b126d25ea909d366ba4
			shouldFetchBasicProfile: true,

			// iOS
			// https://developers.google.com/identity/sign-in/ios/api/interface_g_i_d_sign_in.html#a486c8df263ca799bea18ebe5430dbdf7
			//language: string,

			// iOS
			// https://developers.google.com/identity/sign-in/ios/api/interface_g_i_d_sign_in.html#a0a68c7504c31ab0b728432565f6e33fd
			//loginHint: string,

			// iOS, Android
			// https://developers.google.com/identity/sign-in/ios/api/interface_g_i_d_sign_in.html#ae214ed831bb93a06d8d9c3692d5b35f9
			serverClientID: "845419422405-4e826kofd0al1npjaq6tijn1f3imk43p.apps.googleusercontent.com",

			// Android
			// Whether to request server auth code. Make sure to provide `serverClientID`.
			// https://developers.google.com/android/reference/com/google/android/gms/auth/api/signin/GoogleSignInOptions.Builder.html#requestServerAuthCode(java.lang.String, boolean)
			offlineAccess: true,

			// Android
			// Whether to force code for refresh token.
			// https://developers.google.com/android/reference/com/google/android/gms/auth/api/signin/GoogleSignInOptions.Builder.html#requestServerAuthCode(java.lang.String, boolean)
			//forceCodeForRefreshToken: boolean,

			// iOS
			// https://developers.google.com/identity/sign-in/ios/api/interface_g_i_d_sign_in.html#a211c074872cd542eda53f696c5eef871
			//openIDRealm: string,

			// Android
			// https://developers.google.com/android/reference/com/google/android/gms/auth/api/signin/GoogleSignInOptions.Builder.html#setAccountName(java.lang.String)
			//accountName: "yourServerAccountName",

			// iOS, Android
			// https://developers.google.com/identity/sign-in/ios/api/interface_g_i_d_sign_in.html#a6d85d14588e8bf21a4fcf63e869e3be3
			//hostedDomain: "yourHostedDomain",
		});

		const user = await GoogleSignIn.signInPromise();

		this.setState({ JWT: user.idToken });
		console.log(this.state.JWT);
	}

	render() {

		return (
			<View>

				<Touchable
					onPress={() => this.login()}
					style={styles.touchableStyle}
					background={Touchable.Ripple("blue")}>
					<Text style={styles.rowText}>Login</Text>
				</Touchable>

				<WebView
					source={{ uri: "http://www.google.com" }}
					style={{ marginTop: 20 }}
				/>
			</View>
		);
	}
}

const styles = StyleSheet.create({
	rowText: {
		margin: 5,
		fontSize: 14,
		textAlign: "center",
		padding: 10,
	},
	touchableStyle: {
		backgroundColor: "lightblue",
		margin: 5,
	},
});

BBComView.propTypes = {
	JWT: PropTypes.string,
};
