import React, { Component } from 'react';
import MediaList from './MediaList';
import GlobalMenu from './GlobalMenu';
import BoardGrid from './BoardGrid';

//import ReactDOM from 'react-dom';

class App extends Component {
  // constructor(props) {
  //   super(props);
  // }
  constructor(props) {
    super(props);

    this.state = {
      currentAppBody: "none",
  };

    this.handleSelect = this.handleSelect.bind(this);
    
}

  handleSelect(info) {

    console.log(`selected ${info.key}`);
    this.setState({currentAppBody: info.key});

}
  
  render() {

    let appBody = null;

    switch (this.state.currentAppBody) {
      case "CurrentStatuses":
        appBody = <BoardGrid />;
        break;
      case "BatteryHistory":
        appBody = <div>not implemented...</div>;
        break;
      case "ReorderAudio":
        appBody = <div>not implemented...</div>;
        break;
      case "ReorderVideo":
        appBody = <div>not implemented...</div>;
        break;
      default:
        appBody = <div>not implemented...</div>;
        break;
    };

    return (
      <div className="App" style={{ margin: 0 }}>
        <GlobalMenu handleSelect={this.handleSelect} />
        {appBody}
      </div>
    );
  }
}

export default App;

