import React, { Component } from "react";
import { StyleSheet, Animated } from "react-native";

import PropTypes from "prop-types";

class AnimatedBar extends Component {
	state = {
		animation: new Animated.Value(this.props.progress),
	};

	UNSAFE_componentWillMount() {
		this.widthInterpolate = this.state.animation.interpolate({
			inputRange: [0, 1],
			outputRange: ["0%", "100%"],
			extrapolate: "clamp",
		});
	}

	componentDidMount() {
		this.attachListener();
	}

	attachListener = () => {
		this.removeListeners();

		if (typeof this.props.onAnimate === "function") {
			this.attachListener(this.props.onAnimate);
		}
	};
	removeListeners = () => {
		this.state.animation.removeAllListeners();
	};
	componentWillUnmount() {
		this.removeListeners();
	}

	componentDidUpdate(prevProps) {
		//If our function changes then attach the new one
		if (prevProps.onAnimate !== this.props.onAnimate) {
			this.attachListener();
		}

		//If our progress has changed we should animate
		if (prevProps.progress !== this.props.progress) {
			if (this.props.animate) {
				Animated.timing(this.state.animation, {
					toValue: this.props.progress,
					duration: this.props.duration,
				}).start();
			} else {
				this.state.animation.setValue(this.props.progress);
			}
		}
	}

	render() {
		const {
			children,
			height,
			borderColor,
			borderWidth,
			borderRadius,
			barColor,
			fillColor,
			row,
			style,
			wrapStyle,
			fillStyle,
			barStyle,
		} = this.props;

		return (
			<Animated.View style={[styles.outer, { height }, row ? styles.flex : undefined, style]}>
				<Animated.View style={[styles.flex, { borderColor, borderWidth, borderRadius }, wrapStyle]}>
					<Animated.View
						style={[StyleSheet.absoluteFill, { backgroundColor: fillColor }, fillStyle]}
					/>
					<Animated.View
						style={[
							styles.bar,
							{
								width: this.widthInterpolate,
								backgroundColor: barColor,
							},
							barStyle,
						]}
					/>
					{children}
				</Animated.View>
			</Animated.View>
		);
	}
}

const styles = StyleSheet.create({
	outer: {
		flexDirection: "row",
	},
	flex: {
		flex: 1,
	},
	bar: {
		position: "absolute",
		left: 0,
		top: 0,
		bottom: 0,
	},
});

AnimatedBar.defaultProps = {
	height: 10,
	borderColor: "#000",
	borderWidth: 1,
	borderRadius: 0,
	barColor: "#FFF",
	fillColor: "rgba(0,0,0,.5)",
	duration: 100,
	animate: true,
};

AnimatedBar.propTypes = {
	borderColor: PropTypes.string,
	id: PropTypes.string,
	barColor: PropTypes.string,
	fillColor: PropTypes.string,
	row: PropTypes.object,
	style: PropTypes.string,
	children: PropTypes.object,
	height: PropTypes.string,
	wrapStyle: PropTypes.string,
	fillStyle: PropTypes.string,
	barStyle: PropTypes.string,
	borderWidth: PropTypes.number,
	borderRadius: PropTypes.number,
	onAnimate: PropTypes.func,
	progress: PropTypes.number,
	duration: PropTypes.number,
	animate: PropTypes.bool,

};

export default AnimatedBar;