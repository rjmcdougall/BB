import React from 'react';
import PropTypes from 'prop-types';
import Button from 'material-ui/Button';
import Dialog, {
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  withMobileDialog,
} from 'material-ui/Dialog';

class ResponsiveDialog extends React.Component {

  constructor(props) {
  
    super(props);
    this.state = {
      open: false,
      selectedRows: this.props.selectedRows
    };


    this.handleConfirm = this.props.handleConfirm.bind(this);

  }

  handleClickOpen = () => {
    this.setState({ open: true });
  };

  handleClose = () => {
    this.setState({ open: false });
  };
 

  render() {
    const { fullScreen } = this.props;

    return (
      <div>

    <img onClick={this.handleClickOpen} height="48" width="48" src={require('./images/trash-200.png')} />  

        <Dialog
          //fullScreen={fullScreen}
          open={this.state.open}
          onClose={this.handleClose}
          aria-labelledby="responsive-dialog-title"
        >
          <DialogTitle id="responsive-dialog-title">{"Warning"}</DialogTitle>
          <DialogContent>
            <DialogContentText>
              Deleting a file cannot be undone. 
            </DialogContentText>
          </DialogContent>
          <DialogActions>
            <Button onClick={this.handleClose} color="primary">
              Cancel
            </Button>
            <Button onClick={(event) => { this.handleConfirm(); this.handleClose();}} color="primary" autoFocus>
              Confirm
            </Button>
          </DialogActions>
        </Dialog>
      </div>
    );
  }
}

ResponsiveDialog.propTypes = {
  fullScreen: PropTypes.bool.isRequired,
};

export default withMobileDialog()(ResponsiveDialog);