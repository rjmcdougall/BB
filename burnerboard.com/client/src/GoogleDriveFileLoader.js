import React, { Component } from 'react';
import GoogleDriveMediaPicker from './GoogleDriveMediaPicker';
import GoogleDriveSpinner from './GoogleDriveSpinner';

class GoogleDriveFileLoader extends Component {

    constructor(props) {

        console.log("  in constructor " + props.currentBoard);

        super(props);
        this.state = {
            currentBoard: props.currentBoard,
            spinnerActive: false,
        };

       // this.setSpinnerActive = this.setSpinnerActive.bind(this);

    }

    // myCallback = (dataFromChild) => {
    //     this.setState({ listDataFromChild: dataFromChild });
    // }

    setSpinnerActive = (spinnerActive) => {
        this.setState({spinnerActive: spinnerActive});
    }

    render() {

        return (
            <div>
                <GoogleDriveMediaPicker setSpinnerActive = {this.setSpinnerActive} currentBoard={this.state.currentBoard} />
                <GoogleDriveSpinner loading={this.state.spinnerActive}/>
            </div>
        );
    }
}

export default GoogleDriveFileLoader;

