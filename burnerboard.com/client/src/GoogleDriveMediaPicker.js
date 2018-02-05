import React, { Component } from 'react'
import GooglePicker from 'react-google-picker';
import GoogleDriveSpinner from './GoogleDriveSpinner';

const CLIENT_ID = '845419422405-4e826kofd0al1npjaq6tijn1f3imk43p.apps.googleusercontent.com';
const DEVELOPER_KEY = 'AIzaSyD2LV75zLhuSnHOblyAYz6sUZ-o94dSARQ';
const SCOPE = ['https://www.googleapis.com/auth/drive.readonly'];

class GoogleDriveMediaPicker extends Component {

    constructor(props) {

        super(props);
        this.state = {
            currentBoard: props.currentBoard,
            currentProfile: props.currentProfile,
            jsonResults: "",
            errorInfo: "",
            spinnerActive: false,
            successMessage: "",
        };

    }

    componentWillReceiveProps(nextProps){
        this.setState( {
            currentBoard: nextProps.currentBoard,
            currentProfile: nextProps.currentProfile,
            jsonResults: "",
            errorInfo: "",
            spinnerActive: false,
            successMessage: "",
        });
    }

    setSpinnerActive = (spinnerActive) => {
        var success
        if (spinnerActive === true) {
            success = this.state.jsonResults + ' Click to select another';
            this.setState({
                spinnerActive: spinnerActive,
                successMessage: ""
            });
        }
        else {
            success = this.state.jsonResults + ' Click to select another';
            this.setState({
                spinnerActive: spinnerActive,
                successMessage: success
            });
        }

    }

    render() {

        /*eslint-disable */
        if (this.state.jsonResults === "" && this.state.errorInfo === "") {
            this.state.successMessage = "Click to Open a Picker";
        }
        else {
            if (this.state.errorInfo.length > 0) {
                this.state.successMessage = this.state.errorInfo + ' Click to select another';
            }
            else {
                this.state.successMessage = this.state.jsonResults + ' Click to select another';
            }
        }
        /*eslint-enable */

        return (
            <div className="container">
                <GooglePicker clientId={CLIENT_ID}
                    developerKey={DEVELOPER_KEY}
                    scope={SCOPE}
                    onChange={data => console.log('on change:', data)}
                    onAuthenticate={token => console.log('oauth token:', token)}
                    multiselect={true}
                    navHidden={true}
                    authImmediate={false}
                    viewId={'DOCS'}
                    createPicker={(google, oauthToken) => {
                        const googleViewId = google.picker.ViewId.FOLDERS;
                        const docsView = new google.picker.DocsView(googleViewId)
                            .setIncludeFolders(true)
                            .setMimeTypes('application/vnd.google-apps.audio', 'application/vnd.google-apps.video')
                            .setSelectFolderEnabled(true);

                        const picker = new window.google.picker.PickerBuilder()
                            .setSize(600, 800)
                            .addView(docsView)
                            .setOAuthToken(oauthToken)
                            .setOrigin(window.location.protocol + '//' + window.location.host)
                            .setDeveloperKey(DEVELOPER_KEY)
                            .setCallback((data) => {
                                var url = 'nothing';
                                if (data[google.picker.Response.ACTION] === google.picker.Action.PICKED) {
                                    var doc = data[google.picker.Response.DOCUMENTS][0];
                                    url = doc[google.picker.Document.URL];

                                    var API;
                                    if (this.state.currentBoard != null)
                                        API = '/boards/' + this.state.currentBoard + '/profiles/' + this.state.currentProfile + '/AddFileFromGDrive';
                                    else
                                        API = '/profiles/' + this.state.currentProfile + '/AddFileFromGDrive';

                                    console.log("API FOR UPLOADING MEDIA: " + API);
                                    console.log('BODY FOR UPLOAD: ' + JSON.stringify({
                                        GDriveURL: url,
                                        oauthToken: oauthToken,
                                        fileId: data.docs[0].id,
                                        currentBoard: this.state.currentBoard,
                                    }));

                                    var myErrorInfo = "";
                                    var myJsonResults = "";
                                    const googleDriveMediaPicker = this;

                                    googleDriveMediaPicker.setSpinnerActive(true);

                                    fetch(API, {
                                        method: 'POST',
                                        headers: {
                                            'Accept': 'application/json',
                                            'Content-Type': 'application/json',
                                            'authorization': window.sessionStorage.JWT,
                                        },
                                        body: JSON.stringify({
                                            GDriveURL: url,
                                            oauthToken: oauthToken,
                                            fileId: data.docs[0].id,
                                            currentBoard: this.state.currentBoard,

                                        })
                                    })
                                        .then(res => {
                                            console.log('res ok? ' + res.ok);
                                            console.log('res status:' + res.status);

                                            if (!res.ok) {
                                                res.text().then(function (text) {
                                                    console.log('res text: ' + text);
                                                    myErrorInfo = text;

                                                    console.log("about to set the state: " + JSON.stringify(googleDriveMediaPicker.state));

                                                    googleDriveMediaPicker.setState({
                                                        errorInfo: myErrorInfo,
                                                        jsonResults: myJsonResults
                                                    });
                                                    googleDriveMediaPicker.setSpinnerActive(false);


                                                });
                                            }
                                            else {

                                                res.text().then(function (text) {
                                                    console.log('res : ' + text);
                                                    myJsonResults = text;

                                                    googleDriveMediaPicker.setState({
                                                        errorInfo: myErrorInfo,
                                                        jsonResults: myJsonResults
                                                    });

                                                    googleDriveMediaPicker.setSpinnerActive(false);

                                                });
                                            }
                                        })
                                        .catch((err) => {
                                            console.log("in catch block");
                                            var myErrorInfo = "";
                                            err.text().then(function (text) {
                                                console.log('res text: ' + text);
                                                myErrorInfo = text;
                                            });
                                            this.setState({ errorInfo: myErrorInfo });
                                        });

                                }
                            });

                        picker.build().setVisible(true);
                    }}
                >

                    <div style={{
                        'backgroundColor': 'lightblue',
                        'margin': '1cm 1cm 1cm 1cm',
                        'padding': '10px 5px 15px 20px'
                    }}><GoogleDriveSpinner loading={this.state.spinnerActive} message={this.state.successMessage} />

                    </div>
                </GooglePicker>

            </div>
        )
    }

}

export default GoogleDriveMediaPicker;
