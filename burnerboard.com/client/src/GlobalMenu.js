import React from 'react';
import PropTypes from 'prop-types';
import { withStyles } from 'material-ui/styles';
import AppBar from 'material-ui/AppBar';
import Toolbar from 'material-ui/Toolbar';
import Typography from 'material-ui/Typography';
import IconButton from 'material-ui/IconButton';
import MenuIcon from 'material-ui-icons/Menu';
import AccountCircle from 'material-ui-icons/AccountCircle';
import Drawer from 'material-ui/Drawer';
import List from 'material-ui/List';
import Divider from 'material-ui/Divider';
import ListSubheader from 'material-ui/List/ListSubheader';
import purple from 'material-ui/colors/purple';

import { MenuList, MenuItem } from 'material-ui/Menu';

const menuStyles = {
    root: {
        width: '100%',
    },
    flex: {
        flex: 1,
    },
    menuButton: {
        marginLeft: -12,
        marginRight: 20,
    },
    list: {
        width: 250,
    },
    listFull: {
        width: 'auto',
    },
};

class GlobalMenu extends React.Component {

    constructor(props) {
        super(props);

        this.state = {
            drawerIsOpen: props.drawerIsOpen,
            boardNames: [{ board_name: "loading boards..." }],
            profileNames: ["select Board"],
            globalProfileNames: ["select Board"],
            currentProfile: "Select Profile",
            activeProfile: props.activeProfile,
            currentBoard: props.currentBoard,
        };

        this.handleSelect = this.props.handleSelect.bind(this);
        //this.handleSelect = this.handleSelect.bind(this);

    }

    toggleDrawer = (open) => () => {
        this.setState({
            drawerIsOpen: open,
        });

    };

    handleSelect = (event, key) => {

        this.setState({ currentBoard: key });

        console.log(key);
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

    componentWillReceiveProps(nextProps) {

        console.log("got props current board: " + nextProps.currentBoard);
        console.log("got props current profile: " + nextProps.currentProfile);
        console.log("got props for active profile: " + nextProps.activeProfile);
        var API = '/boards/' + nextProps.currentBoard + '/profiles/';

        var profiles;
        var globalProfiles;

        fetch(API, {
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json',
                'authorization': window.sessionStorage.JWT,
            }
        })
            .then(response => response.json())
            .then(data => {

                profiles = data.map(item => ({
                    profile_name: `${item.name}`,
                }));

                API = '/profiles/';

                fetch(API, {
                    headers: {
                        'Accept': 'application/json',
                        'Content-Type': 'application/json',
                        'authorization': window.sessionStorage.JWT,
                    }
                })
                    .then(response => response.json())
                    .then(data2 => {

                        globalProfiles = data2.map(item => ({
                            profile_name: `${item.name}`,
                        }));

                        API = '/boards/' + nextProps.currentBoard;
                        fetch(API, {
                            headers: {
                                'Accept': 'application/json',
                                'Content-Type': 'application/json',
                                'authorization': window.sessionStorage.JWT,
                            }
                        })
                            .then(response => response.json())
                            .then(data3 => {
                                console.log(data3[0]);
                                console.log("IVE GOTTEN RESULTS FOR BOARD QUERY: " + data3[0].isProfileGlobal);

                                this.setState({
                                    currentBoard: nextProps.currentBoard,
                                    profileNames: profiles,
                                    globalProfileNames: globalProfiles,
                                    currentProfile: nextProps.currentProfile,
                                    activeProfile: data3[0].profile,
                                    activeProfileIsGlobal: data3[0].isProfileGlobal,
                                });
                            })
                            .catch(error => this.setState({ error }));
                    })
                    .catch(error => this.setState({ error }));
            })
            .catch(error => this.setState({ error }));
    };

    render() {
        const { classes } = this.props;
        const { auth, anchorEl } = this.state;
        const open = Boolean(anchorEl);

        var optionsDisabled = false;
        var profileDisabled = false;
        if (this.state.currentBoard === "Select Board") {
            optionsDisabled = true;
        };

        if (this.state.currentProfile === "Select Profile") {
            profileDisabled = true;
        };

        var renderProfileTitle = () => {
            if (this.state.activeProfile === this.state.currentProfile)
                return "* " + this.state.currentProfile;
            else
                return this.state.currentProfile;
        }

        var renderProfiles = (inGlobalBlock, item) => {

            console.log("active profie: " + this.state.activeProfile);
            console.log("activeProfileIsGlobal: " + this.state.activeProfileIsGlobal)
            console.log("in global block: " + inGlobalBlock);
            console.log(JSON.stringify(item));

            if (inGlobalBlock) {
                if (this.state.activeProfile === item.profile_name && this.state.activeProfileIsGlobal === inGlobalBlock)
                    return (
                        <MenuItem onClick={event => this.handleSelect(event, "globalProfile-" + item.profile_name)}
                            key={"globalProfile-" + item.profile_name}
                            selected={item.profile_name === this.state.currentProfile}
                        > {"* " + item.profile_name}
                        </MenuItem>
                    );
                else
                    return (
                        <MenuItem onClick={event => this.handleSelect(event, "globalProfile-" + item.profile_name)}
                            key={"globalProfile-" + item.profile_name}
                            selected={item.profile_name === this.state.currentProfile}
                        > {item.profile_name}
                        </MenuItem>
                    );
            }
            else {
                if (this.state.activeProfile === item.profile_name && this.state.activeProfileIsGlobal === inGlobalBlock)
                    return (
                        <MenuItem onClick={event => this.handleSelect(event, "profile-" + item.profile_name)}
                            key={"profile-" + item.profile_name}
                            selected={item.profile_name === this.state.currentProfile}
                        > {"* " + item.profile_name}
                        </MenuItem>
                    );
                else
                    return (
                        <MenuItem onClick={event => this.handleSelect(event, "profile-" + item.profile_name)}
                            key={"profile-" + item.profile_name}
                            selected={item.profile_name === this.state.currentProfile}
                        > {item.profile_name}
                        </MenuItem>
                    );
            }
        }

        return (
            <div className={classes.root}>
                <AppBar position="static">
                    <Toolbar>
                        <IconButton onClick={this.toggleDrawer(true)} className={classes.menuButton} color="inherit" aria-label="Menu">
                            <MenuIcon />
                        </IconButton>
                        <Typography color="inherit" className={classes.flex}>
                            {this.state.currentBoard} {this.state.currentProfile != "Select Profile" ? " - " + renderProfileTitle() : ""}
                        </Typography>
                        {auth && (
                            <div>
                                <IconButton
                                    aria-owns={open ? 'menu-appbar' : null}
                                    aria-haspopup="true"
                                    onClick={this.handleMenu}
                                    color="inherit"
                                >
                                    <AccountCircle />
                                </IconButton>
                            </div>
                        )}
                    </Toolbar>
                </AppBar>
                <Drawer open={this.state.drawerIsOpen} onClose={this.toggleDrawer(false)}>
                    <div
                        tabIndex={0}
                        role="button"

                        onKeyDown={this.toggleDrawer(false)}
                    >
                        <MenuList subheader={<ListSubheader >Boards</ListSubheader>} className={classes.list} >
                            {this.state.boardNames.map(item => (
                                <MenuItem onClick={event => this.handleSelect(event, "board-" + item.board_name)}
                                    key={"board-" + item.board_name}
                                    selected={item.board_name === this.state.currentBoard}
                                > {item.board_name}
                                </MenuItem>))
                            }
                        </MenuList>

                        {this.state.currentBoard != "Select Board" ? (
                            <MenuList subheader={<ListSubheader>Profiles</ListSubheader>} className={classes.list} >
                                {this.state.profileNames.map(item => {
                                    return renderProfiles(false, item);
                                })}
                                <Divider />
                                {this.state.globalProfileNames.map(item => {
                                    return renderProfiles(true, item);
                                })}
                            </MenuList>
                        ) : ""}
                        <MenuList subheader={<ListSubheader>Board Options</ListSubheader>} className={classes.list} >
                            <MenuItem onClick={event => this.handleSelect(event, "AppBody-ReorderAudio")} disabled={optionsDisabled || profileDisabled} key="AppBody-ReorderAudio">Reorder Audio</MenuItem>
                            <MenuItem onClick={event => this.handleSelect(event, "AppBody-ReorderVideo")} disabled={optionsDisabled || profileDisabled} key="AppBody-ReorderVideo">Reorder Video</MenuItem>
                            <MenuItem onClick={event => this.handleSelect(event, "AppBody-ManageMedia")} disabled={optionsDisabled || profileDisabled} key="AppBody-ManageMedia">Remove Media</MenuItem>
                            <MenuItem onClick={event => this.handleSelect(event, "AppBody-LoadFromGDrive")} disabled={optionsDisabled || profileDisabled} key="AppBody-LoadFromGDrive">Add From G Drive</MenuItem>
                            <MenuItem onClick={event => this.handleSelect(event, "ActivateProfile")} disabled={optionsDisabled || profileDisabled} key="ActivateProfile">Activate This Profile</MenuItem>
                            <Divider />
                            <MenuItem disabled={optionsDisabled} onClick={event => this.handleSelect(event, "AppBody-BatteryHistory")} key="AppBody-BatteryHistory">Battery History</MenuItem>
                        </MenuList>
                        <MenuList subheader={<ListSubheader>Global Options</ListSubheader>} className={classes.list} >
                            <MenuItem onClick={event => this.handleSelect(event, "AppBody-CurrentStatuses")} key="AppBody-CurrentStatuses">Current Statuses</MenuItem>
                            <MenuItem onClick={event => this.handleSelect(event, "AppBody-AddProfile")} key="AppBody-AddProfile">Create Profile</MenuItem>
                            <MenuItem onClick={event => this.handleSelect(event, "AppBody-ManageProfiles")} key="AppBody-ManageProfiles">Manage Profiles</MenuItem>
                        </MenuList>
                    </div>
                </Drawer>
            </div>
        );
    }
}

GlobalMenu.propTypes = {
    classes: PropTypes.object.isRequired,
};

export default withStyles(menuStyles)(GlobalMenu);