import React, { Component } from 'react';
import ReactDataGrid from 'react-data-grid';
import 'bootstrap/dist/css/bootstrap.css'; 
 
const batteryHistoryJSON = {
    batteryHistory: [
        {
            board_name: "loading...",
            BatteryLevel: 0,
            TimeBucket: "loading..."
        }
    ]
};

class BatteryHistoryGrid extends Component {
    constructor(props, context) {
        super(props, context);

        this._columns = [
            {
                key: 'board_name',
                name: 'Board Name'
            },
            {
                key: 'BatteryLevel',
                name: 'Battery Level'
            },
            {
                key: 'TimeBucket',
                name: 'Time Bucket'
            }
        ];

        this.state = {
            boardData: batteryHistoryJSON.batteryHistory,
            currentBoard: props.currentBoard,
        };

    }

    componentDidMount() {

        const API = '/boards/' + this.state.currentBoard +'/BatteryHistory';

        fetch(API, {
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json',
                'authorization': window.sessionStorage.JWT,
            }
        })
            .then(response => response.json())
            .then(data => this.setState({
                boardData: data.map(item => ({
                    board_name: `${item.board_name}`,
                    BatteryLevel: `${item.BatteryLevel}`,
                    TimeBucket: `${item.TimeBucket}`,
                }))
            }))
            .catch(error => this.setState({ error }));

    }
 
    rowGetter = (i) => {
        return this.state.boardData[i];
    };

    render() {
        console.log("Rendering BoardGrid");
        console.log(this.state.boardData);

        return (
            <ReactDataGrid
                columns={this._columns}
                rowGetter={this.rowGetter}
                rowsCount={this.state.boardData.length}
                minHeight={500} />);
    }
}

export default BatteryHistoryGrid;

