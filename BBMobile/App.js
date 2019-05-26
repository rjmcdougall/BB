import React from "react";
import BoardManager from "./BoardManager";
import StateBuilder from "./StateBuilder";
import FileSystemConfig from "./FileSystemConfig";
import Constants from "./Constants";

//import { Client } from "bugsnag-react-native";
//const bugsnag = new Client("905bfbccb8f9a7e3749038ca1900b1b4");

export default class App extends React.Component {

	constructor() {
		super();

		//bugsnag.notify(new Error("App Loaded"));

		this.state = {
			userPrefs: StateBuilder.blankUserPrefs()
		};

		this.setUserPrefs = this.setUserPrefs.bind(this);
	}

	async componentDidMount() {

		var p = await FileSystemConfig.getCache(Constants.USER_PREFS);

		if(p){
			this.setState({
				userPrefs: p,
			});
		}
	}
 
	async setUserPrefs(userPrefs) {

		await FileSystemConfig.setCache(Constants.USER_PREFS, userPrefs);

		this.setState({
			userPrefs: userPrefs, 
		});
	}

	render() {
		return <BoardManager setUserPrefs={this.setUserPrefs} userPrefs={this.state.userPrefs} />;
	}
}
 