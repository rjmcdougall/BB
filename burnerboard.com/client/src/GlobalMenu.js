import React, { Component } from 'react';
import Menu, { Divider, SubMenu, Item as MenuItem } from 'rc-menu';
import 'rc-menu/assets/index.css';

const boardsJSON = {
    boards: [
        {
            board_name: "loading boards...",
            last_seen: "12/18/17 05:57:01",
            is_online: false,
            battery_level: 0
        }
    ]
};

const getBoardNames = function () {
    return boardsJSON.boards.map(item => ({
        board_name: `${item.board_name}`,
    }))
};


class GlobalMenu extends Component {

    constructor(props) {

        console.log("  in constructor " + props.currentBoard);

        super(props);
        this.state = {
            boardNames: getBoardNames(),
            profileNames: ["select Board"],
            globalProfileNames: ["select Board"],
            currentProfile: "Select Profile",
            currentBoard: props.currentBoard,
            activeProfile: props.activeProfile,
        };

        this.onOpenChange = this.onOpenChange.bind(this);
        this.handleSelect = this.props.handleSelect.bind(this);


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

    onOpenChange(value) {
        console.log('onOpenChange', value);
    };

    render() {

        console.log("global profiles: " + JSON.stringify(this.state.globalProfileNames));
        console.log("board profiles: " + JSON.stringify(this.state.profileNames));

        var renderProfiles = (inGlobalBlock, item) => {

            console.log("active profie: " + this.state.activeProfile);
            console.log("activeProfileIsGlobal: " + this.state.activeProfileIsGlobal)
            console.log("in global block: " + inGlobalBlock);
            console.log(JSON.stringify(item));

            if (inGlobalBlock) {
                if (this.state.activeProfile === item.profile_name && this.state.activeProfileIsGlobal === inGlobalBlock)
                    return (
                        <MenuItem key={"globalProfile-" + item.profile_name}>
                            {"* " + item.profile_name}
                        </MenuItem>
                    );
                else
                    return (
                        <MenuItem key={"globalProfile-" + item.profile_name}>
                            {item.profile_name}
                        </MenuItem>
                    );
            }
            else {
                if (this.state.activeProfile === item.profile_name && this.state.activeProfileIsGlobal === inGlobalBlock)
                    return (
                        <MenuItem key={"profile-" + item.profile_name}>
                            {"* " + item.profile_name}
                        </MenuItem>
                    );
                else
                    return (
                        <MenuItem key={"profile-" + item.profile_name}>
                            {item.profile_name}
                        </MenuItem>
                    );
            }
        }

        var renderTitle = () => {
            if (this.state.activeProfile === this.state.currentProfile)
                return "* " + this.state.currentProfile;
            else
                return this.state.currentProfile; 
        }

        var optionsDisabled = false;
        var profileDisabled = false;
        if (this.state.currentBoard === "Select Board") {
            optionsDisabled = true;
        };
        console.log("im about to render and this is my profle " + this.state.currentProfile)
        if (this.state.currentProfile === "Select Profile") {
            profileDisabled = true;
        };
        return (
            <div style={{ margin: 0 }}>
                <Menu mode="horizontal" openAnimation="slide-up" triggerSubMenuAction="hover" onSelect={this.handleSelect} onOpenChange={this.onOpenChange}>
                    <SubMenu title={this.state.currentBoard} key="1">
                        {this.state.boardNames.map(item => (
                            <MenuItem key={"board-" + item.board_name}> {item.board_name}
                            </MenuItem>))
                        }
                    </SubMenu>
                    <SubMenu disabled={optionsDisabled} title={renderTitle()} key="4">
                        {this.state.profileNames.map(item => {
                            return renderProfiles(false, item);
                        })}
                        <Divider />
                        {this.state.globalProfileNames.map(item => {
                            return renderProfiles(true, item);
                        })}
                    </SubMenu>
                    <SubMenu disabled={optionsDisabled} title={<span>Options</span>} key="2">
                        <MenuItem disabled={optionsDisabled || profileDisabled} key="AppBody-ReorderAudio">Reorder Audio</MenuItem>
                        <MenuItem disabled={optionsDisabled || profileDisabled} key="AppBody-ReorderVideo">Reorder Video</MenuItem>
                        <MenuItem disabled={optionsDisabled || profileDisabled} key="AppBody-ManageMedia">Remove Media</MenuItem>
                        <MenuItem disabled={optionsDisabled || profileDisabled} key="AppBody-LoadFromGDrive">Add From G Drive</MenuItem>
                        <MenuItem disabled={optionsDisabled || profileDisabled} key="ActivateProfile">Activate This Profile</MenuItem>
                        <Divider />
                        <MenuItem key="AppBody-BatteryHistory">Battery History</MenuItem>
                    </SubMenu>
                    <SubMenu title={<span>Global</span>} key="3">
                        <MenuItem key="AppBody-CurrentStatuses">Current Statuses</MenuItem>
                        <MenuItem key="AppBody-AddProfile">Create Profile</MenuItem>
                        <MenuItem key="AppBody-ManageProfiles">Manage Profiles</MenuItem>
                    </SubMenu>
                </Menu>
            </div>
        );
    };
};

export default GlobalMenu;

