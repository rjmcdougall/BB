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
import Center from 'react-center';

const styles = theme => ({
    container: {
        display: 'flex',
        flexWrap: 'wrap',
    },
    formControl: {
        // margin: theme.spacing.unit,
        margin: 50

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

    constructor(props) {
        super(props);

        this.state = {
            board: "GLOBAL",
            profile: "",
            createProfileOpenSnackbar: this.props.createProfileOpenSnackbar,
            createProfileResultsMessage: this.props.createProfileResultsMessage,
            boardNames: [{ board_name: "loading..." }],
            createProfileBoardName: "GLOBAL",
        };

        this.handleCreateProfile = this.props.handleCreateProfile.bind(this);
        this.handleChange = this.props.handleChange.bind(this);
    }

    componentWillReceiveProps(nextProps) {
        this.setState({
            createProfileOpenSnackbar: nextProps.createProfileOpenSnackbar,
            createProfileResultsMessage: nextProps.createProfileResultsMessage,
            createProfileBoardName: nextProps.createProfileBoardName,
        });
 
    }

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

    handleClose = () => {
        this.setState({ createProfileOpenSnackbar: false });
    };


    render() {
        const { classes } = this.props;

        console.log(this.state.createProfileBoardName + " : " + JSON.stringify(this.state.boardNames))

        return (
            <Center>

                <div>

                    <form className={classes.container} autoComplete="off">

                        <FormControl className={classes.formControl}>
                            <InputLabel htmlFor="board-picker">Board</InputLabel>
                            <Select
                                value={this.state.createProfileBoardName}
                                onChange={this.handleChange}
                                input={<Input name="createProfileBoardName" id="board-picker" />}>
                                <MenuItem selected={this.state.createProfileBoardName === "GLOBAL"} value="GLOBAL">
                                    GLOBAL
                                </MenuItem>
                                {this.state.boardNames.map(item =>
                                    (
                                        <MenuItem selected={this.state.createProfileBoardName === item.board_name} key={item.board_name} value={item.board_name}>{item.board_name}
                                        </MenuItem>))
                                }
                            </Select>
                            <TextField
                                id="profileName"
                                name="createProfileName"
                                label="Profile Name"
                                input={<Input name="createProfileName" id="profile-text" />}
                                margin="normal"
                                onChange={this.handleChange} />
                            <Button onClick={this.handleCreateProfile} className={classes.button} raised dense>
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
                        open={this.state.createProfileOpenSnackbar}
                        onClose={this.handleClose}
                        SnackbarContentProps={{
                            'aria-describedby': 'message-id',
                        }}
                        message={this.state.createProfileResultsMessage}
                    />
                </div>
            </Center>
        );
    }
}

AddProfile.propTypes = {
    classes: PropTypes.object.isRequired,
};

export default withStyles(styles)(AddProfile);

