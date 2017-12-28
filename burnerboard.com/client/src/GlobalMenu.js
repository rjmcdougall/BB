import React, { Component } from 'react';
//import ReactDOM from 'react-dom';
import Menu, { SubMenu, Item as MenuItem, Divider } from 'rc-menu';
import 'rc-menu/assets/index.css';
//import animate from 'css-animation';


function handleSelect(info) {
    console.log(info);
    console.log(`selected ${info.key}`);
}

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

function onOpenChange(value) {
    console.log('onOpenChange', value);
};

const commonMenu = (<Menu onSelect={handleSelect} onOpenChange={onOpenChange}>
    <SubMenu title={<span>Boards</span>} key="1">
        <MenuItem key="1-1">candy</MenuItem>
        <MenuItem key="1-2">monaco</MenuItem>
        <MenuItem key="1-3">pegasus</MenuItem>
    </SubMenu>
    <SubMenu title={<span>Options</span>} key="4">
        <MenuItem key="2-1">Current Status</MenuItem>
        <MenuItem key="2-2">Reorder Audio</MenuItem>
        <MenuItem key="2-3">Reorder Video</MenuItem>
    </SubMenu>
</Menu>);

const horizontalMenu2 = React.cloneElement(commonMenu, {
    mode: 'horizontal',
    openAnimation: 'slide-up',
    triggerSubMenuAction: 'click',
});

class GlobalMenu extends Component {

    render() {
        return (
            <div>
                <div style={{ margin: 20, width: 800 }}>{horizontalMenu2}</div>
            </div>
        );
    };
};

export default GlobalMenu;

