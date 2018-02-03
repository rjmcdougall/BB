import React from 'react';
import PropTypes from 'prop-types';
import { withStyles } from 'material-ui/styles';
import { FormControl } from 'material-ui/Form';
import SystemUpdate from 'material-ui-icons/SystemUpdate';

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
        margin: theme.spacing.unit*3,
    },
    leftIcon: {
        marginRight: theme.spacing.unit,
    },
    rightIcon: {
        marginLeft: theme.spacing.unit,
    },
});

class SetActiveProfile extends React.Component {


    constructor(props, context) {
        super(props, context);

        this.state = {
            currentProfile: props.currentProfile,
            currentProfileIsGlobal: props.currentProfileIsGlobal,
            currentBoard: props.currentBoard,
        }

    }

    handleActivate = event => {

        var setActiveProfile = this;

        var API = '/boards/' + this.state.currentBoard + '/activeProfile/' + this.state.currentProfile + "/isGlobal/" + this.state.currentProfileIsGlobal;
        console.log(API)
        fetch(API, {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json',
                'Authorization': window.sessionStorage.JWT,
            },
        }).then((res) => res.json())
            .then((data) => {
                console.log(data)
                this.setState({
                    activeProfile: this.state.currentProfile,
                    activeProfileIsGlobal: this.state.currentProfileIsGlobal,
                    open: true,
                    resultsMessage: this.state.currentProfile + " activated",
                });


            })
            .catch((err) => {
                console.log('error : ' + err);
                setActiveProfile.setState({
                    open: true,
                    resultsMessage: err.message
                });

            });




    }

    render() {
        const { classes } = this.props;

        return (
            <Center>
                <div>
                    <div style={{
                        'backgroundColor': 'lightblue',
                        'margin': '1cm 1cm 1cm 1cm',
                        'padding': '10px 5px 15px 20px'
                    }}>When activated, the next time {this.state.currentBoard} is connected to wifi the media will update to "{this.state.currentProfile}" profile.</div>

                    <form className={classes.container} autoComplete="off">

                        <FormControl className={classes.formControl}>
                            <Button onClick={this.handleActivate} className={classes.button} raised dense>
                                <SystemUpdate className={classes.leftIcon} />
                                ActivateProfile
                                <SystemUpdate className={classes.rightIcon} />
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
            </Center>
        );
    }
}

SetActiveProfile.propTypes = {
    classes: PropTypes.object.isRequired,
};

export default withStyles(styles)(SetActiveProfile);

