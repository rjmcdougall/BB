import React from "react";
import { withStyles } from "material-ui/styles";
import Table, { TableBody, TableCell, TableHead, TableRow } from "material-ui/Table";
import Paper from "material-ui/Paper";

const styles = theme => ({
	root: {
		width: "100%",
		marginTop: 0,
	},
	tableWrapper: {
		overflowX: "auto",
	},
	menuButton: {
		marginLeft: 0,
		marginRight: 50,
	},

});

class BoardGrid extends React.Component {

	constructor(props, context) {
		super(props, context);

		this.state = {
			boardData: [
				{
					board_name: "loading...",
					last_seen: "loading...",
					is_online: "loading...",
					battery_level: 0
				}
			],
		};
	}

	componentDidMount() {

		const API = "/currentStatuses";

		fetch(API, {
			headers: {
				"Accept": "application/json",
				"Content-Type": "application/json",
				"authorization": window.sessionStorage.JWT,
			}
		})
			.then(response => response.json())
			.then(data => this.setState({
				boardData: data.map(item => ({
					board_name: `${item.board_name}`,
					last_seen: `${item.last_seen}`,
					is_online: `${item.is_online}`,
					battery_level: `${item.battery_level}`,
				}))
			}))
			.catch(error => this.setState({ error }));

	}

	render() {
		const { classes } = this.props;

		var formatAMPM = date => { // This is to display 12 hour format like you asked
			var hours = date.getHours();
			var minutes = date.getMinutes();
			var ampm = hours >= 12 ? "pm" : "am";
			hours = hours % 12;
			hours = hours ? hours : 12; // the hour "0" should be "12"
			minutes = minutes < 10 ? "0" + minutes : minutes;
			var strTime = hours + ":" + minutes + " " + ampm;
			return strTime;
		};

		var displayDate = dateString => {
			if (dateString === "loading...")
				return "loading...";
			else {
				var myDate = new Date(dateString);
				return (myDate.getMonth() + 1) + "/" + myDate.getDate() + "/" + myDate.getFullYear() + " " + formatAMPM(myDate);
			}
		};

		return (
			<Paper className={classes.root}>
				<Table className={classes.table}>
					<TableHead>
						<TableRow>
							<TableCell padding="none">&nbsp;&nbsp;&nbsp;Name</TableCell>
							<TableCell padding="none">Last Seen</TableCell>
							<TableCell padding="none">Is Online</TableCell>
							<TableCell padding="none">Battery Level</TableCell>
						</TableRow>
					</TableHead>
					<TableBody>
						{this.state.boardData.map(item => {
							return (
								<TableRow key={item.board_name}>
									<TableCell padding="none">&nbsp;&nbsp;&nbsp;{item.board_name.trim()}</TableCell>
									<TableCell padding="none">{displayDate(item.last_seen)}</TableCell>
									<TableCell padding="none">{item.is_online}</TableCell>
									<TableCell padding="none"> <meter min="0" max="100" low="25" high="75" optimum="100" value={item.battery_level}></meter> {item.battery_level}</TableCell>
								</TableRow>
							);
						})}
					</TableBody>
				</Table>
			</Paper>
		);
	}
}

export default withStyles(styles)(BoardGrid);