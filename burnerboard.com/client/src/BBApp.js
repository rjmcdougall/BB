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
import SetActiveProfile from './SetActiveProfile';
//import { MuiThemeProvider, createMuiTheme } from 'material-ui/styles';
//import Typography from 'material-ui/Typography';

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
      activateOpenSnackbar: false,
      activateResultsMessage: "",
      createProfileOpenSnackbar: false,
      createProfileResultsMessage: "",
      createProfileBoardName: "GLOBAL",
      profileDeleteSnackbarOpen: false,
      profileDeleteResultsMessage: "",
      profileSelected: "",
      forceRerendder: false,
    };

    this.handleSelect = this.handleSelect.bind(this);

  }

  handleCreateProfile = event => {

    var comp = this;

    console.log("state: ", JSON.stringify(this.state));

    var boardID;
    if (this.state.createProfileBoardName != null)
      boardID = this.state.createProfileBoardName.trim();
    else
      boardID = "GLOBAL";

    var profileID = this.state.createProfileName.trim();
    var API = "";

    if (boardID !== "GLOBAL")
      API = '/boards/' + boardID + '/profiles/' + profileID;
    else
      API = '/profiles/' + profileID;

    console.log("API TO CREATE PROFILE : " + API);

    fetch(API, {
      method: 'POST',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
        'Authorization': window.sessionStorage.JWT,
      }
    })
      .then((res) => {

        if (!res.ok) {
          res.json().then(function (json) {
            comp.setState({
              createProfileOpenSnackbar: true,
              createProfileResultsMessage: JSON.stringify(json),
            });
          });
        }
        else {
          res.json().then(function (json) {
            comp.setState({
              createProfileOpenSnackbar: true,
              createProfileResultsMessage: JSON.stringify(json),
              forceRerendder: !comp.state.forceRerendder,
            });
          });
        }
      })
      .catch((err) => {
        comp.setState({
          createProfileOpenSnackbar: true,
          createProfileResultsMessage: err.message
        });
      });
  }

  handleProfileClick = (event, id) => {
    let newSelected = [id];
    this.setState({ profileSelected: newSelected });
  };

  handleActivateProfile = event => {

    var comp = this;

    var API = '/boards/' + this.state.currentBoard + '/activeProfile/' + this.state.currentProfile + "/isGlobal/" + this.state.currentProfileIsGlobal;
    console.log("API TO SET BOARD ACTIVE: " + API);

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
        comp.setState({
          activeProfile: this.state.currentProfile,
          activeProfileIsGlobal: this.state.currentProfileIsGlobal,
          activateOpenSnackbar: true,
          activateResultsMessage: this.state.currentProfile + " activated",
        });


      })
      .catch((err) => {
        console.log('error : ' + err);
        comp.setState({
          activateOpenSnackbar: true,
          activateResultsMessage: err.message
        });
      });
  }

  onProfileDelete = () => {

    var comp = this;

    var profileSelected = this.state.profileSelected.toString();
    var profileID = profileSelected.slice(profileSelected.indexOf('-') + 1)
    var boardID = profileSelected.slice(0, profileSelected.indexOf('-'));

    var API = "";
    if (boardID !== "null")
        API = '/boards/' + boardID + '/profiles/' + profileID
    else
        API = '/profiles/' + profileID

        console.log("delete API : " + API);

    fetch(API, {
        method: 'DELETE',
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': window.sessionStorage.JWT,
        }
    }
    )
        .then((res) => {

            if (!res.ok) {
                res.json().then(function (json) {
                    console.log('error : ' + JSON.stringify(json));
                    comp.setState({
                        profileDeleteSnackbarOpen: true,
                        profileDeleteResultsMessage: JSON.stringify(json),
                    });
                });
            }
            else {
                res.json().then(function (json) {
                    console.log('success : ' + JSON.stringify(json));
                    comp.setState({
                        profileDeleteSnackbarOpen: true,
                        profileDeleteResultsMessage: JSON.stringify(json),
                        profileSelected: "",
                    });
                });
            }
        })
        .catch((err) => {
            console.log('error : ' + err);
            comp.setState({
                profileDeleteSnackbarOpen: true,
                profileDeleteResultsMessage: err.message
            });

        });

}

  handleChange = event => {
    console.log("Set state due to form change: " + [event.target.name] + " " + event.target.value)
    this.setState({ [event.target.name]: event.target.value });
  
  };

  handleSelect = (event, key) => {

    var API;

    if (key.startsWith("AppBody-")) {

      this.setState({
        currentAppBody: key,
        drawerIsOpen: false,
        globalDrawerIsOpen: false,
      });
    }
    else if (key.startsWith("board-")) {

      var selectedBoard = key.slice(6);

      API = '/boards/' + selectedBoard;
      console.log("API CALL TO GET ACTIVE PROFILE: " + API);

      fetch(API, {
        method: 'GET',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json',
          'Authorization': window.sessionStorage.JWT,
        },
      }).then((res) => res.json())
        .then((data) => {

          var activeProfile = data[0].profile;
          var activeProfileIsGlobal = data[0].isGlobal;
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
  }


  render() {

    let appBody = null;

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
          appBody = <ManageMediaGrid  currentProfile={this.state.currentProfile} />;
        else
          appBody = <ManageMediaGrid  currentBoard={this.state.currentBoard} currentProfile={this.state.currentProfile} />;
        break;
      case "AppBody-ManageProfiles":
        appBody = <ProfileGrid profileSelected={this.state.profileSelected} handleProfileClick={this.handleProfileClick} onProfileDelete={this.onProfileDelete} profileDeleteSnackbarOpen={this.state.profileDeleteSnackbarOpen} profileDeleteResultsMessage={this.state.profileDeleteResultsMessage} />;
        break;
      case "AppBody-AddProfile":
        appBody = <AddProfile createProfileBoardName={this.state.createProfileBoardName} handleChange={this.handleChange} handleCreateProfile={this.handleCreateProfile} createProfileOpenSnackbar={this.state.createProfileOpenSnackbar} createProfileResultsMessage={this.state.createProfileResultsMessage} />;
        break;
      case "AppBody-ActivateProfile":
        appBody = <SetActiveProfile handleActivateProfile={this.handleActivateProfile} activateResultsMessage={this.state.activateResultsMessage} activateOpenSnackbar={this.state.activateOpenSnackbar} currentBoard={this.state.currentBoard} currentProfile={this.state.currentProfile} currentProfileIsGlobal={this.state.currentProfileIsGlobal} />;
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

    return (
      <div className="BBApp" style={{ margin: 0 }}>


        <GlobalMenu handleSelect={this.handleSelect}
          currentBoard={this.state.currentBoard}
          activeProfile={this.state.activeProfile}
          activeProfileIsGlobal={this.state.activeProfileIsGlobal}
          drawerIsOpen={this.state.drawerIsOpen}
          globalDrawerIsOpen={this.state.globalDrawerIsOpen}
          currentAppBody={this.state.currentAppBody}
          currentProfile={this.state.currentProfile} 
          forceRerender={this.state.forceRerendder} />
        {/* <MuiThemeProvider theme={readableText}>
            <Typography>*/}
        {appBody}
        {/*  </Typography>
        </MuiThemeProvider> */}
      </div>
    );
  }
}

export default BBApp;

