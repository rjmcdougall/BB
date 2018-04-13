import React from "react";
import { StackNavigator } from "react-navigation"; // Version can be specified in package.json
import HomeScreen from "./HomeScreen";
import BBComView from "./BBComView";
import MediaManagement from "./MediaManagement";

const RootStack = StackNavigator(
	{
		Home: {
			screen: HomeScreen,

		},
		BBCom: {
			screen: BBComView,
		},
		MediaScreen: {
			screen: MediaManagement
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