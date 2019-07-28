import React, { Component } from "react";
import { View, Text, TextInput } from "react-native";
import PropTypes from "prop-types";
import Touchable from "react-native-platform-touchable";
import StyleSheet from "./StyleSheet";

export default class WifiController extends Component {
    constructor(props) {
        super(props);
        this.state = {
            c: "",
            p: "",
            firstTime: true,
        };
    }
 
    render() {

        return (
            <View style={{
                margin: 10,
                padding: 10,
                borderColor: "black",
                borderWidth: 2
            }}>
                <Text style={StyleSheet.smallButtonTextCenter}>
                    Wifi
				</Text>
                <View style={{
                    flex: 1, 
                }}>
                    <View style={{ height: 40 }}>
                        <Text style={StyleSheet.rowText}>SSID {this.props.boardState.c}</Text>
                    </View>
                    <View style={{ height: 40 }}>
                        <TextInput
                            style={{ height: 40, width: 200, borderColor: "gray", borderWidth: 1 }}
                            onChangeText={async (c) => {
                                this.setState({ c: c });
                            }}
                            value={this.state.c}
                        />
                    </View>
                </View>
                <View style={{
                    flex: 1, 
                }}>
                    <View style={{ height: 40 }}>
                        <Text style={StyleSheet.rowText}>Password {this.props.boardState.p}</Text>
                    </View>
                    <View style={{ height: 40 }}>
                        <TextInput
                            style={{ height: 40, width: 200, borderColor: "gray", borderWidth: 1 }}
                            onChangeText={async (p) => {
                                this.setState({ p: p });
                            }}
                            value={this.state.p}
                        />
                    </View>
                </View>
                <View style={StyleSheet.button}>
                    <Touchable
                        onPress={async () => {
                            var wifi = this.state.c + "__" + this.state.p;
                            console.log(wifi);
                            await this.props.sendCommand("Wifi", wifi);
                        }}
                        background={Touchable.Ripple("blue")}>
                        <Text style={StyleSheet.smallButtonTextCenter}>Update</Text>
                    </Touchable>
                </View>
            </View>
        );
    }
}
WifiController.propTypes = {
    sendCommand: PropTypes.func,
};