import React from "react";
import { StackNavigator } from "react-navigation"; // Version can be specified in package.json
import BBComView from "./BBComView";
import BoardManager from "./BoardManager";

const RootStack = StackNavigator(
	{ 
		BBCom: {
			screen: BBComView,
		},
		MediaScreen: {
			screen: BoardManager
		}
	},
	{
		initialRouteName: "MediaScreen",
		navigationOptions: {
			headerStyle: {
				backgroundColor: "blue",
				height: 40,
			},
			headerTintColor: "#fff",
			headerTitleStyle: {
				fontWeight: "bold",
				fontSize: 12,
			},
		},
	},
);

export default class App extends React.Component {
	render() {
		return <RootStack />;
	}
}