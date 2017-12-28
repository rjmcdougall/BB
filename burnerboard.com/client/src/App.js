import React, { Component } from 'react';
import MediaList from './MediaList';

//import ReactDOM from 'react-dom';

class App extends Component {
  // constructor(props) {
  //   super(props);
  // }

  // Normally you would want to split things out into separate components.
  // But in this example everything is just done in one place for simplicity
  render() {
    return (
      <div className="App">
        <MediaList />
      </div>
    );
  }
}

export default App;
