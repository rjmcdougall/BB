import React from 'react';
import { View, Text } from 'react-native';
import { StackNavigator } from 'react-navigation'; // Version can be specified in package.json
import HomeScreen from './HomeScreen';
import BBComView from './BBComView';

const RootStack = StackNavigator(
  {
    Home: {
      screen: HomeScreen,
      
    },
    BBCom: { 
      screen: BBComView,
    },   
  },
  {
    initialRouteName: 'Home',
    navigationOptions: {
      headerStyle: {
        backgroundColor: 'blue',
        height:40,
      },
      headerTintColor: '#fff',
      headerTitleStyle: {
        fontWeight: 'bold',
        fontSize: 12,
      },
    },
  },
);

export default class App extends React.Component {
  render() {
    return <RootStack />;
  }
}