import React from 'react';
import { PulseLoader } from 'react-spinners';

class GoogleDriveSpinner extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            loading: props.loading,
            message: props.message,
        }
    }

    componentWillReceiveProps(nextProps){
        this.setState({
            loading: nextProps.loading,
            message: nextProps.message,
        });
    }

    render() {
        
        if(this.state.loading){
            return (
                <div className='sweet-loading'>
                    <PulseLoader
                        color={'#123abc'}
                        loading={true}
                    />
          </div>
            )
           
        }
        else {
            return (
                <div>{this.state.message}</div>
            )
        }

    }
}

export default GoogleDriveSpinner;