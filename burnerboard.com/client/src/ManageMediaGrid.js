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
import PlayIcon from 'material-ui-icons/PlayCircleOutline';
import { lighten } from 'material-ui/styles/colorManipulator';
import Snackbar from 'material-ui/Snackbar';

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
                        <Typography type="title">Media</Typography>
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
        width: '100%',
        marginTop: 0,
    },
    tableWrapper: {
        overflowX: 'auto',
    },
    table: {
        width: '100%',

    },
});

class ManageMediaGrid extends React.Component {
    constructor(props, context) {
        super(props, context);

        this.state = {
            order: 'asc',
            orderBy: 'localName',
            selected: [],
            open: false,
            resultsMessage: "",
            currentProfileIsReadOnly: false,
            mediaArray: [
                {
                    id: 1,
                    mediaType: 'loading...',
                    localName: 'loading...',
                    URL: "",
                    ordinal: 0,
                }].sort((a, b) => (a.localName < b.localName ? -1 : 1)),
        };

        this.loadGrid = this.loadGrid.bind(this);
        this.onDelete = this.onDelete.bind(this);
        this.handleMediaDeleteClose = this.handleMediaDeleteClose.bind(this);
        this.handleRequestSort = this.handleRequestSort.bind(this);
    }

    handleMediaDeleteClose() {
        this.setState({ open: false });
    }

    handleRequestSort(event, property) {
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
        this.loadGrid();
    }

    async loadGrid() {

        var API;
        var profileData;
        var response;
        var data;
        var mediaArray;

        if (this.props.currentBoard != null)
            API = '/boards/' + this.props.currentBoard + '/profiles/' + this.props.currentProfile;
        else
            API = '/profiles/' + this.props.currentProfile;

        console.log("API CALL TO CHECK FOR READ ONLY PROFILE: " + API);

        try {

            response = await fetch(API, {
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json',
                    'authorization': window.sessionStorage.JWT,
                }
            });

            profileData = await response.json();

            if (this.props.currentBoard != null)
                API = '/boards/' + this.props.currentBoard + '/profiles/' + this.props.currentProfile + '/DownloadDirectoryJSON';
            else
                API = '/profiles/' + this.props.currentProfile + '/DownloadDirectoryJSON';

            console.log("API CALL TO LOAD MEDIA GRID: " + API);

            response = await fetch(API, {
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json',
                    'authorization': window.sessionStorage.JWT,
                }
            });

            data = await response.json();

            mediaArray = data.video
                .filter(function (item) {
                    return item.localName != null;
                });

            mediaArray = mediaArray
                .map(function (item) {
                    return {
                        id: "video-" + item.localName,
                        mediaType: "video",
                        localName: item.localName,
                        URL: item.URL,
                        ordinal: item.ordinal,
                    };
                });

            mediaArray = mediaArray.concat(data.audio.map(function (item) {
                return {
                    id: "audio-" + item.localName,
                    mediaType: "audio",
                    URL: item.URL,
                    localName: item.localName,
                    ordinal: item.ordinal,
                }
            }));

            console.log("Profile returned for read only check: " + JSON.stringify(profileData));
            console.log("profile data read only " + profileData[0].readOnly)

            if (profileData[0].readOnly != null) {
                this.setState({
                    currentProfileIsReadOnly: profileData[0].readOnly,
                    "mediaArray": mediaArray,
                    selected: []
                });
            }
            else
                this.setState({
                    currentProfileIsReadOnly: false,
                    "mediaArray": mediaArray,
                    selected: []
                });
        }
        catch (error) {
            console.log(error.message);
        }
    }

    handleClick(event, id) {

        if (!this.state.currentProfileIsReadOnly) {
            let newSelected = [id];
            this.setState({ selected: newSelected });
        }
    }

    handlePreview(event) {
        // eslint-disable-next-line no-console
        console.log(event.currentTarget.getAttribute('dataurl'));
        window.open(event.currentTarget.getAttribute('dataurl'));
    }

    isSelected(id) {
        return this.state.selected.indexOf(id) !== -1;
    }

    async onDelete() {
 
        var selectedItem = this.state.selected[0].toString();
        var selectLocalName = selectedItem.slice(selectedItem.indexOf('-') + 1)
        var selectedMediaType = selectedItem.slice(0, selectedItem.indexOf('-'));
        var profileID = this.props.currentProfile;
        var boardID = this.props.currentBoard;

        var API = "";
        if (boardID != null)
            API = '/boards/' + boardID + '/profiles/' + profileID + '/' + selectedMediaType + '/' + selectLocalName;
        else
            API = '/profiles/' + profileID + '/' + selectedMediaType + '/' + selectLocalName;

        console.log("DELETE MEDIA API: " + API);

        try {
            var res = await fetch(API, {
                method: 'DELETE',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json',
                    'Authorization': window.sessionStorage.JWT,
                }
            });

            var json = await res.json();

            if (!res.ok) {
                this.setState({
                    open: true,
                    resultsMessage: JSON.stringify(json),
                });
            }
            else {
                this.setState({
                    open: true,
                    resultsMessage: JSON.stringify(json),
                    selected: [],
                    mediaArray: this.state.mediaArray.filter(function (item) {
                        return item.id !== selectedItem;
                    })
                });
            }
        }
        catch (error) {
            console.log('error : ' + error);
            this.setState({
                open: true,
                resultsMessage: error.message
            });
        }
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
                                console.log("read only " + this.state.currentProfileIsReadOnly)
                                return (
                                    <TableRow
                                        hover

                                        role="checkbox"
                                        aria-checked={isSelected}
                                        tabIndex={-1}
                                        key={n.id}
                                        selected={isSelected}
                                        disabled={this.state.currentProfileIsReadOnly}
                                    >
                                        <TableCell onClick={event => this.handleClick(event, n.id)} padding="none">
                                            <Checkbox disabled={this.state.currentProfileIsReadOnly} checked={isSelected} />
                                        </TableCell>
                                        <TableCell padding="none" onClick={event => this.handleClick(event, n.id)} >{n.localName}</TableCell>
                                        <TableCell>
                                            <IconButton onClick={event => this.handlePreview(event, n.id)} dataurl={n.URL} className={classes.button}>
                                                <PlayIcon />
                                            </IconButton>
                                        </TableCell>
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
                    onClose={this.handleMediaDeleteClose}
                    autoHideDuration={3000}
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
    currentBoard: PropTypes.string,
    currentProfile: PropTypes.string,
};

export default withStyles(styles)(ManageMediaGrid);