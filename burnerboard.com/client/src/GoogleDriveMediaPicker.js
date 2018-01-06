import React, { Component } from 'react'
import GooglePicker from 'react-google-picker';

const CLIENT_ID = '845419422405-4e826kofd0al1npjaq6tijn1f3imk43p.apps.googleusercontent.com';
const DEVELOPER_KEY = 'AIzaSyD2LV75zLhuSnHOblyAYz6sUZ-o94dSARQ';
const SCOPE = ['https://www.googleapis.com/auth/drive.readonly'];

class GoogleDriveMediaPicker extends Component {


    constructor(props) {

        console.log("  in constructor " + props.currentBoard);

        super(props);
        this.state = {
            currentBoard: props.currentBoard,
            jsonResults: "",
            errorInfo: "",
        };
  
     }
 
    render() {

        let successMessage = "";
 
        console.log(this.state);

        if (this.state.jsonResults == "" && this.state.errorInfo=="") {
            successMessage = "Click to Open a Picker";
        }
        else {
            if (this.state.errorInfo.length>0){
                successMessage = this.state.errorInfo + ' Click to select another' ;
            }
            else {
                successMessage = this.state.jsonResults + ' Click to select another';
            }
        }

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
                            //.setQuery("*.mp4")
                            .setMimeTypes('application/vnd.google-apps.audio', 'application/vnd.google-apps.video')
                            .setSelectFolderEnabled(true);

                        const picker = new window.google.picker.PickerBuilder()
                            .setSize(600,800)
                            .addView(docsView)
                            .setOAuthToken(oauthToken)
                            .setOrigin(window.location.protocol + '//' + window.location.host)
                            //.setSelectableMimeTypes('application/vnd.google-apps.audio','application/vnd.google-apps.video','application/vnd.google-apps.folder')
                            .setDeveloperKey(DEVELOPER_KEY)
                            .setCallback((data) => {
                                var url = 'nothing';
                                if (data[google.picker.Response.ACTION] == google.picker.Action.PICKED) {
                                    var doc = data[google.picker.Response.DOCUMENTS][0];
                                    url = doc[google.picker.Document.URL];

                                    var message = 'URL: ' + url;
                                    console.log(message);
                                    message = 'file ID: ' + data.docs[0].id;
                                    console.log(message);
                                    message = 'oauthToken: ' + oauthToken;
                                    console.log(message);
                                    message = 'current board: ' + this.state.currentBoard;
                                    
                                    console.log(message);

                                    var API = '/boards/' + 'vega' + '/AddFileFromGDrive';

                                    var postResults = "";
                                    var myErrorInfo = "";
                                    var myJsonResults = "";
                               

                                    const googleDriveMediaPicker = this;
                                     
                                    googleDriveMediaPicker.props.setSpinnerActive(true);

                                    fetch(API, {
                                        method: 'POST',
                                        headers: {
                                            'Accept': 'application/json',
                                            'Content-Type': 'application/json'
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

                                        googleDriveMediaPicker.props.setSpinnerActive(false);

                                        if (!res.ok) {
                                            res.text().then(function (text) {
                                                console.log('res text: ' + text);
                                                myErrorInfo = text;

                                                console.log("about to set the state: " + JSON.stringify(googleDriveMediaPicker.state));

                                                googleDriveMediaPicker.setState({
                                                    errorInfo: myErrorInfo,
                                                    jsonResults: myJsonResults
                                                });


                                            });
                                        }
                                        else {

                                            res.text().then(function (text) {
                                                console.log('res : ' + text);
                                                myJsonResults = text;

                                                console.log("about to set the state: " + JSON.stringify(googleDriveMediaPicker.state));

                                                googleDriveMediaPicker.setState({
                                                    errorInfo: myErrorInfo,
                                                    jsonResults: myJsonResults
                                                });
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
                }}>{successMessage}

                </div>
                </GooglePicker>
            </div>
        )
    }

}

export default GoogleDriveMediaPicker;
