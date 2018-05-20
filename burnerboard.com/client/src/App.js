import React, { Component } from "react";
import BBApp from "./BBApp";
import GoogleLoginPage from "./GoogleLoginPage";

class App extends Component {

	constructor(props) {
		super(props);

		this.state = {
			JWT: "",
			buttonText: "Login with Google",
		};

		this.responseGoogle = this.responseGoogle.bind(this);
	}

	getQueryVariable(variable) {
		var query = window.location.search.substring(1);
		var vars = query.split("&");
		for (var i = 0; i < vars.length; i++) {
			var pair = vars[i].split("=");
			if (decodeURIComponent(pair[0]) == variable) {
				return decodeURIComponent(pair[1]);
			}
		}
		console.log("Query variable %s not found", variable);
	}

	// @ts-ignore
	async responseGoogle(response) {
		console.log("google response: " + JSON.stringify(response));

		if (response.error != null && response.error !== "") {

			var errorText = "";
			if (response.error.contains("http://burnerboard.com"))
				errorText = "YOU MUST USE HTTPS!!!!!!!!!!!!! GET OFF OF HTTP ITS NOT SECURE!!!!!!";
			else
				errorText = "Error: " + response.error + " " + response.details + " Please try again.";

			this.setState({ buttonText: errorText });
		}
		else {

			var id_token = response.getAuthResponse().id_token;
			var access_token = response.getAuthResponse().access_token;
			console.log("id token : " + id_token);

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
					res.text().then(function (text) {

						app.setState({
							JWT: "",
							buttonText: text,
						});
					});
				}
				else {

					window.sessionStorage.setItem("JWT", id_token);
					window.sessionStorage.setItem("accessToken", access_token);
					console.log("JWT stored in sessionStorage: " + id_token);
					console.log("accessToken stored in sessionStorage: " + access_token);

					app.setState({
						JWT: window.sessionStorage.JWT,
						buttonText: "",
					});
				}
			}
			catch (err) {
				try {
					var text = await err.text();
					app.setState({
						JWT: "",
						buttonText: text,
					});
				}
				finally { }
			}
		}
	}

	render() {

		var JWT = "";
		var appBody = "";
		var queryJWT = this.getQueryVariable("JWT");

		if (queryJWT && queryJWT != "") {
			console.log(queryJWT);
			window.sessionStorage.setItem("JWT", queryJWT);
		}

		if (window.sessionStorage.JWT != null)
			JWT = window.sessionStorage.JWT;

		if (JWT === "")
			appBody = <GoogleLoginPage buttonText={this.state.buttonText} responseGoogle={this.responseGoogle} />;
		else
			appBody = <BBApp />;

		return (
			<div className="App" style={{ margin: 0 }}>
				{appBody}
			</div>
		);
	}
}

export default App;

