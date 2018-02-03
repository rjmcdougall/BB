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
import { MuiThemeProvider, createMuiTheme } from 'material-ui/styles';
import Typography from 'material-ui/Typography';

class BBApp extends Component {

  constructor(props) {
    super(props);

    this.state = {
      currentAppBody: "none",
      currentBoard: "Select Board",
      currentProfile: "Select Profile",
      currentProfileIsGlobal: false,
      activeProfileIsGlobal: false,
      activeProfile: "",
      currentSelection: "",
      drawerIsOpen: false,
      globalDrawerIsOpen: false,
    };

    this.handleSelect = this.handleSelect.bind(this);
  }

  handleSelect(event, key) {

    var API;


    if (key.startsWith("AppBody-")) {
      console.log(`SELECTED!!!!!! ${key}`);
      this.setState({ currentAppBody: key,
        drawerIsOpen : false,
        globalDrawerIsOpen : false,
       });
    }
    else if (key === "ActivateProfile") {

      API = '/boards/' + this.state.currentBoard + '/activeProfile/' + this.state.currentProfile + "/isGlobal/" + this.state.currentProfileIsGlobal;
      console.log(API)
      fetch(API, {
        method: 'POST',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json',
          'Authorization': window.sessionStorage.JWT,
        },
      }).then((res) => res.json())
        .then((data) => {
          console.log(data)
          this.setState({
            activeProfile: this.state.currentProfile,
            activeProfileIsGlobal: this.state.currentProfileIsGlobal,
          });

        })
        .catch((err) => console.log(err));

    }
    else if (key.startsWith("board-")) {

      var selectedBoard = key.slice(6);

      API = '/boards/' + selectedBoard;
      console.log(API)
      fetch(API, {
        method: 'GET',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json',
          'Authorization': window.sessionStorage.JWT,
        },
      }).then((res) => res.json())
        .then((data) => {
          console.log(data)
          var activeProfile = data[0].profile;
          var activeProfileIsGlobal = data[0].isGlobal;
          console.log("active profile " + activeProfile);
          this.setState({
            activeProfile: activeProfile,
            activeProfileIsGlobal: activeProfileIsGlobal,
            currentBoard: selectedBoard
          });
        })
        .catch((err) => console.log(err));

    }
    else if (key.startsWith("profile-")) {
      this.setState({
        currentProfile: key.slice(8),
        currentProfileIsGlobal: false
      });
    }
    else if (key.startsWith("globalProfile-")) {
      this.setState({
        currentProfile: key.slice(14),
        currentProfileIsGlobal: true
      });
    }
    ;
  }


  render() {

    let appBody = null;
    console.log("current app key : " + this.state.currentAppBody);


    const readableText = createMuiTheme({
      typography: {
          htmlFontSize: 10,
      },
      palette: {
        primary: {
          light: '#757ce8',
          main: '#3f50b5',
          dark: '#002884',
          contrastText: '#fff',
        },
        secondary: {
          light: '#ff7961',
          main: '#f44336',
          dark: '#ba000d',
          contrastText: '#000',
        },
     },
  }); 
  
    switch (this.state.currentAppBody) {
      case "AppBody-CurrentStatuses":
        appBody = <BoardGrid />;
        break;
      case "AppBody-BatteryHistory":
        appBody = <BatteryHistoryGrid currentBoard={this.state.currentBoard} />;
        break;
      case "AppBody-ReorderAudio":
        if (this.state.currentProfileIsGlobal)
          appBody = <AudioList currentProfile={this.state.currentProfile} />;
        else
          appBody = <AudioList currentBoard={this.state.currentBoard} currentProfile={this.state.currentProfile} />;
        break;
      case "AppBody-ReorderVideo":
        if (this.state.currentProfileIsGlobal)
          appBody = <VideoList currentProfile={this.state.currentProfile} />;
        else
          appBody = <VideoList currentBoard={this.state.currentBoard} currentProfile={this.state.currentProfile} />;
        break;
      case "AppBody-LoadFromGDrive":
        if (this.state.currentProfileIsGlobal)
          appBody = <GoogleDriveMediaPicker currentProfile={this.state.currentProfile} />;
        else
          appBody = <GoogleDriveMediaPicker currentBoard={this.state.currentBoard} currentProfile={this.state.currentProfile} />;
        break;
      case "AppBody-ManageMedia":
        if (this.state.currentProfileIsGlobal)
          appBody = <ManageMediaGrid currentProfile={this.state.currentProfile} />;
        else
          appBody = <ManageMediaGrid currentBoard={this.state.currentBoard} currentProfile={this.state.currentProfile} />;
        break;
      case "AppBody-ManageProfiles":
        appBody = <ProfileGrid />;
        break;
      case "AppBody-AddProfile":
        appBody = <AddProfile />;
        break;
      default:
        if (this.state.currentBoard !== "Select Board") {
          appBody = <div style={{
            'backgroundColor': 'lightblue',
            'margin': '1cm 1cm 1cm 1cm',
            'padding': '10px 5px 15px 20px'
          }}><div>You selected {this.state.currentBoard}.</div><div>Board-specific options available.</div><div>Select a profile to manage media. The * indicates the active profile on {this.state.currentBoard}.</div></div>;
        }
        else {
          appBody = <div style={{
            'backgroundColor': 'lightblue',
            'margin': '1cm 1cm 1cm 1cm',
            'padding': '10px 5px 15px 20px'
          }}><div>Global options available.</div> <div>Please select a board for board-specific options.</div></div>;
        }
        break;
    };

    console.log("rendering in app " + appBody);


    return (
      <div className="BBApp" style={{ margin: 0 }}>
        <MuiThemeProvider theme={readableText}>
         
          <GlobalMenu handleSelect={this.handleSelect} currentBoard={this.state.currentBoard} activeProfile={this.state.activeProfile} activeProfileIsGlobal={this.state.activeProfileIsGlobal} 
                      drawerIsOpen={this.state.drawerIsOpen}
                      globalDrawerIsOpen={this.state.globalDrawerIsOpen}
                      currentAppBody={this.state.currentAppBody}
                      currentProfile={this.state.currentProfile} />
          <Typography>
             {appBody}
          </Typography>
        </MuiThemeProvider>
      </div>
    );
  }
}

export default BBApp;

