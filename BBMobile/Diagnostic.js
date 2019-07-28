import React, { Component} from "react";
import { View, ScrollView, Text,} from "react-native";
import StateBuilder from "./StateBuilder";
import PropTypes from "prop-types";
import StyleSheet from "./StyleSheet";
export default class Diagnostic extends Component {
	constructor(props) {
		super(props);
	}

	render() {
		return (
			<View style={StyleSheet.container}  >

				<ScrollView style={{ margin: 10 }}>
					<Text>APK Version: {this.props.boardState.apkv} {"\n"}
						Last Updated: {new Date(this.props.boardState.apkd).toDateString()} {"\n"}
						SSID: {this.props.boardState.s} {"\n"}
						IP Address: {this.props.boardState.ip} {"\n"}
					</Text>
					{
						this.props.logLines.map((line) => {
							var color = "white";
							if (line.isError)
								color = "yellow";

							return (<Text key={Math.random()} style={{ backgroundColor: color }}>{line.logLine}</Text>);
						})
					}
				</ScrollView>
			</View>
		);
	}
}
Diagnostic.propTypes = {
	boardState: PropTypes.object,
	logLines: PropTypes.array,
};

Diagnostic.defaultProps = {
	boardState: StateBuilder.blankBoardState(),
	logLines: StateBuilder.blankLogLines(),
};

