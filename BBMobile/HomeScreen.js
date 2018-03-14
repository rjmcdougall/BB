import React from 'react';
import { View, Text, Image } from 'react-native';
import { StackNavigator } from 'react-navigation'; // Version can be specified in package.json
import { Button } from 'react-native';
import { BleManager } from 'react-native-ble-plx'

export default class HomeScreen extends React.Component {

    constructor() {
        super();
        this.manager = new BleManager()
        this.state = {info: "", values: {}}

        this.localService = "03c21568-159a-11e8-b642-0ed5f89f718b"
        this.locationCharacteristic = "03c2193c-159a-11e8-b642-0ed5f89f718b"
        this.LocationDescriptor = "03c21a90-159a-11e8-b642-0ed5f89f718b"
        this.bbConfig = "03c21db0-159a-11e8-b642-0ed5f89f718b"

        this.AudioService = "89239614-1937-11e8-accf-0ed5f89f718b"
        this.AudioInfoCharacteristic = "892398a8-1937-11e8-accf-0ed5f89f718b"
        this.AudioChannelCharacteristic = "892399e8-1937-11e8-accf-0ed5f89f718b"
        this.AudioVolumeCharacteristic = "59629212-1938-11e8-accf-0ed5f89f718b"
        this.AudioDescriptor = "89239b0a-1937-11e8-accf-0ed5f89f718b"

        this.BatteryService = "4dfc5ef6-22a9-11e8-b467-0ed5f89f718b"
        this.BatteryCharacteristic = "4dfc6194-22a9-11e8-b467-0ed5f89f718b"
    }

    componentWillMount() {
        if (Platform.OS === 'ios') {
            this.manager.onStateChange((state) => {
                if (state === 'PoweredOn') this.scanAndConnect()
            })
        } else {
            this.scanAndConnect()
        }
    }

    scanAndConnect() {
        this.manager.startDeviceScan(null, null, (error, device) => {
            this.info("Scanning...")
            console.log(device)

            if (error) {
                this.error(error.message)
                return
            }

            if (device.name === 'TI BLE Sensor Tag' || device.name === 'SensorTag') {
                this.info("Connecting to TI Sensor")
                this.manager.stopDeviceScan()
                device.connect()
                    .then((device) => {
                        this.info("Discovering services and characteristics")
                        return device.discoverAllServicesAndCharacteristics()
                    })
                    .then((device) => {
                        this.info("Setting notifications")
                        return this.setupNotifications(device)
                    })
                    .then(() => {
                        this.info("Listening...")
                    }, (error) => {
                        this.error(error.message)
                    })
            }
        });
    }


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
