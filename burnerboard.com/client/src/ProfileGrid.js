import React from "react";
import classNames from "classnames";
import PropTypes from "prop-types";
import { withStyles } from "material-ui/styles";
import Table, {
	TableBody,
	TableCell,
	TableHead,
	TableRow,
	TableSortLabel,
} from "material-ui/Table";
import Toolbar from "material-ui/Toolbar";
import Paper from "material-ui/Paper";
import Checkbox from "material-ui/Checkbox";
import IconButton from "material-ui/IconButton";
import DeleteIcon from "material-ui-icons/Delete";
import { lighten } from "material-ui/styles/colorManipulator";
import Snackbar from "material-ui/Snackbar";
import Typography from "material-ui/Typography";

const columnData = [
	{ id: "board", numeric: false, label: "Board" },
	{ id: "profile", numeric: false, label: "Profile" },
];
class EnhancedTableHead extends React.Component {
	createSortHandler = property => event => {
		this.props.onRequestSort(event, property);
	};

	render() {
		const { order, orderBy } = this.props;

		return (
			<TableHead>
				<TableRow>
					<TableCell padding="checkbox">
					</TableCell>
					{columnData.map(column => {
						return (
							<TableCell padding="none"
								key={column.id}
								numeric={column.numeric}
								sortDirection={orderBy === column.id ? order : false}
							>
								<TableSortLabel
									active={orderBy === column.id}
									direction={order}
									onClick={this.createSortHandler(column.id)}
								>
									{column.label}
								</TableSortLabel>

							</TableCell>
						);
					}, this)}
				</TableRow>
			</TableHead>
		);
	}
}

EnhancedTableHead.propTypes = {
	numSelected: PropTypes.number.isRequired,
	onRequestSort: PropTypes.func.isRequired,
	order: PropTypes.string.isRequired,
	orderBy: PropTypes.string.isRequired,
	rowCount: PropTypes.number.isRequired,
};

const toolbarStyles = theme => ({
	root: {
		width: "100%",
		overflowX: "auto",
	},
	highlight:
		theme.palette.type === "light"
			? {
				color: theme.palette.secondary.dark,
				backgroundColor: lighten(theme.palette.secondary.light, 0.4),
			}
			: {
				color: lighten(theme.palette.secondary.light, 0.4),
				backgroundColor: theme.palette.secondary.dark,
			},
	spacer: {
		flex: "1 1 100%",
	},
	actions: {
		color: theme.palette.text.secondary,
	},
	title: {
		flex: "0 0 auto",
	},
	menuButton: {
		marginLeft: 0,
		marginRight: 50,
	},
});

let EnhancedTableToolbar = props => {
	const { numSelected, classes } = props;

	return (
		<Toolbar
			className={classNames(classes.root, {
				[classes.highlight]: numSelected > 0,
			})}
		>
			<div className={classes.title}>
				{numSelected > 0 ? (
					<Typography type="subheading">{numSelected} selected</Typography>
				) : (
						<Typography type="title">Profiles</Typography>
					)}
			</div>
			<div className={classes.spacer} />
			<div className={classes.actions}>
				{numSelected > 0 ? (
					<IconButton aria-label="Delete" onClick={props.onDelete} className={classes.menuButton}>
						<DeleteIcon />
					</IconButton>
				) : (
						<div />
					)}
			</div>
		</Toolbar>
	);
};

EnhancedTableToolbar.propTypes = {
	classes: PropTypes.object.isRequired,
	numSelected: PropTypes.number.isRequired,
};

EnhancedTableToolbar = withStyles(toolbarStyles)(EnhancedTableToolbar);

const styles = theme => ({
	root: {
		width: "100%",
		marginTop: 0,
	},
	tableWrapper: {
		overflowX: "auto",
	},
});

class ProfileGrid extends React.Component {
	constructor(props, context) {
		super(props, context);

		this.state = {
			order: "asc",
			orderBy: "profile",
			selected: [],  
			profileArray: [
				{
					id: 1,
					profile: "loading...",
					board: "loading...",
					readOnly: false,
				}].sort((a, b) => (a.profile < b.profile ? -1 : 1))
		};

		this.onProfileDelete = props.onProfileDelete.bind(this);
		this.handleProfileClick = props.handleProfileClick.bind(this);
		this.handleProfileDeleteClose = props.handleProfileDeleteClose.bind(this);
	}

	handleRequestSort = (event, property) => {
		const orderBy = property;
		let order = "desc";

		if (this.state.orderBy === property && this.state.order === "desc") {
			order = "asc";
		}

		const profileArray =
			order === "desc"
				? this.state.profileArray.sort((a, b) => (b[orderBy] < a[orderBy] ? -1 : 1))
				: this.state.profileArray.sort((a, b) => (a[orderBy] < b[orderBy] ? -1 : 1));

		this.setState({ profileArray, order, orderBy });
	};

	componentDidMount() {
		this.loadProfileGrid();
	}
 
	async loadProfileGrid() {
		const API = "/allProfiles/";

		console.log("API CALL TO LOAD PROFILES: " + API);

		try {
			var response = await fetch(API, {
				headers: {
					"Accept": "application/json",
					"Content-Type": "application/json",
					"authorization": window.sessionStorage.JWT,
				}
			});

			var data = await response.json();

			var profileArray = data
				.map(function (item) {
					if (item.isGlobal)
						return {
							id: item.board + "-" + item.name,
							board: "GLOBAL",
							profile: item.name,
							readOnly: item.readOnly,
						}
					else
						return {
							id: item.board + "-" + item.name,
							board: item.board,
							profile: item.name,
							readOnly: item.readOnly,
						};
				});

			this.setState({ "profileArray": profileArray });
		}
		catch (error) {
			this.setState({ error })
		}
	}

	isSelected = id => this.props.profileSelected.indexOf(id) !== -1;

	render() {
		const { classes } = this.props;
		const { profileArray, order, orderBy  } = this.state;

		return (
			<Paper className={classes.root}>
				<EnhancedTableToolbar numSelected={this.props.profileSelected.length} onDelete={this.onProfileDelete} />
				<div className={classes.tableWrapper}>
					<Table className={classes.table}>
						<EnhancedTableHead
							numSelected={this.props.profileSelected.length}
							order={order}
							orderBy={orderBy}
							onRequestSort={this.handleRequestSort}
							rowCount={profileArray.length}
						/>
						<TableBody className={classes.table}>
							{profileArray.map(n => {
								const isSelected = this.isSelected(n.id);
								return (
									<TableRow
										hover
										onClick={event => {
											if (!n.readOnly)
												this.handleProfileClick(event, n.id);
										}}
										role="checkbox"
										aria-checked={isSelected}
										tabIndex={-1}
										key={n.id}
										selected={isSelected}
										disabled={n.readOnly}
									>
										<TableCell padding="none">
											<Checkbox disabled={n.readOnly} checked={isSelected} />
										</TableCell>
										<TableCell padding="none">{n.board}</TableCell>
										<TableCell padding="none">{n.profile}</TableCell>
									</TableRow>
								);
							})}
						</TableBody>
					</Table>
				</div>
				<Snackbar
					anchorOrigin={{
						vertical: "bottom",
						horizontal: "center",
					}}
					open={this.props.profileDeleteSnackbarOpen}
					onClose={this.handleProfileDeleteClose}
					autoHideDuration={3000}
					SnackbarContentProps={{
						"aria-describedby": "message-id",
					}}
					message={this.props.profileDeleteResultsMessage}
				/>
			</Paper>
		);
	}
}

ProfileGrid.propTypes = {
	classes: PropTypes.object.isRequired,
	profileDeleteResultsMessage: PropTypes.string,
	profileDeleteSnackbarOpen: PropTypes.bool,
	profileSelected: PropTypes.string,
};

ProfileGrid.defaultProps = {
	profileDeleteSnackbarOpen: false,
	profileDeleteResultsMessage: "",
	profileSelected: "",
};


export default withStyles(styles)(ProfileGrid);