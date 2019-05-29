import React from "react";
import BoardManager from "./BoardManager";
import StateBuilder from "./StateBuilder";
import Cache from "./Cache";
import Constants from "./Constants";
export default class App extends React.Component {

	constructor() {
		super();

		this.state = {
			userPrefs: StateBuilder.blankUserPrefs()
		};

		this.setUserPrefs = this.setUserPrefs.bind(this);
	}

	async componentDidMount() {

		var p = await Cache.get(Constants.USER_PREFS);

		if(p){
			this.setState({
				userPrefs: p,
			});
		}
	}
 
	async setUserPrefs(userPrefs) {

		await Cache.set(Constants.USER_PREFS, userPrefs);

		this.setState({
			userPrefs: userPrefs, 
		});
	}

	render() {
		return <BoardManager setUserPrefs={this.setUserPrefs} userPrefs={this.state.userPrefs} />;
	}
}
 