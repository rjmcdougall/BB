import React, { Component } from "react";
import BBApp from "./BBApp";
import GoogleLoginPage from "./GoogleLoginPage";
import HttpsRedirect from "react-https-redirect";

class App extends Component {

	constructor(props) {
		super(props);

		this.state = {
			buttonText: "Login with Google",
		};

		this.responseGoogle = this.responseGoogle.bind(this);
	}

	getQueryVariable(variable) {
		var query = window.location.search.substring(1);
		var vars = query.split("&");
		for (var i = 0; i < vars.length; i++) {
			var pair = vars[i].split("=");
			if (decodeURIComponent(pair[0]) === variable) {
				return decodeURIComponent(pair[1]);
			}
		}
		console.log("Query variable %s not found", variable);
	}

	// @ts-ignore
	async responseGoogle(response) {

		if (response.error != null && response.error !== "") {
			var errorText = "Error: " + response.error + " " + response.details + " Please try again.";
			this.setState({ buttonText: errorText });
		}
		else {

			var id_token = response.getAuthResponse().id_token;
			var access_token = response.getAuthResponse().access_token;
			var expires_at = response.getAuthResponse().expires_at - 600000;
			//var expires_at = Date.now() + 10000; // for testing now+10 seconds

			const app = this;
			var API = "/users/Auth";

			try {
				var res = await fetch(API, {
					method: "POST",
					headers: {
						"Accept": "application/json",
						"Content-Type": "application/json",
						"authorization": id_token,
					}
				});

				if (!res.ok) {
					var text = await res.text();
					app.setState({
						buttonText: text,
					});
				}
				else {
					window.sessionStorage.JWT = id_token;
					window.sessionStorage.accessToken = access_token;
					window.sessionStorage.expires_at = expires_at;

					app.setState({
						buttonText: "",
					});
				}
			}
			catch (err) {
				try {
					var text2 = await err.text();
					app.setState({
						buttonText: text2,
					});
				}
				finally { }
			}
		}
	}

	reloadOnExpiration() {
		// if the token is expiring, wipe the session storage.
		if (window.sessionStorage.JWT != null) {
			if (window.sessionStorage.expires_at) {
				if (Date.now() > window.sessionStorage.expires_at) {
					window.sessionStorage.removeItem("expires_at");
					window.sessionStorage.removeItem("JWT");
					window.sessionStorage.removeItem("accessToken");

					this.setState({
						buttonText: "Login with Google",
					});
					window.location.reload(true);
				}
			}
		}

	}

	render() {

		var appBody = "";
		var queryJWT = this.getQueryVariable("JWT");

		// passed in from BBMobile App
		if (queryJWT && queryJWT !== "") {
			window.sessionStorage.setItem("JWT", queryJWT);
		}
 
		if (window.sessionStorage.getItem("JWT") !== null && window.sessionStorage.getItem("JWT") !== "") {
			appBody = <BBApp reloadOnExpiration={this.reloadOnExpiration} />;
		}
		else {
			appBody = <GoogleLoginPage buttonText={this.state.buttonText} responseGoogle={this.responseGoogle} />;
		}

		return (

			<HttpsRedirect>
				<div className="App" style={{ margin: 0 }}>
					{appBody}
				</div>
			</HttpsRedirect>

		);
	}
}

export default App;

