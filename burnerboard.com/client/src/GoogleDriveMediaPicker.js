import React, { Component } from 'react'
import GooglePicker from 'react-google-picker';

const CLIENT_ID = '845419422405-4e826kofd0al1npjaq6tijn1f3imk43p.apps.googleusercontent.com';
const DEVELOPER_KEY = '6f7dfc0c9d15829025e8e3abaf204223a5ffd614';
const SCOPE = ['https://www.googleapis.com/auth/drive.readonly'];

class GoogleDriveMediaPicker extends Component {

    render() {
        return (
            <div className="container">
                <GooglePicker clientId={CLIENT_ID}
                    developerKey={DEVELOPER_KEY}
                    scope={SCOPE}
                    onChange={data => console.log('on change:', data)}
                    multiselect={true}
                    navHidden={true}
                    authImmediate={false}
                    mimeTypes={['image/png', 'image/jpeg', 'image/jpg']}
                    viewId={'DOCS'}>
                    <span>Click me!</span>
                    <div className="google"></div>
                </GooglePicker>
                {/* <br />
                <hr />
                <br />
                <GooglePicker clientId={CLIENT_ID}
                    developerKey={DEVELOPER_KEY}
                    scope={SCOPE}
                    onChange={data => console.log('on change:', data)}
                    multiselect={true}
                    navHidden={true}
                    authImmediate={false}
                    viewId={'FOLDERS'}
                    createPicker={(google, oauthToken) => {
                        const googleViewId = google.picker.ViewId.FOLDERS;
                        const docsView = new google.picker.DocsView(googleViewId)
                            .setIncludeFolders(true)
                            .setMimeTypes('application/vnd.google-apps.folder')
                            .setSelectFolderEnabled(true);

                        const picker = new window.google.picker.PickerBuilder()
                            .addView(docsView)
                            .setOAuthToken(oauthToken)
                            .setDeveloperKey(DEVELOPER_KEY)
                            .setCallback(() => {
                                console.log('Custom picker is ready!');
                            });

                        picker.build().setVisible(true);
                    }}
                >
                    <span>Click to build a picker which shows folders and you can select folders</span>
                    <div className="google"></div>
                </GooglePicker> */}

            </div>
        )
    }

}

export default GoogleDriveMediaPicker;
