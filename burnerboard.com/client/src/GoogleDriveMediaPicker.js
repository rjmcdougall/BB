import React, { Component } from 'react'
import GooglePicker from 'react-google-picker';

const CLIENT_ID = '845419422405-4e826kofd0al1npjaq6tijn1f3imk43p.apps.googleusercontent.com';
const DEVELOPER_KEY = 'AIzaSyD2LV75zLhuSnHOblyAYz6sUZ-o94dSARQ';
const SCOPE = ['https://www.googleapis.com/auth/drive.readonly'];

class GoogleDriveMediaPicker extends Component {

    render() {
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
                            .addView(docsView)
                            .setOAuthToken(oauthToken)
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

                                    var API = '/boards/' + 'vega' + '/AddFileFromGDrive';

                                    fetch(API, {
                                        method: 'POST',
                                        headers: {
                                            'Accept': 'application/json',
                                            'Content-Type': 'application/json'
                                        },
                                        body: JSON.stringify({
                                            GDriveURL: url,
                                            oauthToken: oauthToken,
                                            fileId: data.docs[0].id
                                        })
                                    }).then((res) => res.json())
                                        .then((data) => console.log(data))
                                        .catch((err) => console.log(err));
                                }
                            });

                        picker.build().setVisible(true);
                    }}
                >
                    <span>Click to build a picker which shows folders and you can select folders</span>
                    <div className="google"></div>
                </GooglePicker>

            </div>
        )
    }

}

export default GoogleDriveMediaPicker;
