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
 
    if (response.error != null && response.error !== ""){

      this.setState({ buttonText: "Error: " + response.error + " Please try again." });
  }
    else {

      var id_token = response.getAuthResponse().id_token;

      console.log("id token : " + id_token);

      const app = this;

      var API = '/users/Auth';

      fetch(API, {
        method: 'POST',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json',
          'authorization': id_token,
          }
        })
        .then(res => {
          console.log('res ok? ' + res.ok);
          console.log('res status:' + res.status);

          if (!res.ok) {
            res.text().then(function (text) {

              app.setState({
                JWT: "",
                buttonText: text,
              });

              console.log('res not ok json: ' + text);
            });
          }
          else {

            window.sessionStorage.setItem("JWT", id_token)
            console.log("JWT stored in sessionStorage: " + id_token);

            app.setState({
              JWT: window.sessionStorage.JWT,
              buttonText: "",
            });

            res.text().then(function (text) {
              console.log('OK res : ' + text);

            });
          }
        })
        .catch((err) => {
          console.log("in catch block");

          err.text().then(function (text) {
            app.setState({
              JWT: "",
              buttonText: text,
            });
          });

        });

    }


  }

  render() {

    var JWT = "";
    var appBody = "";

    if (window.sessionStorage.JWT != null)
      JWT = window.sessionStorage.JWT;

    if (JWT === "")
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

