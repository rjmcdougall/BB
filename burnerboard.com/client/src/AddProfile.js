import React from 'react';
import PropTypes from 'prop-types';
import { withStyles } from 'material-ui/styles';
import Input, { InputLabel } from 'material-ui/Input';
import { MenuItem } from 'material-ui/Menu';
import { FormControl } from 'material-ui/Form';
import Select from 'material-ui/Select';
import TextField from 'material-ui/TextField';
import Save from 'material-ui-icons/Save';
import Button from 'material-ui/Button';
import Snackbar from 'material-ui/Snackbar';

const styles = theme => ({
    container: {
        display: 'flex',
        flexWrap: 'wrap',
    },
    formControl: {
        margin: theme.spacing.unit,
        minWidth: 120,
    },
    selectEmpty: {
        marginTop: theme.spacing.unit * 2,
    },
    button: {
        margin: theme.spacing.unit,
    },
    leftIcon: {
        marginRight: theme.spacing.unit,
    },
    rightIcon: {
        marginLeft: theme.spacing.unit,
    },
});

class AddProfile extends React.Component {
    state = {
        age: '',
        name: 'hai',
        board: "GLOBAL",
        profile: "",
        open: false,
        resultsMessage: "",
        boardNames: [{ board_name: "loading..." }]
    };

    componentDidMount() {

        const API = '/boards';

        fetch(API, {
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json',
                'authorization': window.sessionStorage.JWT,
            }
        })
            .then(response => response.json())
            .then(data => this.setState({
                boardNames: data.map(item => ({
                    board_name: `${item.name}`,
                }))
            }))
            .catch(error => this.setState({ error }));
    }

    handleChange = event => {
        this.setState({ [event.target.name]: event.target.value });
    };

    handleClose = () => {
        this.setState({ open: false });
    };

    handleClick = event => {

        var addProfile = this;

        var boardID = this.state.board.trim();
        var profileID = this.state.profile.trim();

        console.log("boardID : " + boardID + " profileID : " + profileID);
        if (boardID != "GLOBAL")
            var API = '/boards/' + boardID + '/profiles/' + profileID;
        else
            var API = '/profiles/' + profileID;

        fetch(API, {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json',
                'Authorization': window.sessionStorage.JWT,
            }
        }
        )
            .then((res) => {

                if (!res.ok) {
                    res.json().then(function (json) {
                        console.log('error : ' + JSON.stringify(json));
                        addProfile.setState({
                            open: true,
                            resultsMessage: JSON.stringify(json)
                        });
                    });
                }
                else {
                    res.json().then(function (json) {
                        console.log('success : ' + JSON.stringify(json));
                        addProfile.setState({
                            open: true,
                            resultsMessage: JSON.stringify(json)
                        });
                    });
                }
            })
            .catch((err) => {
                console.log('error : ' + err);
                addProfile.setState({
                    open: true,
                    resultsMessage: err.message
                });

            });
    }

    render() {
        const { classes } = this.props;

        return (
            <div>
                <form className={classes.container} autoComplete="off">
                    <FormControl className={classes.formControl}>
                        <InputLabel htmlFor="board-picker">Board</InputLabel>
                        <Select
                            value={this.state.board}
                            onChange={this.handleChange}
                            input={<Input name="board" id="board-picker" />}
                        >
                            <MenuItem key="GLOBAL" value="GLOBAL">
                                GLOBAL
                        </MenuItem>
                            {this.state.boardNames.map(item => (
                                <MenuItem key={item.board_name} value={item.board_name}>{item.board_name}
                                </MenuItem>))
                            }
                        </Select>
                        <TextField
                            id="profileName"
                            name="profile"
                            label="Profile Name"
                            input={<Input name="profile" id="profile-text" />}
                            margin="normal"
                            onChange={this.handleChange}
                        />
                        <Button onClick={this.handleClick} className={classes.button} raised dense>
                            <Save className={classes.leftIcon} />
                            Save
                     </Button>
                    </FormControl>
                </form>

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
            </div>
        );
    }
}

AddProfile.propTypes = {
    classes: PropTypes.object.isRequired,
};

export default withStyles(styles)(AddProfile);

