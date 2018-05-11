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

	// @ts-ignore
	responseGoogle(response) {
		console.log("google response: " + response);

		if (response.error != null && response.error !== "") {

			var errorText = "";
			if(response.error.contains("http://burnerboard.com"))
				errorText = "YOU MUST USE HTTPS!!!!!!!!!!!!! GET OFF OF HTTP ITS NOT SECURE!!!!!!"
			else
				errorText = "Error: " + response.error + " " + response.details + " Please try again." ;


			this.setState({ buttonText: errorText});
		}
		else {

			var id_token = response.getAuthResponse().id_token;

			console.log("id token : " + id_token);

			const app = this;

			var API = "/users/Auth";

			fetch(API, {
				method: "POST",
				headers: {
					"Accept": "application/json",
					"Content-Type": "application/json",
					"authorization": id_token,
				}
			})
				.then(res => {
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
						console.log("JWT stored in sessionStorage: " + id_token);
						console.log(window.sessionStorage.getItem("JWT"));


						app.setState({
							JWT: window.sessionStorage.JWT,
							buttonText: "",
						});

						res.text().then(function (text) {
							console.log("OK res : " + text);

						});
					}
				})
				.catch((err) => {

					err.text().then(function (text) {
						app.setState({
							JWT: "",
							buttonText: text,
						});
					});

				});

		}


	}

	render() {

		var JWT = "";
		var appBody = "";

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

