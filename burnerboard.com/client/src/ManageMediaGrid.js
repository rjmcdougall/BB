import React from 'react';
import classNames from 'classnames';
import PropTypes from 'prop-types';
import { withStyles } from 'material-ui/styles';
import Table, {
    TableBody,
    TableCell,
    TableHead,
    TableRow,
    TableSortLabel,
} from 'material-ui/Table';
import Toolbar from 'material-ui/Toolbar';
import Typography from 'material-ui/Typography';
import Paper from 'material-ui/Paper';
import Checkbox from 'material-ui/Checkbox';
import IconButton from 'material-ui/IconButton';
import DeleteIcon from 'material-ui-icons/Delete';
import { lighten } from 'material-ui/styles/colorManipulator';
import Snackbar from 'material-ui/Snackbar';
import ConfirmDeleteDialog from './ConfirmDeleteDialog'

const columnData = [
    { id: 'localName', numeric: false, disablePadding: true, label: 'File Name' }
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
                            <TableCell
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
        paddingRight: theme.spacing.unit,
        width: '100%',
        overflowX: 'auto',

    },
    highlight:
        theme.palette.type === 'light'
            ? {
                color: theme.palette.secondary.dark,
                backgroundColor: lighten(theme.palette.secondary.light, 0.4),
            }
            : {
                color: lighten(theme.palette.secondary.light, 0.4),
                backgroundColor: theme.palette.secondary.dark,
            },
    spacer: {
        flex: '1 1 100%',
    },
    actions: {
        color: theme.palette.text.secondary,
    },
    title: {
        flex: '0 0 auto',
    },
    table: {
        minWidth: 400,
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
                        <Typography type="title">File Name</Typography>
                    )}
            </div>
            <div className={classes.spacer} />
            <div className={classes.actions}>
                {numSelected > 0 ? (
                    <IconButton aria-label="Delete" onClick={props.onDelete}>
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
        width: '100%',
        marginTop: 0,
    },
    table: {
        minWidth: 375,
    },
    tableWrapper: {
        overflowX: 'auto',
    },
});

class ManageMediaGrid extends React.Component {
    constructor(props, context) {
        super(props, context);

        console.log("props board:" + this.props.currentBoard);
        console.log("props profile: " + this.props.currentProfile);
        this.state = {
            order: 'asc',
            orderBy: 'localName',
            selected: [],
            open: false,
            resultsMessage: "",
            currentBoard: this.props.currentBoard,
            currentProfile: this.props.currentProfile,
            mediaArray: [
                {
                    id: 1,
                    mediaType: 'loading...',
                    localName: 'loading...',
                    ordinal: 0,
                }].sort((a, b) => (a.localName < b.localName ? -1 : 1))
        };
    }

    handleRequestSort = (event, property) => {
        const orderBy = property;
        let order = 'desc';

        if (this.state.orderBy === property && this.state.order === 'desc') {
            order = 'asc';
        }

        const mediaArray =
            order === 'desc'
                ? this.state.mediaArray.sort((a, b) => (b[orderBy] < a[orderBy] ? -1 : 1))
                : this.state.mediaArray.sort((a, b) => (a[orderBy] < b[orderBy] ? -1 : 1));

        this.setState({ mediaArray, order, orderBy });
    };

    componentDidMount() {

        const API = '/boards/' + this.state.currentBoard + '/profiles/' + this.state.currentProfile + '/DownloadDirectoryJSON';

        fetch(API, {
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json',
                'authorization': window.sessionStorage.JWT,
            }
        })
            .then(response => response.json())
            .then(data => {

                console.log("RESULTS FROM GET" + JSON.stringify(data));

                var mediaArray = data.video
                    .filter(function (item) {
                        console.log(item);
                        return item.localName != null;
                    });

                mediaArray = mediaArray
                    .map(function (item) {
                        return {
                            id: "video-" + item.localName,
                            mediaType: "video",
                            localName: `${item.localName}`,
                            ordinal: `${item.ordinal}`,
                        };
                    });

                mediaArray = mediaArray.concat(data.audio.map(function (item) {
                    return {
                        id: "audio-" + item.localName,
                        mediaType: "audio",
                        localName: `${item.localName}`,
                        ordinal: `${item.ordinal}`,
                    }
                }));

                console.log("mediaArray LOADED:" + mediaArray.length);
                this.setState({ "mediaArray": mediaArray });
            })
            .catch(error => {
                console.log(error.message);
            });

    }

    handleClick = (event, id) => {
        const { selected } = this.state;
        const selectedIndex = selected.indexOf(id);
        let newSelected = [id];

        // console.log("selectedIndex : " + selected.indexOf(id));
        
        // if (selectedIndex === -1) {
        //     newSelected = newSelected.concat(selected, id);
        // } else if (selectedIndex === 0) {
        //     newSelected = newSelected.concat(selected.slice(1));
        // } else if (selectedIndex === selected.length - 1) {
        //     newSelected = newSelected.concat(selected.slice(0, -1));
        // } else if (selectedIndex > 0) {
        //     newSelected = newSelected.concat(
        //         selected.slice(0, selectedIndex),
        //         selected.slice(selectedIndex + 1),
        //     );
        // }
        console.log("new selected :" + newSelected);
        this.setState({ selected: newSelected });
    };

    isSelected = id => this.state.selected.indexOf(id) !== -1;

    onDelete = () => {

        var mediaGrid = this;
        var selectedItem = this.state.selected[0].toString();
        var selectLocalName = selectedItem.slice(selectedItem.indexOf('-') + 1)
        var selectedMediaType = selectedItem.slice(0, selectedItem.indexOf('-'));
        var profileID = this.state.currentProfile;
        var boardID = this.state.currentBoard;

        console.log("delete clicked : " + selectedMediaType + ' : ' + selectLocalName + ' : ' + profileID + ' : ' + boardID);
        var API = "";
        if (boardID !== "null")
            API = '/boards/' + boardID + '/profiles/' + profileID + '/' + selectedMediaType + '/' + selectLocalName;
        else
            API = '/profiles/' + profileID + '/' + selectedMediaType + '/' + selectLocalName;

        fetch(API, {
            method: 'DELETE',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json',
                'Authorization': window.sessionStorage.JWT,
            }
        })
            .then((res) => {

                if (!res.ok) {
                    res.json().then(function (json) {
                        console.log('error : ' + JSON.stringify(json));
                        mediaGrid.setState({
                            open: true,
                            resultsMessage: JSON.stringify(json),
                        });
                    });
                }
                else {
                    res.json().then(function (json) {
                        console.log('success : ' + JSON.stringify(json));
                        mediaGrid.setState({
                            open: true,
                            resultsMessage: JSON.stringify(json),
                            selected: [],
                            mediaArray: mediaGrid.state.mediaArray.filter(function (item) {
                                return item.id !== selectedItem;
                            })
                        });
                    });
                }
            })
            .catch((err) => {
                console.log('error : ' + err);
                mediaGrid.setState({
                    open: true,
                    resultsMessage: err.message
                });
            });
    }

    render() {
        const { classes } = this.props;
        const { mediaArray, order, orderBy, selected } = this.state;

        return (
            <Paper className={classes.root}>
                <EnhancedTableToolbar numSelected={selected.length} onDelete={this.onDelete} />
                <div className={classes.tableWrapper}>
                    <Table className={classes.table}>
                        <EnhancedTableHead
                            numSelected={selected.length}
                            order={order}
                            orderBy={orderBy}
                            onRequestSort={this.handleRequestSort}
                            rowCount={mediaArray.length}
                        />
                        <TableBody>
                            {mediaArray.map(n => {
                                const isSelected = this.isSelected(n.id);
                                return (
                                    <TableRow
                                        hover
                                        onClick={event => this.handleClick(event, n.id)}
                                        role="checkbox"
                                        aria-checked={isSelected}
                                        tabIndex={-1}
                                        key={n.id}
                                        selected={isSelected}
                                    >
                                        <TableCell padding="checkbox">
                                            <Checkbox checked={isSelected} />
                                        </TableCell>
                                        <TableCell >{n.localName}</TableCell>
                                    </TableRow>
                                );
                            })}
                        </TableBody>
                    </Table>
                </div>

                <Snackbar
                    anchorOrigin={{
                        vertical: 'bottom',
                        horizontal: 'center',
                    }}
                    open={this.state.open}
                    onClose={this.handleClose}
                    SnackbarContentProps={{
                        'aria-describedby': 'message-id',
                    }}
                    message={this.state.resultsMessage}
                />
            </Paper>
        );
    }
}

ManageMediaGrid.propTypes = {
    classes: PropTypes.object.isRequired,
};

export default withStyles(styles)(ManageMediaGrid);