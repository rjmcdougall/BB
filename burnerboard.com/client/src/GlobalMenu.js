import React, { Component } from 'react';
//import ReactDOM from 'react-dom';
import Menu, { SubMenu, Item as MenuItem } from 'rc-menu';
import 'rc-menu/assets/index.css';
//import animate from 'css-animation';




// const animation = {
//     enter(node, done) {
//         let height;
//         return animate(node, 'rc-menu-collapse', {
//             start() {
//                 height = node.offsetHeight;
//                 node.style.height = 0;
//             },
//             active() {
//                 node.style.height = `${height}px`;
//             },
//             end() {
//                 node.style.height = '';
//                 done();
//             },
//         });
//     },

//     appear() {
//         return this.enter.apply(this, arguments);
//     },

//     leave(node, done) {
//         return animate(node, 'rc-menu-collapse', {
//             start() {
//                 node.style.height = `${node.offsetHeight}px`;
//             },
//             active() {
//                 node.style.height = 0;
//             },
//             end() {
//                 node.style.height = '';
//                 done();
//             },
//         });
//     },
// };


 
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

const API = '/boards';

class GlobalMenu extends Component {

    constructor(props) {
        super(props);
        this.state = {
            boardNames: getBoardNames(),
        };

        this.onOpenChange = this.onOpenChange.bind(this);
        this.handleSelect = this.props.handleSelect.bind(this);
    }

    componentDidMount() {

        fetch(API)
          .then(response => response.json())
          .then(data => this.setState({
            boardNames: data.map(item => ({
                board_name: `${item.board_name}`,
            }))
          }))
          .catch(error => this.setState({ error}));
    
      }

    onOpenChange(value) {
        console.log('onOpenChange', value);
    };

    // handleSelect(info) {
    //     console.log(info);
    //     console.log(`selected ${info.key}`);
    // }

    render() {
        return (
            <div style={{ margin: 0 }}>
                <Menu mode="horizontal" openAnimation="slide-up" triggerSubMenuAction="click" onSelect={this.handleSelect} onOpenChange={this.onOpenChange}>
                    <SubMenu title={<span>Select Board</span>} key="1">
                        {this.state.boardNames.map(item => (
                            <MenuItem key={item.board_name}>{item.board_name}
                            </MenuItem>))
                        }
                    </SubMenu>
                    <SubMenu title={<span>Options</span>} key="2">
                        <MenuItem key="BatteryHistory">Battery History</MenuItem>
                        <MenuItem key="ReorderAudio">Reorder Audio</MenuItem>
                        <MenuItem key="ReorderVideo">Reorder Video</MenuItem>
                    </SubMenu>
                    <SubMenu title={<span>Global</span>} key="3">
                        <MenuItem key="CurrentStatuses">Current Statuses</MenuItem>
                    </SubMenu>
                </Menu>
            </div>
        );
    };
};

export default GlobalMenu;

