import React, { Component } from 'react';
import BBApp from './BBApp';
import GoogleLoginPage from './GoogleLoginPage';

class App extends Component {

  constructor(props) {
    super(props);

    this.state = {
      JWT: "",
      buttonText: "Login with Google",
    };

    this.responseGoogle = this.responseGoogle.bind(this);
  }

  responseGoogle = (response) => {
    console.log(response);
    if (response.error != null && response.error != "")
      this.setState({ buttonText: "Error: " + response.error + " Please try again." });

    else {
      var id_token = response.getAuthResponse().id_token;
      window.localStorage.setItem("JWT", id_token)
      console.log("JWT stored in localStorage: " + id_token);

      this.setState({ JWT: window.localStorage.JWT });
    }


  }

  render() {

    console.log("JWT: " + window.localStorage.JWT);
    var JWT = "";
    var appBody = "";

    if (window.localStorage.JWT != null)
      JWT = window.localStorage.JWT;

    if (JWT == "")
      appBody = <GoogleLoginPage buttonText={this.state.buttonText} responseGoogle={this.responseGoogle} />;
    else
      appBody = <BBApp />;

    return (
      <div className="App" style={{ margin: 0 }}>
        {appBody}
      </div>
    );
  }
}

export default App;

