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
					<Text>APK Version: {this.props.mediaState.state.APKVersion} {"\n"}
						Last Updated: {new Date(this.props.mediaState.state.APKUpdateDate).toDateString()} {"\n"}
						IP Address: {this.props.mediaState.state.IPAddress} {"\n"}
					</Text>
					{
						this.props.logLines.map((line) => {
							var color = "white";
							if (line.isError)
								color = "red";

							return (<Text key={Math.random()} style={{ backgroundColor: color }}>{line.logLine}</Text>);
						})
					}
				</ScrollView>
			</View>
		);
	}
}
Diagnostic.propTypes = {
	mediaState: PropTypes.object,
	logLines: PropTypes.array,
};

Diagnostic.defaultProps = {
	mediaState: StateBuilder.blankMediaState(),
	logLines: StateBuilder.blankLogLines(),
};

