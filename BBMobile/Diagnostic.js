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
					<Text>APK Version: {this.props.mediaState.APKVersion} {"\n"}
						Last Updated: {this.props.mediaState.APKUpdateDate} {"\n"}
					</Text>
					{
						this.props.mediaState.logLines.map((line) => {
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
};

Diagnostic.defaultProps = {
	mediaState: StateBuilder.blankMediaState(),
};

