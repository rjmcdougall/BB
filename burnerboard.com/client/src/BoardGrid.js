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

        const API = '/currentStatuses';

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
                    last_seen: `${item.last_seen}`,
                    is_online: `${item.is_online}`,
                    battery_level: `${item.battery_level}`,
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
                        <TableRow><TableCell>
                            <Typography type="title">Status</Typography>
                        </TableCell></TableRow>
                        <TableRow>
                            <TableCell>Name</TableCell>
                            <TableCell>Last Seen</TableCell>
                            <TableCell>Is Online</TableCell>
                            <TableCell>Battery Level</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {this.state.boardData.map(item => {
                            return (
                                <TableRow key={item.board_name}>
                                    <TableCell>{item.board_name}</TableCell>
                                    <TableCell>{item.last_seen}</TableCell>
                                    <TableCell>{item.is_online}</TableCell>
                                    <TableCell>{item.battery_level}</TableCell>
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