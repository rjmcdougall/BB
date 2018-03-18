import React from "react";
import { View, Text, WebView, Button, TouchableHighlight, Slider } from "react-native";
import BleManager from 'react-native-ble-manager';
import BLEIDs from './BLEIDs';

export default class VolumeController extends React.Component {
  constructor(props) {
    super(props);

    this.BLEIDs = new BLEIDs();

    this.state = { peripheral: props.peripheral,
                    volume: 0 };
  }

  componentWillReceiveProps(nextProps){
    this.setState({peripheral: nextProps.peripheral});
  }

  onUpdateVolume(){
    console.log("VolumeController: pressed");

    if (this.state.peripheral) {
        setTimeout(() => {
            BleManager.write(this.state.peripheral.id, this.BLEIDs.AudioService, this.BLEIDs.AudioVolumeCharacteristic, [70]).then(() => {
                
                this.readVolumeFromBLE();
    
                console.log('VolumeController: Set Volume to ' + this.state.volume);
                })
                .catch((error) => {
                // Failure code
                console.log("VolumeController: " + error);
                });
        }, 5000);  
    }
  }

  readVolumeFromBLE(){
    if (this.state.peripheral) {
        setTimeout(() => {
            BleManager.read(this.state.peripheral.id, this.BLEIDs.AudioService, this.BLEIDs.AudioVolumeCharacteristic).then((readData) => {
                    console.log('VolumeController Read Volume: ' + readData);
                    this.setState({volume: readData});
    
                })
                .catch((error) => {
                // Failure code
                console.log("r1:" + error);
                });
        }, 5000);
    }
  }

  render() {
      console.log("VolumeController peripheral:" + this.state.peripheral);
      
      this.readVolumeFromBLE();

 

    return ( <View>
        <Text>{this.state.volume}</Text>
        <TouchableHighlight style={{marginTop: 0,margin: 20, padding:20, backgroundColor:'#ccc'}} onPress={() => this.onUpdateVolume() }>
          <Text>Update Volume</Text>
        </TouchableHighlight>

        </View>);
  }
}
