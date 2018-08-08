import React from "react";
import PropTypes from "prop-types";
import { withStyles } from "material-ui/styles";
import AppBar from "material-ui/AppBar";
import Toolbar from "material-ui/Toolbar";
import Typography from "material-ui/Typography";
import IconButton from "material-ui/IconButton";
import MenuIcon from "material-ui-icons/Menu";
import HelpIcon from "material-ui-icons/Help";
import CheckCircle from "material-ui-icons/CheckCircle";
import MenuGlobal from "material-ui-icons/Language";
import Drawer from "material-ui/Drawer";
import Divider from "material-ui/Divider";
import ListSubheader from "material-ui/List/ListSubheader";
import { MenuList, MenuItem } from "material-ui/Menu";

const menuStyles = {
    root: {
        width: "100%",
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
        backgroundColor: "#3f50b5",//"#757ce8",
        color: "white",
    },
    listFull: {
        width: "auto",
    },
};
class GlobalMenu extends React.Component {

    constructor(props) {
        super(props);

        this.state = {  
            showBoards: false,
            showTesters: false,
            showDevices: false,
            showProfiles: false,
            showMedia: false,
        };
        
    }

    handleInstructions = event => {
        // eslint-disable-next-line no-console
        console.log(event.currentTarget.getAttribute("dataurl"));
        window.open(event.currentTarget.getAttribute("dataurl"));
    }
 
    render() {
        const { classes } = this.props;

        var optionsDisabled = false;
        var profileDisabled = false;
        if (this.props.currentBoard === "Select Board") {
            optionsDisabled = true;
        };

        if (this.props.currentProfile === "Select Profile") {
            profileDisabled = true;
        };

        var renderProfileTitle = () => {
            return this.props.currentProfile;
        }

        return (
            <div className={classes.root}>
                <AppBar position="static">
                    <Toolbar>
                        <IconButton onClick={() => this.props.toggleDrawer(true)} className={classes.menuButtonLeft} color="inherit" aria-label="Menu">
                            <MenuIcon />
                        </IconButton>
                        <Typography color="inherit" className={classes.flex}>
                            {this.props.currentBoard} {this.props.currentProfile !== "Select Profile" ? " - " + renderProfileTitle() : ""}
                        </Typography>
                        <IconButton onClick={() => this.props.toggleGlobalDrawer(true)} className={classes.menuButtonRight} color="inherit" aria-label="Global" >
                            <MenuGlobal />
                        </IconButton>
                        <IconButton onClick={event => this.handleInstructions(event)} dataurl={"https://docs.google.com/document/d/1rbPOly_-OwgPFjdC9Gr13xeY-RzDLHVyPivl0Hv7Ss4/edit?usp=sharing"} className={classes.menuButtonRight} color="inherit" aria-label="Help">
                            <HelpIcon />
                        </IconButton>
                    </Toolbar>
                </AppBar>
                <Drawer open={this.props.drawerIsOpen} onClose={() => this.props.toggleDrawer(false)} >
                    <div
                        tabIndex={0}
                        role="button"

                        onKeyDown={() => this.props.toggleDrawer(false)}
                    >
                        <MenuList subheader={<ListSubheader className={classes.listSubheader} disableSticky={true} onClick={event => this.setState({ showBoards: !this.state.showBoards })}>Boards</ListSubheader>} className={classes.list} >
                            {this.props.boardNames.filter((item) => { return item.type === "board" }).map(item => (
                                <MenuItem onClick={event => { this.props.onSelectBoard(event, "board-" + item.board_name); this.setState({ showProfiles: true }) }}
                                    key={"board-" + item.board_name}
                                    selected={item.board_name === this.props.currentBoard}
                                    style={{ display: this.state.showBoards ? "block" : "none" }}
                                > {item.board_name}
                                </MenuItem>))
                            }
                        </MenuList>
                        <MenuList subheader={<ListSubheader className={classes.listSubheader} disableSticky={true} onClick={event => this.setState({ showDevices: !this.state.showDevices })}>Devices</ListSubheader>} className={classes.list} >
                            {this.props.boardNames.filter((item) => { return item.type === "device" }).map(item => (
                                <MenuItem onClick={event => { this.props.onSelectBoard(event, "board-" + item.board_name); this.setState({ showProfiles: true }) }}
                                    key={"board-" + item.board_name}
                                    selected={item.board_name === this.props.currentBoard}
                                    style={{ display: this.state.showDevices ? "block" : "none" }}
                                > {item.board_name}
                                </MenuItem>))
                            }
                        </MenuList>
                        <MenuList subheader={<ListSubheader className={classes.listSubheader} disableSticky={true} onClick={event => this.setState({ showTesters: !this.state.showTesters })}>Testers</ListSubheader>} className={classes.list} >
                            {this.props.boardNames.filter((item) => { return item.type === "tester" }).map(item => (
                                <MenuItem onClick={event => { this.props.onSelectBoard(event, "board-" + item.board_name); this.setState({ showProfiles: true }) }}
                                    key={"board-" + item.board_name}
                                    selected={item.board_name === this.props.currentBoard}
                                    style={{ display: this.state.showTesters ? "block" : "none" }}
                                > {item.board_name}
                                </MenuItem>))
                            }
                        </MenuList>

                        <MenuList subheader={<ListSubheader className={classes.listSubheader} disableSticky={true} onClick={event => this.setState({ showProfiles: !this.state.showProfiles })}>Profiles</ListSubheader>} className={classes.list} >
                            {
                                this.props.profileNames.map(item => {
                                    return (<MenuItem onClick={event => { this.props.onSelectProfile(event, "profile-" + item.profile_name); this.setState({ showMedia: true }); }}
                                        key={"profile-" + item.profile_name}
                                        selected={item.profile_name === this.props.currentProfile}
                                        style={{ display: this.state.showProfiles ? "block" : "none" }} >
                                        {((this.props.activeProfiles[0].profile === item.profile_name || this.props.activeProfiles[1].profile === item.profile_name)) ? <CheckCircle /> : ""}
                                        &nbsp; {item.profile_name}
                                    </MenuItem>);
                                })
                            }
                            <Divider />
                            {
                                this.props.globalProfileNames.map(item => {
                                    return (
                                        <MenuItem onClick={event => { this.props.onSelectProfile(event, "globalProfile-" + item.profile_name); this.setState({ showMedia: true }); }}
                                            key={"globalProfile-" + item.profile_name}
                                            selected={item.profile_name === this.props.currentProfile}
                                            style={{ display: this.state.showProfiles ? "block" : "none" }}>
                                            {((this.props.activeProfiles[0].profile === item.profile_name || this.props.activeProfiles[1].profile === item.profile_name)) ? <CheckCircle /> : ""}
                                            &nbsp; {item.profile_name} &nbsp;<MenuGlobal />
                                        </MenuItem>);
                                })
                            }
                        </MenuList>

                        <MenuList subheader={<ListSubheader className={classes.listSubheader} disableSticky={true} onClick={event => this.setState({ showMedia: !this.state.showMedia })}>Media</ListSubheader>} className={classes.list} >
                            <MenuItem selected={"AppBody-ReorderAudio" === this.props.currentAppBody}
                                onClick={event => this.props.onSelectAppBody(event, "AppBody-ReorderAudio")}
                                style={{ display: this.state.showMedia && !optionsDisabled && !profileDisabled ? "block" : "none" }}
                                key="AppBody-ReorderAudio">Reorder Audio</MenuItem>
                            <MenuItem selected={"AppBody-ReorderVideo" === this.props.currentAppBody}
                                onClick={event => this.props.onSelectAppBody(event, "AppBody-ReorderVideo")}
                                style={{ display: this.state.showMedia && !optionsDisabled && !profileDisabled ? "block" : "none" }}
                                key="AppBody-ReorderVideo">Reorder Video</MenuItem>
                            <MenuItem selected={"AppBody-ManageMedia" === this.props.currentAppBody}
                                onClick={event => this.props.onSelectAppBody(event, "AppBody-ManageMedia")}
                                style={{ display: this.state.showMedia && !optionsDisabled && !profileDisabled ? "block" : "none" }}
                                key="AppBody-ManageMedia">Remove Media</MenuItem>
                            <MenuItem selected={"AppBody-LoadFromGDrive" === this.props.currentAppBody}
                                onClick={event => this.props.onSelectAppBody(event, "AppBody-LoadFromGDrive")}
                                style={{ display: this.state.showMedia && !optionsDisabled && !profileDisabled ? "block" : "none" }}
                                key="AppBody-LoadFromGDrive">Add From G Drive</MenuItem>
                            <MenuItem selected={"AppBody-ActivateProfile" === this.props.currentAppBody}
                                onClick={event => this.props.onSelectAppBody(event, "AppBody-ActivateProfile")}
                                style={{ display: this.state.showMedia && !optionsDisabled && !profileDisabled ? "block" : "none" }}
                                key="AppBody-ActivateProfile">Activate This Profile</MenuItem>
                        </MenuList>
                        <MenuList subheader={<ListSubheader className={classes.listSubheader} disableSticky={true} >Other</ListSubheader>} className={classes.list} >
                            <MenuItem disabled={optionsDisabled} onClick={event => this.props.onSelectAppBody(event, "AppBody-BatteryHistory")} key="AppBody-BatteryHistory">Battery History</MenuItem>
                        </MenuList>
                    </div>
                </Drawer>
                <Drawer anchor="right" open={this.props.globalDrawerIsOpen} onClose={() => this.props.toggleGlobalDrawer(false)}>
                    <div
                        tabIndex={0}
                        role="button"
                        onKeyDown={() => this.props.toggleGlobalDrawer(false)}
                    >
                        <MenuList subheader={<ListSubheader className={classes.listSubheader} disableSticky={true} >Global Options</ListSubheader>} className={classes.list} >
                            <MenuItem selected={"AppBody-CurrentStatuses" === this.props.currentAppBody} onClick={event => { this.props.toggleGlobalDrawer(false); this.props.onSelectAppBody(event, "AppBody-CurrentStatuses") }} key="AppBody-CurrentStatuses">Current Statuses</MenuItem>
                            <MenuItem selected={"AppBody-AddProfile" === this.props.currentAppBody} onClick={event => this.props.onSelectAppBody(event, "AppBody-AddProfile")} key="AppBody-AddProfile">Create Profile</MenuItem>
                            <MenuItem selected={"AppBody-ManageProfiles" === this.props.currentAppBody} onClick={event => this.props.onSelectAppBody(event, "AppBody-ManageProfiles")} key="AppBody-ManageProfiles">Manage Profiles</MenuItem>
                        </MenuList>
                    </div>
                </Drawer>
            </div>
        );
    }
}

GlobalMenu.propTypes = {
    classes: PropTypes.object.isRequired,
    onSelectAppBody: PropTypes.func,
    onSelectProfile: PropTypes.func,
    onSelectBoard: PropTypes.func,
    activeProfiles: PropTypes.array,
    toggleDrawer: PropTypes.func,
    toggleGlobalDrawe: PropTypes.func,
    drawerIsOpen: PropTypes.bool,
    globalDrawerIsOpen: PropTypes.bool,
    currentBoard: PropTypes.string,
    currentAppBody: PropTypes.string,
    currentProfile: PropTypes.string,
    boardNames: PropTypes.array,
    profileNames: PropTypes.array,
    globalProfileNames: PropTypes.array,
};
 
export default withStyles(menuStyles)(GlobalMenu);