import React from 'react';
import { View, Text, WebView } from 'react-native';
import { StackNavigator } from 'react-navigation'; // Version can be specified in package.json

export default class ManageBoardSettings extends React.Component {
    render() {
        const { navigate } = this.props.navigation;
        return (
            <View><Text>Nothing Here...</Text></View>
        );
    }
}
