import React from "react";
import { WebView } from "react-native";

export default class BBComView extends React.Component {
	render() {
		
		return (
			<WebView
				source={{ uri: "https://burnerboard.com" }}
				style={{ marginTop: 0 }}
			/>
		);
	}
}
