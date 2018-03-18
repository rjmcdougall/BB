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
import BLEIDs from './BLEIDs';
import VolumeController from './VolumeController';

const window = Dimensions.get('window');
const ds = new ListView.DataSource({
    rowHasChanged: (r1, r2) => r1 !== r2
});

const BleManagerModule = NativeModules.BleManager;
const bleManagerEmitter = new NativeEventEmitter(BleManagerModule);

export default class HomeScreen extends Component {
    constructor() {
        super()

        this.state = {
            scanning: false,
            peripherals: new Map(),
            appState: '',
            selectedPeripheral: null,
        }

        this.audioChannels = {};
        this.audioChannelSelection = {channel: ''};

        this.handleDiscoverPeripheral = this.handleDiscoverPeripheral.bind(this);
        this.handleStopScan = this.handleStopScan.bind(this);
        this.handleUpdateValueForCharacteristic = this.handleUpdateValueForCharacteristic.bind(this);
        this.handleDisconnectedPeripheral = this.handleDisconnectedPeripheral.bind(this);
        this.handleAppStateChange = this.handleAppStateChange.bind(this);
 
        this.BLEIDs = new BLEIDs();

    }

    componentDidMount() {
        AppState.addEventListener('change', this.handleAppStateChange);

        BleManager.start({
            showAlert: false
        });

        this.handlerDiscover = bleManagerEmitter.addListener('BleManagerDiscoverPeripheral', this.handleDiscoverPeripheral);
        this.handlerStop = bleManagerEmitter.addListener('BleManagerStopScan', this.handleStopScan);
        this.handlerDisconnect = bleManagerEmitter.addListener('BleManagerDisconnectPeripheral', this.handleDisconnectedPeripheral);
        this.handlerUpdate = bleManagerEmitter.addListener('BleManagerDidUpdateValueForCharacteristic', this.handleUpdateValueForCharacteristic);


        if (Platform.OS === 'android' && Platform.Version >= 23) {
            PermissionsAndroid.check(PermissionsAndroid.PERMISSIONS.ACCESS_COARSE_LOCATION).then((result) => {
                if (result) {
                    console.log("Permission is OK");
                } else {
                    PermissionsAndroid.requestPermission(PermissionsAndroid.PERMISSIONS.ACCESS_COARSE_LOCATION).then((result) => {
                        if (result) {
                            console.log("User accept");
                        } else {
                            console.log("User refuse");
                        }
                    });
                }
            });
        }

    }

    handleAppStateChange(nextAppState) {
        if (this.state.appState.match(/inactive|background/) && nextAppState === 'active') {
            console.log('App has come to the foreground!')
            BleManager.getConnectedPeripherals([]).then((peripheralsArray) => {
                console.log('Connected boards: ' + peripheralsArray.length);
            });
        }
        this.setState({
            appState: nextAppState
        });
    }

    componentWillUnmount() {
        this.handlerDiscover.remove();
        this.handlerStop.remove();
        this.handlerDisconnect.remove();
        this.handlerUpdate.remove();
    }

    handleDisconnectedPeripheral(data) {
        let peripherals = this.state.peripherals;
        let peripheral = peripherals.get(data.peripheral);
        if (peripheral) {
            peripheral.connected = false;
            peripherals.set(peripheral.id, peripheral);
            this.setState({
                peripherals
            });
        }
        console.log('Disconnected from ' + data.peripheral);
    }

    handleUpdateValueForCharacteristic(data) {
        console.log('Received data from ' + data.peripheral + ' characteristic ' + data.characteristic, data.value);
    }

    handleStopScan() {
        console.log('Scan is stopped');
        this.setState({
            scanning: false
        });
    }

    startScan() {
        if (!this.state.scanning) {
            this.setState({
                peripherals: new Map()
            });
            BleManager.scan([this.BLEIDs.bbUUID], 3, true).then((results) => {
                console.log('Scanning...');
                this.setState({
                    scanning: true
                });
            });
        }
    }

    retrieveConnected() {
        BleManager.getConnectedPeripherals([]).then((results) => {
            console.log(results);
            console.log("connected");
            var peripherals = this.state.peripherals;
            for (var i = 0; i < results.length; i++) {
                var peripheral = results[i];
                peripheral.connected = true;
                peripherals.set(peripheral.id, peripheral);
                this.setState({
                    peripherals
                });
            }
        });
    }

    handleDiscoverPeripheral(peripheral) {
        var peripherals = this.state.peripherals;
        if (!peripherals.has(peripheral.id)) {
            //console.log('Got ble peripheral', peripheral);
            peripherals.set(peripheral.id, peripheral);
            this.setState({
                peripherals
            })
        }
    }

    test(peripheral) {
        if (peripheral) {
            if (peripheral.connected) {
                BleManager.disconnect(peripheral.id);
                this.setState({selectedPeripheral: null});
                
            } else {
                BleManager.connect(peripheral.id).then(() => {
                    let peripherals = this.state.peripherals;
                    let selectedPeripheral = peripherals.get(peripheral.id);
                    if (selectedPeripheral) {
                        selectedPeripheral.connected = true;
                        peripherals.set(peripheral.id, selectedPeripheral);
                        this.setState({
                            peripherals: peripherals,
                            selectedPeripheral: selectedPeripheral,
                        });
                        console.log("selected peripheral:" + this.state.selectedPeripheral);
                    }
                    console.log('Connected to ' + peripheral.id);


                    //setTimeout(() => {

                        /* Test read current RSSI value
                         BleManager.retrieveServices(peripheral.id).then((peripheralData) => {
                            //console.log('Retrieved peripheral services', peripheralData);
                            console.log('Retrieved peripheral services');

                            BleManager.readRSSI(peripheral.id).then((rssi) => {
                                console.log('Retrieved actual RSSI value', rssi);
                            });
                           });
                           */

                        BleManager.retrieveServices(peripheral.id).then((peripheralInfo) => {
                            console.log("retrieve Services");
                            console.log(peripheralInfo);
                        //});



                    //}, 1000);

                                                //setTimeout(() => {

                                                    //BleManager.startNotification(peripheral.id, this.AudioService, this.AudioVolumeCharacteristic).then(() => {
                                                        //console.log('Started notification on ' + peripheral.id);

                                                        setTimeout(() => {
                                                            BleManager.read(peripheral.id, this.BLEIDs.AudioService, this.BLEIDs.AudioVolumeCharacteristic).then((readData) => {
                                                                    console.log('Read Volume 1: ' + readData);
                                                                })
                                                                .catch((error) => {
                                                                // Failure code
                                                                console.log("r1:" + error);
                                                                });
                                                        }, 3333);

                                                        setTimeout(() => {
                                                            BleManager.read(peripheral.id, this.BLEIDs.BatteryService, this.BLEIDs.BatteryCharacteristic).then((readData) => {
                                                                    console.log('Battery: ' + readData);
                                                                })
                                                                .catch((error) => {
                                                                // Failure code
                                                                console.log("batt:" + error);
                                                                });
                                                        }, 5333);

                                                        // setTimeout(() => {
                                                        //     BleManager.write(peripheral.id, this.BLEIDs.AudioService, this.BLEIDs.AudioVolumeCharacteristic, [30]).then(() => {
                                                        //         console.log('Set Volume to 30');
                                                        //         })
                                                        //         .catch((error) => {
                                                        //         // Failure code
                                                        //         console.log(error);
                                                        //         });
                                                        // }, 500);
                                                        setInterval(() => {
                                                            BleManager.read(peripheral.id, this.BLEIDs.AudioService, this.BLEIDs.AudioInfoCharacteristic).then((readData) => {
                                                                console.log('Read Info: ' + readData);
                                                                var channelNo = readData[0];
                                                                var channelInfo = "";
                                                                for (var i = 1; i < readData.length; i++) {
                                                                  channelInfo += String.fromCharCode(readData[i]);
                                                                }
                                                               if (channelInfo && 0 != channelInfo.length) {
                                                                 console.log('Read Info channel: ' +channelNo + ", name = " + channelInfo);
                                                                 this.audioChannels[channelNo] = channelInfo;
                                                               }

                                                                })
                                                                .catch((error) => {
                                                                // Failure code
                                                                console.log("r2: " + error);
                                                                });
                                                        }, 1000);


                                                    //}).catch((error) => {
                                                    //    console.log('Notification error', error);
                        });
                                                //}, 1000);

                }).catch((error) => {
                    console.log('Connection error', error);
                });
            }
        }
    }


  render() {
    const list = Array.from(this.state.peripherals.values());
    const dataSource = ds.cloneWithRows(list);


    return (
      <View style={styles.container}>
     
        <Image
        style={{width: 100, height: 100, alignSelf:"center"}}
        source={require('./images/BurnerBoardIcon.png')}
                />
                 <VolumeController peripheral={this.state.selectedPeripheral}/>

        <TouchableHighlight style={{marginTop: 40,margin: 20, padding:20, backgroundColor:'#ccc'}} onPress={() => this.startScan() }>
          <Text>Scan for Burner Boards ({this.state.scanning ? 'scanning' : 'paused'})</Text>
        </TouchableHighlight>
        {/* <TouchableHighlight style={{marginTop: 0,margin: 20, padding:20, backgroundColor:'#ccc'}} onPress={() => this.retrieveConnected() }>
          <Text>Retrieve connected peripherals</Text>
        </TouchableHighlight> */}
        <ScrollView style={styles.scroll}>
          {(list.length == 0) &&
            <View style={{flex:1, margin: 20}}>
              <Text style={{textAlign: 'center'}}>No peripherals</Text>
            </View>
          }
          <ListView
            enableEmptySections={true}
            dataSource={dataSource}
            renderRow={(item) => {
              const color = item.connected ? 'green' : '#fff';
              return (
                <TouchableHighlight onPress={() => this.test(item) }>
                  <View style={[styles.row, {backgroundColor: color}]}>
                    <Text style={{fontSize: 12, textAlign: 'center', color: '#333333', padding: 10}}>{item.name}</Text>
                    <Text style={{fontSize: 8, textAlign: 'center', color: '#333333', padding: 10}}>{item.id}</Text>
                  </View>
                </TouchableHighlight>
              );
            }}
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
                <Text>{this.state.info}</Text>

        </ScrollView>
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
        flex: 1,
        backgroundColor: '#f0f0f0',
        margin: 10,
    },
    row: {
        margin: 10
    },
});
