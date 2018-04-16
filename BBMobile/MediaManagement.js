import React, {
    Component
} from 'react';
import {
    AppRegistry,
    StyleSheet,
    Text,
    View,
    Image,
    Button,
    Picker,
    TouchableHighlight,
    NativeAppEventEmitter,
    NativeEventEmitter,
    NativeModules,
    Platform,
    PermissionsAndroid,
    ListView,
    ScrollView,
    AppState,
    Dimensions,
} from 'react-native';
import BleManager from 'react-native-ble-manager';
//import BleManager from './BLEManagerFake';
import BLEIDs from './BLEIDs';
import VolumeController from './VolumeController';
import TrackController from './TrackController';
import BTController from './BTController';
import Touchable from 'react-native-platform-touchable';

const window = Dimensions.get('window');
// const ds = new ListView.DataSource({
//     rowHasChanged: (r1, r2) => r1 !== r2
// });

const BleManagerModule = NativeModules.BleManager;
const bleManagerEmitter = new NativeEventEmitter(BleManagerModule);

export default class MediaManagement extends Component {
    constructor(props) {
        super(props)

        const { params } = this.props.navigation.state;
        const peripheral = params.peripheral;

        this.state = {
            peripheral: peripheral,
        }

    }

    static navigationOptions = {
        title: 'Media Management',
    };

    render() {

        const { navigate } = this.props.navigation;

        var boardConnected;

        if (this.state.peripheral.connected) {
            boardConnected = (
                <Text style={[styles.rowText, { backgroundColor: '#fff' }]}>Connected to {this.state.peripheral.name}</Text>
            )
        } else {
            boardConnected = (
                <Text style={[styles.rowText, { backgroundColor: '#ff0000' }]}>NOT connected to {this.state.peripheral.name}</Text>

            )
        }

        return (
            <View style={styles.container}>
                <VolumeController peripheral={this.state.peripheral} />
                <TrackController peripheral={this.state.peripheral} mediaType="Audio" />
                <TrackController peripheral={this.state.peripheral} mediaType="Video" />
                <BTController peripheral={this.state.peripheral} />
                {boardConnected}
            </View>
        );
    }

}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#FFF',
        width: window.width,
        height: window.height
    },
    scroll: {
        backgroundColor: '#f0f0f0',
        margin: 10,
    },
    touchableStyle: {
        backgroundColor: '#ccc',
        margin: 10,
    },
    rowText: {
        margin: 5,
        fontSize: 12,
        textAlign: 'center',
        padding: 10,
    },
});
