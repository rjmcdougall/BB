import React from 'react';
import { View, Text, WebView } from 'react-native';
import { StackNavigator } from 'react-navigation'; // Version can be specified in package.json

export default class BBComView extends React.Component {
    render() {
        const { navigate } = this.props.navigation;
        return (
            <WebView
                source={{ uri: 'https://burnerboard.com' }}
                style={{ marginTop: 0 }}
            />
        );
    }
}
