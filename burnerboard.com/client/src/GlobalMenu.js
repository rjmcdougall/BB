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
            currentProfile: "Select Profile",
            currentBoard: props.currentBoard,
        };

        this.onOpenChange = this.onOpenChange.bind(this);
        this.handleSelect = this.props.handleSelect.bind(this);

    }

    componentWillReceiveProps(nextProps) {

        console.log("got props current board: " + nextProps.currentBoard);
        console.log("got props current profile: " + nextProps.currentProfile);

        const API = '/boards/' + nextProps.currentBoard + '/profiles/';

        var profiles;

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

                this.setState({
                    currentBoard: nextProps.currentBoard,
                    profileNames: profiles,
                    currentProfile: nextProps.currentProfile,
                });

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
        console.log("rendering in menu " + this.state.currentBoard);

        var optionsDisabled = false;
        var profileDisabled = false;
        if (this.state.currentBoard == "Select Board") {
            optionsDisabled = true;
        };
        console.log("im about to render and this is my profle " + this.state.currentProfile)
        if (this.state.currentProfile == "Select Profile") {
            profileDisabled = true;
        };
        return (
            <div style={{ margin: 0 }}>
                <Menu mode="horizontal" openAnimation="slide-up" triggerSubMenuAction="hover" onSelect={this.handleSelect} onOpenChange={this.onOpenChange}>
                    <SubMenu title={this.state.currentBoard} key="1">
                        {this.state.boardNames.map(item => (
                            <MenuItem key={"board-" + item.board_name}>{item.board_name}
                            </MenuItem>))
                        }
                    </SubMenu>
                    <SubMenu disabled={optionsDisabled} title={this.state.currentProfile} key="4">
                        {this.state.profileNames.map(item => (
                            <MenuItem key={"profile-" + item.profile_name}>{item.profile_name}
                            </MenuItem>))
                        }
                    </SubMenu>
                    <SubMenu disabled={optionsDisabled} title={<span>Options</span>} key="2">
                        <MenuItem disabled={optionsDisabled || profileDisabled} key="AppBody-ReorderAudio">Reorder Audio</MenuItem>
                        <MenuItem disabled={optionsDisabled || profileDisabled} key="AppBody-ReorderVideo">Reorder Video</MenuItem>
                        <MenuItem disabled={optionsDisabled || profileDisabled} key="AppBody-ManageMedia">Remove Media</MenuItem>
                        <MenuItem disabled={optionsDisabled || profileDisabled} key="AppBody-LoadFromGDrive">Add From G Drive</MenuItem>
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

