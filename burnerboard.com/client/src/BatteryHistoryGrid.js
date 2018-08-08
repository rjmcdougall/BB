import React from "react";
import { withStyles } from "material-ui/styles";
import Table, { TableBody, TableCell, TableHead, TableRow } from "material-ui/Table";
import Paper from "material-ui/Paper";
import { Charts, BarChart, Resizable, ChartContainer, ChartRow, YAxis } from "react-timeseries-charts";
import { TimeSeries, Index } from "pondjs";
import PropTypes from "prop-types";

const styles = theme => ({
	root: {
		width: "100%",
		marginTop: 0,
		overflowX: "auto",
	},
	table: {
		width: "100%",
	},
});

class BatteryHistoryGrid extends React.Component {

	constructor(props, context) {
		super(props, context);

		this.state = {
			boardData: [
				{
					board_name: "loading...",
					BatteryLevel: 0,
					TimeBucket: "loading..."
				}
			],
			timeSeriesData: [
				["Sat Mar 03 2018 13:15:00", 50],
			],
		};
		this.loadBoardData = this.loadBoardData.bind(this);
	}

	async loadBoardData() {

		const API = "/boards/" + this.props.currentBoard + "/BatteryHistory";

		console.log("API TO GET BATTERY DATA: " + API);

		try {
			var response = await fetch(API, {
				headers: {
					"Accept": "application/json",
					"Content-Type": "application/json",
					"authorization": window.sessionStorage.JWT,
				}
			});

			var data = await response.json();
			if (data.length > 0) {
				this.setState({
					boardData: data.map(item => ({
						board_name: item.board_name,
						BatteryLevel: item.BatteryLevel,
						TimeBucket: item.TimeBucket,
					})),
					timeSeriesData: data.map(item => {
						var BatteryLevel = item.BatteryLevel;
						if (BatteryLevel < 0)
							BatteryLevel = 0;
						return [new Date(item.TimeBucket),
							BatteryLevel];
					})
				});
			}
			else {
				this.setState({
					boardData: data.map(item => ({
						board_name: item.board_name,
						BatteryLevel: item.BatteryLevel,
						TimeBucket: item.TimeBucket,
					})),
					timeSeriesData: [
						["2017-01-24 00:00", 0],
					],
				});
			}
		}
		catch (error) {
			console.log(error);
			this.setState({ error });
		}
	}

	componentDidMount() {
		this.loadBoardData();
	}

	render() {

		try {
			const { classes } = this.props;
 
			var timeSeriesData = this.state.timeSeriesData.map(([d, value]) => [
				Index.getIndexString("15m", new Date(d)),
				value
			]);
 
			const series = new TimeSeries({
				name: "hilo_rainfall",
				columns: ["index", "precip"],
				points: timeSeriesData,
			});
 
			return (
				<div>
					<div>
						<hr />
						<div className="row">
							<div className="col-md-12">
								<Resizable>
									<ChartContainer timeRange={series.range()} >
										<ChartRow height="150">
											<YAxis
												id="rain"
												label="Battery"
												min={0}
												max={100}
												// format=".2f"
												width="70"
												type="linear"
											/>
											<Charts>
												<BarChart
													axis="rain"
													//   style={style}
													spacing={1}
													columns={["precip"]}
													series={series}
												/>
											</Charts>
										</ChartRow>
									</ChartContainer>
								</Resizable>
							</div>
						</div>
					</div>
					<Paper className={classes.root}>
						<Table className={classes.table}>
							<TableHead>
								<TableRow>
									<TableCell padding="dense">Battery Level</TableCell>
									<TableCell padding="dense">Time Bucket</TableCell>
								</TableRow>
							</TableHead>
							<TableBody>
								{this.state.boardData.map(item => {
									return (
										<TableRow key={item.board_name.trim() + "-" + item.TimeBucket.trim()}>

											<TableCell padding="dense">{item.BatteryLevel}</TableCell>
											<TableCell padding="dense">{item.TimeBucket.trim()}</TableCell>
										</TableRow>
									);
								})}
							</TableBody>
						</Table>
					</Paper>

				</div>
			);
		}
		catch (error) {
			console.log(error);
		}
	}
}

BatteryHistoryGrid.propTypes = {
	currentBoard: PropTypes.string,
};
export default withStyles(styles)(BatteryHistoryGrid);