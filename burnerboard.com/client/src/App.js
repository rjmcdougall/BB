import React, { Component } from 'react';
import MediaList from './MediaList';
import GlobalMenu from './GlobalMenu';
import BoardGrid from './BoardGrid';
import BatteryHistoryGrid from './BatteryHistoryGrid';
import GoogleDriveMediaPicker from './GoogleDriveMediaPicker';

//import ReactDOM from 'react-dom';

class App extends Component {
  // constructor(props) {
  //   super(props);
  // }
  constructor(props) {
    super(props);

    this.state = {
      currentAppBody: "none",
      currentBoard: "Select Board",
    };

    this.handleSelect = this.handleSelect.bind(this);
  }

  handleSelect(info) {

    console.log(`selected ${info.key}`);

    if (info.key.startsWith("AppBody-")) {
      this.setState({ currentAppBody: info.key });
    }
    else /*it is a board name*/ {
      this.setState({ currentBoard: info.key });
    };
  }

  render() {

    let appBody = null;
    console.log(this.state.currentAppBody);

    var myState = this.state.currentBoard;

    switch (this.state.currentAppBody) {
      case "AppBody-CurrentStatuses":
        console.log("IN BOARDGRID SWITCH");
        appBody = <BoardGrid />;
        break;
      case "AppBody-BatteryHistory":
        console.log("IN BatteryHistory SWITCH");
        appBody = <BatteryHistoryGrid currentBoard={myState} />;
        break;
      case "AppBody-ReorderAudio":
        console.log("IN reorder audio SWITCH");
        appBody = <MediaList currentBoard={myState} />;
        break;
      case "AppBody-LoadFromGDrive":
        console.log("IN load from g drive SWITCH");
        appBody = <GoogleDriveMediaPicker />;
        break;      
      default:
        appBody = <div>not implemented...</div>;
        break;
    };

    console.log("rendering in app " + appBody);


    return (
      <div className="App" style={{ margin: 0 }}>
        <GlobalMenu handleSelect={this.handleSelect} currentBoard={myState} />
        {appBody}
      </div>
    );
  }
}

export default App;

