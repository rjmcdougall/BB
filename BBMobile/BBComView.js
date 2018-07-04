import React from "react";
import { WebView, View } from "react-native";
import PropTypes from "prop-types";
 
export default class BBComView extends React.Component {

	constructor(props) {
		super(props);

		this.state = {
			JWT: "",
		};

	}

	render() {

	//	var URI = "https://burnerboard.com?JWT=" + JWT;

		return (
			<View style={{ flex: 1 }}>
				<WebView
					source={{
						uri: "s",
					}}
					style={{ marginTop: 0 }}

				/>
			</View>
		);
	}
}
 

BBComView.propTypes = {
	JWT: PropTypes.string,
};
