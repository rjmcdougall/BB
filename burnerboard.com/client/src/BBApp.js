import React, { Component } from 'react';
import AudioList from './AudioList';
import VideoList from './VideoList';
import GlobalMenu from './GlobalMenu';
import BoardGrid from './BoardGrid';
import BatteryHistoryGrid from './BatteryHistoryGrid';
import GoogleDriveMediaPicker from './GoogleDriveMediaPicker';
import ManageMediaGrid from './ManageMediaGrid';
import ProfileGrid from './ProfileGrid';
import AddProfile from './AddProfile';

class BBApp extends Component {

  constructor(props) {
    super(props);

    this.state = {
      currentAppBody: "none",
      currentBoard: "Select Board",
      currentProfile: "Select Profile"
    };

    this.handleSelect = this.handleSelect.bind(this);
  }

  handleSelect(info) {

    console.log(`selected ${info.key}`);

    if (info.key.startsWith("AppBody-")) {
      this.setState({ currentAppBody: info.key });
    }
    else if (info.key.startsWith("board-")) {
      this.setState({ currentBoard: info.key.slice(6) });
    }
    else if (info.key.startsWith("profile-")) {
      this.setState({ currentProfile: info.key.slice(8) });
    };
  }

  render() {

    let appBody = null;
    console.log(this.state.currentAppBody);

    switch (this.state.currentAppBody) {
      case "AppBody-CurrentStatuses":
       appBody = <BoardGrid />;
        break;
      case "AppBody-BatteryHistory":
        appBody = <BatteryHistoryGrid currentBoard={this.state.currentBoard} />;
        break;
      case "AppBody-ReorderAudio":
        appBody = <AudioList currentBoard={this.state.currentBoard} />;
        break;
      case "AppBody-ReorderVideo":
       appBody = <VideoList currentBoard={this.state.currentBoard} />;
        break;
      case "AppBody-LoadFromGDrive":
       appBody = <GoogleDriveMediaPicker currentBoard={this.state.currentBoard} />;
        break;
      case "AppBody-ManageMedia":
       appBody = <ManageMediaGrid currentBoard={this.state.currentBoard} />;
        break;
      case "AppBody-ManageProfiles":
        appBody = <ProfileGrid/>;
        break;
      case "AppBody-AddProfile":
        appBody = <AddProfile/>;
        break;
      default:
        if (this.state.currentBoard !== "Select Board") {
          appBody = <div style={{
            'backgroundColor': 'lightblue',
            'margin': '1cm 1cm 1cm 1cm',
            'padding': '10px 5px 15px 20px'
          }}><p>You selected {this.state.currentBoard}.</p><p>Global options available.</p> <p>Board-specific options available.</p></div>;
        }
        else {
          appBody = <div style={{
            'backgroundColor': 'lightblue',
            'margin': '1cm 1cm 1cm 1cm',
            'padding': '10px 5px 15px 20px'
          }}><p>Global options available.</p> <p>Please select a board for board-specific options.</p></div>;
        }
        break;
    };

    console.log("rendering in app " + appBody);


    return (
      <div className="BBApp" style={{ margin: 0 }}>
        <GlobalMenu handleSelect={this.handleSelect} currentBoard={this.state.currentBoard} currentProfile={this.state.currentProfile} />
        {appBody}
      </div>
    );
  }
}

export default BBApp;

