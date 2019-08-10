import { StyleSheet } from "react-native";

export default StyleSheet.create({
	buttonTextCenter: {
		margin: 2,
		fontSize: 24,
		textAlign: "center",
		padding: 5,
		fontWeight: "bold",
	},
	connectButtonTextCenter: {
		margin: 2,
		fontSize: 18,
		textAlign: "center",
		padding: 5,
		fontWeight: "bold",
	},
	smallButtonTextCenter: {
		margin: 2,
		fontSize: 14,
		textAlign: "center",
		padding: 5,
		fontWeight: "bold",
	},
	button: {
		marginTop: 2,
		backgroundColor: "skyblue"
	},
	horizonralButton: {
		flex: 1,
		marginTop: 2,
		backgroundColor: "skyblue"
	},
	horizontalButtonBar: {
		flexDirection: "row",
	},
	icon: {
		margin: 5
	},
	map: {
		flex: 1,
		height: 300,
	},
	mapView: {
		flex: 1,
		margin: 2
	},
	monitorContainer: {
		flex: 1,
		flexDirection: "row",
	},
	monitorMap: {
		flex: 1,
	},
	batteryList: {
		flex: .3,
	},
	container: {
		flex: 1,
	},
	dropDownRowText: {
		marginLeft: 10,
		fontSize: 24,
	},
	rowText: {
		margin: 5,
		fontSize: 14,
		padding: 5,
		fontWeight: "bold",
	},
	footer: {
		height: 40,
		margin: 2,
		justifyContent: "center"
	},
	switch: {
		margin: 5,
		padding: 5,
		flex: 1,
		flexDirection: "row",
	},
	switchText: {
		fontSize: 14,
		fontWeight: "bold",
		textAlign: "center",
	},
	sliderTrack: {
		height: 10,
		borderRadius: 5,
		backgroundColor: "lightgrey",
	},
	sliderThumb: {
		width: 20,
		height: 20,
		borderRadius: 5,
		backgroundColor: "green",
	},
	annotationContainer: {
		width: 30,
		height: 30,
		alignItems: "center",
		justifyContent: "center",
		backgroundColor: "white",
		borderRadius: 15
	},
	annotationFill: {
		width: 30,
		height: 30,
		borderRadius: 15,
		transform: [{ scale: 0.6 }]
	}
});
