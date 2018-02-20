import React from 'react';
import PropTypes from 'prop-types';
import { withStyles } from 'material-ui/styles';
import AppBar from 'material-ui/AppBar';
import Toolbar from 'material-ui/Toolbar';
import Typography from 'material-ui/Typography';
import IconButton from 'material-ui/IconButton';
import MenuIcon from 'material-ui-icons/Menu';
import CheckCircle from 'material-ui-icons/CheckCircle';
import MenuGlobal from 'material-ui-icons/Language';
import Drawer from 'material-ui/Drawer';
import Divider from 'material-ui/Divider';
import ListSubheader from 'material-ui/List/ListSubheader';

import { MenuList, MenuItem } from 'material-ui/Menu';

const menuStyles = {
    root: {
        width: '100%',
    },
    flex: {
        flex: 1,
    },
    menuButtonLeft: {
        marginLeft: -12,
        marginRight: 20,
    },
    menuButtonRight: {
        marginLeft: 1,
        marginRight: 1,
    },
    list: {
        width: 250,
    },
    listSubheader: {
        backgroundColor: '#3f50b5',//'#757ce8',
        color: 'white',
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
            globalDrawerIsOpen: props.globalDrawerIsOpen,
            boardNames: [{ board_name: "loading boards..." }],
            profileNames: ["select Board"],
            globalProfileNames: ["select Board"],
            currentProfile: "Select Profile",
            activeProfile: props.activeProfile,
            currentBoard: props.currentBoard,
            currentAppBody: props.currentAppBody,
        };

        this.handleSelect = this.props.handleSelect.bind(this);

    }

    toggleDrawer = (open) => () => {
        this.setState({
            drawerIsOpen: open,
        });


    };

    toggleGlobalDrawer = (open) => () => {
        this.setState({
            globalDrawerIsOpen: open,
        });
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

    componentWillReceiveProps(nextProps) {

        console.log("FORCE RENDER: " + nextProps.forceRerendder)
        var API = '/boards/' + nextProps.currentBoard + '/profiles/';

        var profiles;
        var globalProfiles;

        console.log("GET ALL PROFILES FOR BOARD API: " + API);
        fetch(API, {
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json',
                'authorization': window.sessionStorage.JWT,
            }
        })
            .then(response => response.json())
            .then(data => {

                console.log("returned these profiles: " + JSON.stringify(data));

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

                                this.setState({
                                    currentBoard: nextProps.currentBoard,
                                    profileNames: profiles,
                                    globalProfileNames: globalProfiles,
                                    currentProfile: nextProps.currentProfile,
                                    activeProfile: data3[0].profile,
                                    activeProfileIsGlobal: data3[0].isProfileGlobal,
                                    currentAppBody: nextProps.currentAppBody,
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

            if (inGlobalBlock) {
                if (this.state.activeProfile === item.profile_name && this.state.activeProfileIsGlobal === inGlobalBlock)
                    return (
                        <MenuItem onClick={event => this.handleSelect(event, "globalProfile-" + item.profile_name)}
                            key={"globalProfile-" + item.profile_name}
                            selected={item.profile_name === this.state.currentProfile}
                        >  <CheckCircle /> &nbsp; {item.profile_name} &nbsp; <MenuGlobal />
                        </MenuItem>
                    );
                else
                    return (
                        <MenuItem onClick={event => this.handleSelect(event, "globalProfile-" + item.profile_name)}
                            key={"globalProfile-" + item.profile_name}
                            selected={item.profile_name === this.state.currentProfile}
                        > {item.profile_name} &nbsp; <MenuGlobal />
                        </MenuItem>
                    );
            }
            else {
                if (this.state.activeProfile === item.profile_name && this.state.activeProfileIsGlobal === inGlobalBlock)
                    return (
                        <MenuItem onClick={event => this.handleSelect(event, "profile-" + item.profile_name)}
                            key={"profile-" + item.profile_name}
                            selected={item.profile_name === this.state.currentProfile}
                        > <CheckCircle /> &nbsp; {item.profile_name}
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
                        <IconButton onClick={this.toggleDrawer(true)} className={classes.menuButtonLeft} color="inherit" aria-label="Menu">
                            <MenuIcon />
                        </IconButton>
                        <Typography color="inherit" className={classes.flex}>
                            {this.state.currentBoard} {this.state.currentProfile !== "Select Profile" ? " - " + renderProfileTitle() : ""}
                        </Typography>

                        <IconButton onClick={this.toggleGlobalDrawer(true)} className={classes.menuButtonRight} color="inherit" aria-label="Global" >
                            <MenuGlobal />
                        </IconButton>

                    </Toolbar>
                </AppBar>
                <Drawer open={this.state.drawerIsOpen} onClose={this.toggleDrawer(false)}>
                    <div
                        tabIndex={0}
                        role="button"

                        onKeyDown={this.toggleDrawer(false)}
                    >
                        <MenuList subheader={<ListSubheader className={classes.listSubheader} disableSticky={true} >Boards</ListSubheader>} className={classes.list} >
                            {this.state.boardNames.map(item => (
                                <MenuItem onClick={event => this.handleSelect(event, "board-" + item.board_name)}
                                    key={"board-" + item.board_name}
                                    selected={item.board_name === this.state.currentBoard}
                                > {item.board_name}
                                </MenuItem>))
                            }
                        </MenuList>

                        {this.state.currentBoard !== "Select Board" ? (
                            <MenuList subheader={<ListSubheader className={classes.listSubheader} disableSticky={true} >Profiles</ListSubheader>} className={classes.list} >
                                {this.state.profileNames.map(item => {
                                    return renderProfiles(false, item);
                                })}
                                <Divider />
                                {this.state.globalProfileNames.map(item => {
                                    return renderProfiles(true, item);
                                })}
                            </MenuList>
                        ) :
                            <MenuList disabled={true} subheader={<ListSubheader className={classes.listSubheader} disableSticky={true} >Profiles</ListSubheader>} className={classes.list} >
                                {/* {this.state.globalProfileNames.map(item => {
                                    return renderProfiles(true, item);
                                })} */}
                            </MenuList>
                        }
                        <MenuList subheader={<ListSubheader className={classes.listSubheader} disableSticky={true} >Media</ListSubheader>} className={classes.list} >
                            <MenuItem selected={"AppBody-ReorderAudio" === this.state.currentAppBody} onClick={event => this.handleSelect(event, "AppBody-ReorderAudio")} disabled={optionsDisabled || profileDisabled} key="AppBody-ReorderAudio">Reorder Audio</MenuItem>
                            <MenuItem selected={"AppBody-ReorderVideo" === this.state.currentAppBody} onClick={event => this.handleSelect(event, "AppBody-ReorderVideo")} disabled={optionsDisabled || profileDisabled} key="AppBody-ReorderVideo">Reorder Video</MenuItem>
                            <MenuItem selected={"AppBody-ManageMedia" === this.state.currentAppBody} onClick={event => this.handleSelect(event, "AppBody-ManageMedia")} disabled={optionsDisabled || profileDisabled} key="AppBody-ManageMedia">Remove Media</MenuItem>
                            <MenuItem selected={"AppBody-LoadFromGDrive" === this.state.currentAppBody} onClick={event => this.handleSelect(event, "AppBody-LoadFromGDrive")} disabled={optionsDisabled || profileDisabled} key="AppBody-LoadFromGDrive">Add From G Drive</MenuItem>
                            <MenuItem selected={"AppBody-ActivateProfile" === this.state.currentAppBody} onClick={event => this.handleSelect(event, "AppBody-ActivateProfile")} disabled={optionsDisabled || profileDisabled} key="AppBody-ActivateProfile">Activate This Profile</MenuItem>
                        </MenuList>
                        <MenuList subheader={<ListSubheader className={classes.listSubheader} disableSticky={true} >Other</ListSubheader>} className={classes.list} >
                            <MenuItem disabled={optionsDisabled} onClick={event => this.handleSelect(event, "AppBody-BatteryHistory")} key="AppBody-BatteryHistory">Battery History</MenuItem>
                        </MenuList>
                    </div>
                </Drawer>
                <Drawer anchor="right" open={this.state.globalDrawerIsOpen} onClose={this.toggleGlobalDrawer(false)}>
                    <div
                        tabIndex={0}
                        role="button"
                        onKeyDown={this.toggleGlobalDrawer(false)}
                    >
                        <MenuList subheader={<ListSubheader className={classes.listSubheader} disableSticky={true} >Global Options</ListSubheader>} className={classes.list} >
                            <MenuItem selected={"AppBody-CurrentStatuses" === this.state.currentAppBody} onClick={event => { this.toggleGlobalDrawer(false); this.handleSelect(event, "AppBody-CurrentStatuses") }} key="AppBody-CurrentStatuses">Current Statuses</MenuItem>
                            <MenuItem selected={"AppBody-AddProfile" === this.state.currentAppBody} onClick={event => this.handleSelect(event, "AppBody-AddProfile")} key="AppBody-AddProfile">Create Profile</MenuItem>
                            <MenuItem selected={"AppBody-ManageProfiles" === this.state.currentAppBody} onClick={event => this.handleSelect(event, "AppBody-ManageProfiles")} key="AppBody-ManageProfiles">Manage Profiles</MenuItem>
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