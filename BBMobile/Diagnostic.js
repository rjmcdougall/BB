import React, {
	Component
} from "react";
import {
	StyleSheet,
	View,
	ScrollView,
	Text,
} from "react-native";

import BLEBoardData from "./BLEBoardData";
import PropTypes from "prop-types";

export default class Diagnostic extends Component {
	constructor(props) {
		super(props);
	}

	render() {
		return (
			<View style={styles.container}  >
				<View style={styles.contentContainer}>
					<ScrollView style={styles.scroll}>
						<Text> APK Version: {this.props.mediaState.APKVersion} {"\n"}
							Last Updated: {this.props.mediaState.APKUpdateDate} {"\n"}
						</Text>
						{
							this.props.mediaState.logLines.map((line) => {
								var color = "white";
								if(line.isError)
									color="red";

								return (<Text key={Math.random()} style={{backgroundColor: color}}>{line.logLine}</Text>);
							})
						}
					</ScrollView>
				</View>
			</View>
		);
	}
}
Diagnostic.propTypes = {
	mediaState: PropTypes.object,
};

Diagnostic.defaultProps = {
	mediaState: BLEBoardData.emptyMediaState,
};

const styles = StyleSheet.create({
	container: {
		flex: 1,
		backgroundColor: "#FFF",
	},
	contentContainer: {
		flex: 1 // pushes the footer to the end of the screen
	}, 
});