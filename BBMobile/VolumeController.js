import React from "react";
import { View, Text,  Slider, StyleSheet } from "react-native";
import PropTypes from "prop-types";

export default class VolumeController extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			mediaState: props.mediaState,
			volume: null
		};

		this.onUpdateVolume = this.props.onUpdateVolume.bind(this);
	}

	componentWillReceiveProps(nextProps) {
		this.setState({
			mediaState: nextProps.mediaState,
		});
	}

	render() {

		return (
			<View style={{ margin: 10, backgroundColor: "skyblue", height: 80 }}>
				<View style={{
					flex: 1,
					flexDirection: "row",
					justifyContent: "space-between",

				}}>
					<View style={{ height: 40 }}><Text style={styles.rowText}>Volume</Text></View>
					<View style={{ height: 40 }}><Text style={styles.rowText}>{this.state.mediaState.volume}</Text></View>
				</View>
				<View style={{ height: 40 }}><Slider value={this.state.mediaState.audio.volume}
					onSlidingComplete={async (value) => {
						try {
							await this.onUpdateVolume(
								{ value }
							);
						}
						catch (error) {
							console.log("VolumeController Error: " + error);
						}
					}}
					minimumValue={0} maximumValue={100} step={10} />
				</View>
			</View>
		);
	}
}

VolumeController.propTypes = {
	mediaState: PropTypes.object,
	onUpdateVolume: PropTypes.func,
};

const styles = StyleSheet.create({
	rowText: {
		margin: 5,
		fontSize: 14,
		padding: 5,
	},
});