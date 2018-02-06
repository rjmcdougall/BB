import React from 'react';
import { withStyles } from 'material-ui/styles';
import Table, { TableBody, TableCell, TableHead, TableRow } from 'material-ui/Table';
import Paper from 'material-ui/Paper';
import Typography from 'material-ui/Typography';

const styles = theme => ({
    root: {
        width: '100%',
        marginTop: 0,
    },
    tableWrapper: {
        overflowX: 'auto',
    },
    menuButton: {
        marginLeft: -12,
        marginRight: 20,
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
            currentBoard: props.currentBoard,
        };

        this.loadBoardData = this.loadBoardData.bind(this);

    }

    loadBoardData() {

        const API = '/boards/' + this.state.currentBoard + '/BatteryHistory';

        console.log("API TO GET BATTERY DATA: " + API);

        fetch(API, {
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json',
                'authorization': window.sessionStorage.JWT,
            }
        })
            .then(response => response.json())
            .then(data => this.setState({
                boardData: data.map(item => ({
                    board_name: `${item.board_name}`,
                    BatteryLevel: `${item.BatteryLevel}`,
                    TimeBucket: `${item.TimeBucket}`,
                }))
            }))
            .catch(error => this.setState({ error }));

    }
    componentDidMount() {
        this.loadBoardData();
    }

    componentWillReceiveProps(nextProps){
        this.setState({
            currentBoard: nextProps.currentBoard,
        }, this.loadBoardData);
    }

    render() {
        const { classes } = this.props;

        return (
            <Paper className={classes.root}>
                <Table className={classes.table}>
                    <TableHead>
                        <TableRow>
                            <TableCell>
                                <Typography type="title">Battery History</Typography>
                            </TableCell>
                        </TableRow>
                        <TableRow>
                            <TableCell>Name</TableCell>
                            <TableCell>Battery Level</TableCell>
                            <TableCell>Time Bucket</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {this.state.boardData.map(item => {
                            return (
                                <TableRow key={item.board_name + "-" + item.TimeBucket}>
                                    <TableCell>{item.board_name}</TableCell>
                                    <TableCell>{item.BatteryLevel}</TableCell>
                                    <TableCell>{item.TimeBucket}</TableCell>
                                </TableRow>
                            );
                        })}
                    </TableBody>
                </Table>
            </Paper>
        );
    }
}

export default withStyles(styles)(BatteryHistoryGrid);