import React, { Component } from 'react';
import ReactDataGrid from 'react-data-grid';
import 'bootstrap/dist/css/bootstrap.css';
import PropTypes from 'prop-types';

import Dialog, { DialogActions, DialogContent, DialogTitle } from 'material-ui/Dialog';

import ConfirmationDialogDemo from './ConfirmDeleteDialog'

// class PercentCompleteFormatter extends React.Component {

//     render() {
//         const percentComplete = this.props.value + '%';
//         return (
//             <div className="progress" style={{ marginTop: '20px' }}>
//                 <div className="progress-bar" role="progressbar" aria-valuenow="60" aria-valuemin="0" aria-valuemax="100" style={{ width: percentComplete }}>
//                     {percentComplete}
//                 </div>
//             </div>);
//     }
// }

const mediaArray = [
    {
        localName: "loading...",
        ordinal: ""
    }
]



class ManageMediaGrid extends React.Component {
    constructor(props, context) {
        super(props, context);

        this._columns = [
            {
                key: 'localName',
                name: 'File Name',
                sortable: true
            }
        ];

        this.state = {
            mediaArray: mediaArray,
            currentBoard: props.currentBoard,
            selectedRows: []
        };


        this.handleConfirm = this.handleConfirm.bind(this);
    }

    componentDidMount() {

        const API = '/boards/' + this.state.currentBoard + '/DownloadDirectoryJSON';

        fetch(API, {
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json',
                'x-access-token': window.sessionStorage.JWT,
            }
        })
            .then(response => response.json())
            .then(data => {

                console.log(JSON.stringify(data));

                var mediaArray = data.video
                    .filter(function (item) {
                        console.log (item);
                        return item.localName != null;
                    });

                mediaArray = mediaArray
                    .map(function (item) {
                        return {
                            id: `${item.localName}`,
                            mediaType: "video",
                            localName: `${item.localName}`,
                            ordinal: `${item.ordinal}`,
                        };
                    });

                mediaArray = mediaArray.concat(data.audio.map(function (item) {
                    return {
                        id: `${item.localName}`,
                        mediaType: "audio",
                        localName: `${item.localName}`,
                        ordinal: `${item.ordinal}`,
                    }
                }));


                this.setState({ "mediaArray": mediaArray });
            })
            .catch(error => this.setState({ error }));

    }

    rowGetter = (i) => {
        return this.state.mediaArray[i];
    };

    onRowSelect = (rows) => {
        this.setState({ selectedRows: rows });
    };

    handleGridSort = (sortColumn, sortDirection) => {
        const comparer = (a, b) => {
            if (sortDirection === 'ASC') {
                return (a[sortColumn] > b[sortColumn]) ? 1 : -1;
            } else if (sortDirection === 'DESC') {
                return (a[sortColumn] < b[sortColumn]) ? 1 : -1;
            }
        };

        const mediaArray = sortDirection === 'NONE' ? this.state.mediaArray.slice(0) : this.state.mediaArray.sort(comparer);

        this.setState({ mediaArray: mediaArray });
    };

    handleConfirm = () => {

        const manageMediaGrid = this;

        console.log("delete" + JSON.stringify(this.state.selectedRows));

        var API = '/boards/' + this.state.currentBoard + '/' + this.state.selectedRows[0].mediaType + '/' + this.state.selectedRows[0].localName;

        fetch(API, {
            method: 'DELETE',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json',
                'x-access-token': window.sessionStorage.JWT,
            }
        }
        )
            .then((res) => {

                if (!res.ok) {
                    res.json().then(function (json) {
                        console.log('error : ' + JSON.stringify(json));
                    });
                }
                else {

                    res.json().then(function (json) {

                        manageMediaGrid.setState({
                            mediaArray: manageMediaGrid.state.mediaArray.filter(function (item) {
                                return item.id !== manageMediaGrid.state.selectedRows[0].id;
                            })
                        });

                        console.log('success : ' + JSON.stringify(json));

                    });
                }
            })
            .catch((err) => {
                console.log('error : ' + err);
            });

    };

    render() {
        console.log("Rendering mediaArray");
        console.log(this.state.mediaArray);

        return (
            <div style={{"display":"flex", "flex-direction":"row"}}>
                <ReactDataGrid
                    onGridSort={this.handleGridSort}
                    enableRowSelect="single"
                    onRowSelect={this.onRowSelect}
                    columns={this._columns}
                    rowGetter={this.rowGetter}
                    rowsCount={this.state.mediaArray.length}
                    minHeight={500}
                    
                    midWidth={300}/>

                <ConfirmationDialogDemo handleConfirm={this.handleConfirm} />
            </div>);
    }
}

export default ManageMediaGrid;

