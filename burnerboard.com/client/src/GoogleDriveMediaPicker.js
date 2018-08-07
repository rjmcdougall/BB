import React, { Component } from "react";
import GoogleDriveSpinner from "./GoogleDriveSpinner";
import PropTypes from "prop-types";
import loadScript from "load-script";

const GOOGLE_SDK_URL = "https://apis.google.com/js/api.js";
const DEVELOPER_KEY = "AIzaSyD2LV75zLhuSnHOblyAYz6sUZ-o94dSARQ";
var scriptLoadingStarted = false;
class GoogleDriveMediaPicker extends Component {

	constructor(props) {
		super(props);
		this.state = {
			currentBoard: props.currentBoard,
			currentProfile: props.currentProfile,
			jsonResults: "",
			errorInfo: "",
			spinnerActive: false,
			successMessage: "",
		};

		this.onApiLoad = this.onApiLoad.bind(this);
		this.onChoose = this.onChoose.bind(this);
	}

	componentDidMount() {
		if (this.isGoogleReady()) {
			// google api is already exists
			// init immediately
			this.onApiLoad();
		} else if (!scriptLoadingStarted) {
			// load google api and the init
			scriptLoadingStarted = true;
			loadScript(GOOGLE_SDK_URL, this.onApiLoad);
		} else {
			// is loading
		}
	}

	componentWillReceiveProps(nextProps) {
		this.setState({
			currentBoard: nextProps.currentBoard,
			currentProfile: nextProps.currentProfile,
			jsonResults: "",
			errorInfo: "",
			spinnerActive: false,
			successMessage: "",
		});
	}

	isGoogleReady() {
		return !!window.gapi;
	}

	isGooglePickerReady() {
		return !!window.google.picker;
	}

	onApiLoad() {
		window.gapi.load("picker");
	}

	onChoose() {
		if (!this.isGoogleReady() || !this.isGooglePickerReady())
			return null;
		else
			this.createPicker();
	}

	createPicker() {

		// eslint-disable-next-line
		const googleViewId = google.picker.ViewId.FOLDERS;
		// eslint-disable-next-line
		const docsView = new google.picker.DocsView(googleViewId)
			.setIncludeFolders(true)
			.setMimeTypes("application/vnd.google-apps.audio", "application/vnd.google-apps.video")
			.setSelectFolderEnabled(true);

		const picker = new window.google.picker.PickerBuilder()
			.setSize(600, 800)
			.addView(docsView)
			.setOAuthToken(sessionStorage.getItem("accessToken"))
			.enableFeature(window.google.picker.Feature.NAV_HIDDEN)
			.setOrigin(window.location.protocol + "//" + window.location.host)
			.setDeveloperKey(DEVELOPER_KEY)
			.setCallback(async (data) => {
				var url = "nothing";
				// eslint-disable-next-line
				if (data[google.picker.Response.ACTION] === google.picker.Action.PICKED) {
					// eslint-disable-next-line
					var doc = data[google.picker.Response.DOCUMENTS][0];
					// eslint-disable-next-line
					url = doc[google.picker.Document.URL];

					var API;
					if (this.state.currentBoard != null)
						API = "/boards/" + this.state.currentBoard + "/profiles/" + this.state.currentProfile + "/AddFileFromGDrive";
					else
						API = "/profiles/" + this.state.currentProfile + "/AddFileFromGDrive";

					console.log("API FOR UPLOADING MEDIA: " + API);
					console.log("BODY FOR UPLOAD: " + JSON.stringify({
						GDriveURL: url,
						oauthToken: sessionStorage.getItem("accessToken"),
						fileId: data.docs[0].id,
						currentBoard: this.state.currentBoard,
					}));

					var myErrorInfo = "";
					var myJsonResults = "";
					const googleDriveMediaPicker = this;

					googleDriveMediaPicker.setSpinnerActive(true);

					try {
						var res = await fetch(API, {
							method: "POST",
							headers: {
								"Accept": "application/json",
								"Content-Type": "application/json",
								"authorization": window.sessionStorage.JWT,
							},
							body: JSON.stringify({
								GDriveURL: url,
								oauthToken: sessionStorage.getItem("accessToken"),
								fileId: data.docs[0].id,
								currentBoard: this.state.currentBoard,
							})
						});

						var text = await res.text();
						if (!res.ok) {
							googleDriveMediaPicker.setState({
								errorInfo: text,
								jsonResults: myJsonResults
							});
						}
						else {
							googleDriveMediaPicker.setState({
								errorInfo: myErrorInfo,
								jsonResults: text
							});
						}
						googleDriveMediaPicker.setSpinnerActive(false);
					}
					catch (err) {
						this.setState({ errorInfo: await err.text() });
					}
				}
			});
		picker.build().setVisible(true);
	}

	setSpinnerActive(spinnerActive) {
		var success;
		if (spinnerActive === true) {
			success = this.state.jsonResults + " Click to select another";
			this.setState({
				spinnerActive: spinnerActive,
				successMessage: ""
			});
		}
		else {
			success = this.state.jsonResults + " Click to select another";
			this.setState({
				spinnerActive: spinnerActive,
				successMessage: success
			});
		}
	}

	render() {

		/*eslint-disable */
		if (this.state.jsonResults === "" && this.state.errorInfo === "") {
			this.state.successMessage = "Click to Open a Picker";
		}
		else {
			if (this.state.errorInfo.length > 0) {
				this.state.successMessage = this.state.errorInfo + " Click to select another";
			}
			else {
				this.state.successMessage = this.state.jsonResults + " Click to select another";
			}
		}
		/*eslint-enable */

		return (
			<div onClick={this.onChoose}>
				<div style={{
					"backgroundColor": "lightblue",
					"margin": "1cm 1cm 1cm 1cm",
					"padding": "10px 5px 15px 20px"
				}}><GoogleDriveSpinner loading={this.state.spinnerActive} message={this.state.successMessage} />
				</div>
			</div>
		);
	}
}

GoogleDriveMediaPicker.propTypes = {
	currentBoard: PropTypes.string,
	currentProfile: PropTypes.string
};

export default GoogleDriveMediaPicker;
