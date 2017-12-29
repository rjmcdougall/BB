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

        console.log("  in constructor " + props.currentBoard);
   
        super(props);
        this.state = {
            boardNames: getBoardNames(),
            currentBoard: props.currentBoard,
        };

        this.onOpenChange = this.onOpenChange.bind(this);
        this.handleSelect = this.props.handleSelect.bind(this);
    }

    componentWillReceiveProps(nextProps){
        this.setState({ currentBoard: nextProps.currentBoard });
    };

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

    render() {
        console.log("rendering in menu " + this.state.currentBoard);
 
        var optionsDisabled=false;
        if(this.state.currentBoard=="Select Board"){
            optionsDisabled=true;
        };

        return (
            <div style={{ margin: 0 }}>
                <Menu mode="horizontal" openAnimation="slide-up" triggerSubMenuAction="click" onSelect={this.handleSelect} onOpenChange={this.onOpenChange}>
                    <SubMenu title={this.state.currentBoard} key="1">
                        {this.state.boardNames.map(item => (
                            <MenuItem key={item.board_name}>{item.board_name}
                            </MenuItem>))
                        }
                    </SubMenu>
                    <SubMenu disabled={optionsDisabled} title={<span>Options</span>} key="2">
                        <MenuItem key="AppBody-BatteryHistory">Battery History</MenuItem>
                        <MenuItem key="AppBody-ReorderAudio">Reorder Audio</MenuItem>
                        <MenuItem disabled={true} key="AppBody-ReorderVideo">Reorder Video</MenuItem>
                        <MenuItem disabled={true} key="AppBody-UploadFromDesktop">Upload From Desktop</MenuItem>
                        <MenuItem disabled={true}  key="AppBody-LoadFromGDrive">Load From G Drive</MenuItem>
                        <MenuItem disabled={true} key="AppBody-LoadFromDropBox">Load From DropBox</MenuItem>
                    
                    </SubMenu>
                    <SubMenu title={<span>Global</span>} key="3">
                        <MenuItem key="AppBody-CurrentStatuses">Current Statuses</MenuItem>
                        <MenuItem disabled={true} key="AppBody-MapEm">Map 'Em</MenuItem>
                        
                    </SubMenu>
                </Menu>
            </div>
        );
    };
};

export default GlobalMenu;

