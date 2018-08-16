import { AppRegistry } from "react-native";
import App from "./App";
import { Client } from 'bugsnag-react-native';
const bugsnag = new Client("905bfbccb8f9a7e3749038ca1900b1b4");

AppRegistry.registerComponent("BBMobile", () => App);