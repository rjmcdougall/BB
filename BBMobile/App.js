import React from 'react';
import { View, Text } from 'react-native';
import { StackNavigator } from 'react-navigation'; // Version can be specified in package.json
import {Button} from 'react-native';
import HomeScreen from './HomeScreen';
import BBComView from './BBComView';
import ManageBoardSettings from './ManageBoardSettings';
  
const RootStack = StackNavigator(
  {
    Home: {
      screen: HomeScreen,
    },
    BBCom: { 
      screen: BBComView,
    },   
    BoardSettings: { 
      screen: ManageBoardSettings,
    }, 
  },
  {
    initialRouteName: 'Home',
  }
);

export default class App extends React.Component {
  render() {
    return <RootStack />;
  }
}