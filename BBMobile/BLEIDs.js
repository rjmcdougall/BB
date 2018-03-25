export default class BLEIDs {

    constructor() {

        this.bbUUID = "58fdc6ee-15d1-11e8-b642-0ed5f89f718b";

        this.localService = "04c21568-159a-11e8-b642-0ed5f89f718b";
        this.locationCharacteristic = "03c2193c-159a-11e8-b642-0ed5f89f718b";
        this.LocationDescriptor = "03c21a90-159a-11e8-b642-0ed5f89f718b";
        this.bbConfig = "03c21db0-159a-11e8-b642-0ed5f89f718b";

        this.AudioService = "89239614-1937-11e8-accf-0ed5f89f718b";
        this.AudioInfoCharacteristic = "892398a8-1937-11e8-accf-0ed5f89f718b";
        this.AudioChannelCharacteristic = "892399e8-1937-11e8-accf-0ed5f89f718b";
        this.AudioVolumeCharacteristic = "59629212-1938-11e8-accf-0ed5f89f718b";
        this.AudioDescriptor = "89239b0a-1937-11e8-accf-0ed5f89f718b";

        this.BatteryService = "4dfc5ef6-22a9-11e8-b467-0ed5f89f718b";
        this.BatteryCharacteristic = "4dfc6194-22a9-11e8-b467-0ed5f89f718b";

        this.fakeDiscoveredPeripherals =
            [
                {
                    id: 'vega',
                    services: [{
                        serviceUUID: this.AudioService,
                        characteristics:
                            [
                                { characteristicUUID: this.AudioInfoCharacteristic, data: 'XXXX' },
                                { characteristicUUID: this.AudioChannelCharacteristic, data: 'XXX' },
                                { characteristicUUID: this.AudioVolumeCharacteristic, data: 'XXX' },
                                { characteristicUUID: this.AudioDescriptor, data: 'XXX' },
                            ]
                    }]
                },
                {
                    id: 'candy',
                    services: [{
                        serviceUUID: this.AudioService,
                        characteristics:
                            [
                                { characteristicUUID: this.AudioInfoCharacteristic, data: 'XXXX' },
                                { characteristicUUID: this.AudioChannelCharacteristic, data: 'XXX' },
                                { characteristicUUID: this.AudioVolumeCharacteristic, data: 'XXX' },
                                { characteristicUUID: this.AudioDescriptor, data: 'XXX' },
                            ]
                    }]
                },
            ]



    }



}
