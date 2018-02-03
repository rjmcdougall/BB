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

    }

    componentDidMount() {

        const API = '/boards/' + this.state.currentBoard + '/BatteryHistory';

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