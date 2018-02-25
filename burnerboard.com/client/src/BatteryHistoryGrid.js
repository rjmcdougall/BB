import React from 'react';
import { withStyles } from 'material-ui/styles';
import Table, { TableBody, TableCell, TableHead, TableRow } from 'material-ui/Table';
import Paper from 'material-ui/Paper';
 
const styles = theme => ({
    root: {
        width: '100%',
        marginTop: 0,
        overflowX: 'auto',
    },
    table: {
        width: '100%',
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
        );
    }
}

export default withStyles(styles)(BatteryHistoryGrid);