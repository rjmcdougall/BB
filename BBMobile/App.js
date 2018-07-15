import React from "react";
import BoardManager from "./BoardManager";
import StateBuilder from "./StateBuilder";
 
export default class App extends React.Component {

	constructor() {
		super();

		this.state = {
			userPrefs: StateBuilder.blankUserPrefs()
		};

		this.setUserPrefs = this.setUserPrefs.bind(this);
	}

	async componentDidMount() {
		
		var p = await StateBuilder.getUserPrefs();

		if(p){
			this.setState({
				userPrefs: p,
			});
		}
	}
 
	async setUserPrefs(userPrefs) {
		this.setState({
			userPrefs: await StateBuilder.setUserPrefs(userPrefs),
		});
	}

	render() {
		return <BoardManager setUserPrefs={this.setUserPrefs} userPrefs={this.state.userPrefs} />;
	}
}
 