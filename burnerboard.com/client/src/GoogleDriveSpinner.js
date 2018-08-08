import React from "react";
import { PulseLoader } from "react-spinners";
import PropTypes from "prop-types";

class GoogleDriveSpinner extends React.Component {
	render() {

		if (this.props.loading) {
			return (
				<div className="sweet-loading">
					<PulseLoader
						color={"#123abc"}
						loading={true}
					/>
				</div>
			);

		}
		else {
			return (
				<div>{this.props.message}</div>
			);
		}
	}
}

GoogleDriveSpinner.propTypes = {
	message: PropTypes.string,
	loading: PropTypes.bool,
};

export default GoogleDriveSpinner;