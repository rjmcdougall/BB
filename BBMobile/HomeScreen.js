import React from 'react';
import { View, Text, Image } from 'react-native';
import { StackNavigator } from 'react-navigation'; // Version can be specified in package.json
import { Button } from 'react-native';

export default class HomeScreen extends React.Component {

    render() {
        const { navigate } = this.props.navigation;

        return (
            <View>
                <Image
                    style={{width: 100, height: 100, alignSelf:"center"}}
                    source={require('./images/BurnerBoardIcon.png')}
                />
                <Button
                    title="Go To BB.com"
                    onPress={() => {
                        navigate('BBCom');
                    }
                    }
                />
                <Button
                    title="Manage Board Settings"
                    onPress={() => {
                        navigate('BoardSettings');
                    }
                    }
                />
            </View>
        );

    }
}