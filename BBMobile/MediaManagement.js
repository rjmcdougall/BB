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
import TrackRefresher from './TrackRefresher';
import Touchable from 'react-native-platform-touchable';

const window = Dimensions.get('window');
// const ds = new ListView.DataSource({
//     rowHasChanged: (r1, r2) => r1 !== r2
// });

const BleManagerModule = NativeModules.BleManager;
const bleManagerEmitter = new NativeEventEmitter(BleManagerModule);

export default class MediaManagement extends Component {
    constructor() {
        super()

        this.state = {
            scanning: false,
            peripherals: new Map(),
            appState: '',
            selectedPeripheral: null,
        }

        this.audioChannels = {};
        this.audioChannelSelection = { channel: '' };
       
   }
   
    static navigationOptions = {
        title: 'Media Management', 
      };

    connectToPeripheral(peripheral) {
        if (peripheral) {
            if (peripheral.connected) {
                BleManager.disconnect(peripheral.id);

            } else {
                BleManager.connect(peripheral.id).then(() => {
                    let peripherals = this.state.peripherals;
                    let selectedPeripheral = peripherals.get(peripheral.id);
                    if (selectedPeripheral) {
                        selectedPeripheral.connected = true;
                        peripherals.set(peripheral.id, selectedPeripheral);

                        console.log("MediaManagement: selected peripheral:" + this.state.selectedPeripheral);
                    }
                    console.log('MediaManagement: Connected to ' + peripheral.id);

                    BleManager.retrieveServices(peripheral.id).then((peripheralInfo) => {
                        console.log("MediaManagement: retrieve Services");
                        console.log("MediaManagement: " + peripheralInfo);

                        this.setState({
                            peripherals: peripherals,
                            selectedPeripheral: selectedPeripheral,
                        });

                    });

                }).catch((error) => {
                    console.log('Connection error', error);
                });
            }
        }
    }

    render() {

        const { navigate } = this.props.navigation;

        const { params } = this.props.navigation.state;
        const peripheral = params ? params.peripheral : null;
        var boardConnected;

        if (peripheral.connected) {
            boardConnected = (
              <Text style={[styles.rowText,{ backgroundColor: '#fff' }]}>Connected to {peripheral.name}</Text>
            )
          } else {
            boardConnected = (
                <Text style={[styles.rowText,{ backgroundColor: '#ff0000' }]}>NOT connected to {peripheral.name}</Text>
           
            )
          }

        return (
            <View style={styles.container}>
                <VolumeController peripheral={peripheral} />
    {/*   <TrackController peripheral={peripheral} mediaType="Audio" />  */}
        <TrackController peripheral={peripheral} mediaType="Video" />   
     {/*   <TrackRefresher peripheral={peripheral} mediaType="Video" /> */}
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
    touchableStyle:{
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
