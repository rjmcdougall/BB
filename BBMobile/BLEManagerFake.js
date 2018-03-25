'use strict';
import React from 'react-native';
import BLEIDs from './BLEIDs';

var fakeDiscoveredPeripherals = [];
var fakeDiscoveredPeripherals = [];
var fakeBondedPeripherals = [];
var fakeDiscoveredPeripherals = [];
var fakePeripherals = [];
 
exports.read = function (peripheralId, serviceUUID, characteristicUUID) {
        return new Promise((fulfill, reject) => {
            //   bleManager.read(peripheralId, serviceUUID, characteristicUUID, (error, data) => {
            //     if (error) {
            //       reject(error);
            //     } else {
            var p = this.fakeConnectedPeripherals.find((item) => {
                return item.id === peripheralId;
            })
            var s = p.services.find((item) => {
                return item.serviceUUID === serviceUUID;
            })
            var data = s.find((item) => {
                return item.characteristicUUID === characteristicUUID;
            })

            fulfill(data);
            //     }
            //    });
        });
    }

    exports.readRSSI = function  (peripheralId) {
        return new Promise((fulfill, reject) => {
            //   bleManager.readRSSI(peripheralId, (error, rssi) => {
            //     if (error) {
            //       reject(error);
            //     } else {
            fulfill(rssi);
            //     }
            //   });
        });
    }

    exports.retrieveServices = function (peripheralId) {
        return new Promise((fulfill, reject) => {
            //   bleManager.retrieveServices(peripheralId, (error, peripheral) => {
            //     if (error) {
            //       reject(error);
            //     } else {
            var peripheral = this.fakeConnectedPeripherals.find((item) => {
                return item.id === peripheralId;
            })
            fulfill(peripheral);
            // }
            //  });
        });
    }

    exports.write = function(peripheralId, serviceUUID, characteristicUUID, data, maxByteSize) {
        if (maxByteSize == null) {
            maxByteSize = 20;
        }
        return new Promise((fulfill, reject) => {
            //   bleManager.write(peripheralId, serviceUUID, characteristicUUID, data, maxByteSize, (error) => {
            //     if (error) {
            //       reject(error);
            //     } else {
            var p = this.fakeConnectedPeripherals.find((item) => {
                return item.id === peripheralId;
            })
            var s = p.services.find((item) => {
                return item.serviceUUID === serviceUUID;
            })
            var newData = s.find((item) => {
                return item.characteristicUUID === characteristicUUID;
            })
            newData = data;
            fulfill();
            //     }
            //   });
        });
    }

    exports.writeWithoutResponse = function (peripheralId, serviceUUID, characteristicUUID, data, maxByteSize, queueSleepTime) {
        if (maxByteSize == null) {
            maxByteSize = 20;
        }
        if (queueSleepTime == null) {
            queueSleepTime = 10
        }
        return new Promise((fulfill, reject) => {
            //   bleManager.writeWithoutResponse(peripheralId, serviceUUID, characteristicUUID, data, maxByteSize, queueSleepTime, (error) => {
            //     if (error) {
            //       reject(error);
            //     } else {

            var p = this.fakeConnectedPeripherals.find((item) => {
                return item.id === peripheralId;
            })
            var s = p.services.find((item) => {
                return item.serviceUUID === serviceUUID;
            })
            var newData = s.find((item) => {
                return item.characteristicUUID === characteristicUUID;
            })
            newData = data;
            fulfill();
            //     }
            //   });
        });
    }

    exports.connect = function(peripheralId) {
        return new Promise((fulfill, reject) => {
            //   bleManager.connect(peripheralId, (error) => {
            //     if (error) {
            //       reject(error);
            //     } else {
            this.fakeConnectedPeripherals.push({ id: peripheralId });
            fulfill();
            //     }
            //   });
        });
    }

    exports.createBond = function(peripheralId) {
        return new Promise((fulfill, reject) => {
            //   bleManager.createBond(peripheralId, (error) => {
            //     if (error) {
            //       reject(error);
            //     } else {
            this.fakeBondedPeripherals.push({ id: peripheralId });
            fulfill();
            //     }
            //   });
        });
    }

    exports.removeBond = function(peripheralId) {
        return new Promise((fulfill, reject) => {
            //   bleManager.removeBond(peripheralId, (error) => {
            //     if (error) {
            //       reject(error);
            //     } else {
            this.fakeBondedPeripherals = this.fakeBondedPeripherals.filter((item => {
                return item.id !== peripheralId;
            }))
            fulfill();
            //     }
            //   });
        });
    }

    exports.disconnect = function(peripheralId) {
        return new Promise((fulfill, reject) => {
            //   bleManager.disconnect(peripheralId, (error) => {
            //     if (error) {
            //       reject(error);
            //     } else {
            this.fakeConnectedPeripherals = this.fakeConnectedPeripherals.filter((item => {
                return item.id !== peripheralId;
            }))
            fulfill();
            //     }
            //   });
        });
    }

    exports.startNotification = function(peripheralId, serviceUUID, characteristicUUID) {
        return new Promise((fulfill, reject) => {
            //   bleManager.startNotification(peripheralId, serviceUUID, characteristicUUID, (error) => {
            //     if (error) {
            //       reject(error);
            //     } else {
            fulfill();
            //     }
            //   });
        });
    }

    exports.stopNotification = function(peripheralId, serviceUUID, characteristicUUID) {
        return new Promise((fulfill, reject) => {
            //   bleManager.stopNotification(peripheralId, serviceUUID, characteristicUUID, (error) => {
            //     if (error) {
            //       reject(error);
            //     } else {
            fulfill();
            //     }
            //   });
        });
    }

    exports.checkState = function() {
        //bleManager.checkState()
        var i=1;
    }

    exports.start = function(options) {
        return new Promise((fulfill, reject) => {
            if (options == null) {
                options = {};
            }
            //   bleManager.start(options, (error) => {
            //     if (error) {
            //       reject(error);
            //     } else {
            fulfill();
            //     }
            //   });
        });
    }

    exports.scan = function(serviceUUIDs, seconds, allowDuplicates, scanningOptions = {}) {
        return new Promise((fulfill, reject) => {
            if (allowDuplicates == null) {
                allowDuplicates = false;
            }

            // (ANDROID) Match as many advertisement per filter as hw could allow
            // dependes on current capability and availability of the resources in hw.
            if (scanningOptions.numberOfMatches == null) {
                scanningOptions.numberOfMatches = 3
            }

            //(ANDROID) Defaults to MATCH_MODE_AGGRESSIVE
            if (scanningOptions.matchMode == null) {
                scanningOptions.matchMode = 1
            }

            //(ANDROID) Defaults to SCAN_MODE_LOW_POWER on android
            if (scanningOptions.scanMode == null) {
                scanningOptions.scanMode = 0;
            }

            //   bleManager.scan(serviceUUIDs, seconds, allowDuplicates, scanningOptions, (error) => {
            //     if (error) {
            //       reject(error);
            //     } else {

            var myBLEDs = new BLEIDs();
            fakePeripherals = myBLEDs.fakeDiscoveredPeripherals;
            fakeDiscoveredPeripherals = fakePeripherals;

            fulfill();
            //     }
            //   });
        });
    }

    exports.stopScan = function() {
        return new Promise((fulfill, reject) => {
            //   bleManager.stopScan((error) => {
            //     if (error != null) {
            //       reject(error);
            //     } else {
            fulfill();
            //     }
            //   });
        });
    }

    exports.enableBluetooth = function() {
        return new Promise((fulfill, reject) => {
            //   bleManager.enableBluetooth((error) => {
            //     if (error != null) {
            //       reject(error);
            //     } else {
            fulfill();
            //     }
            //   });
        });
    }

    exports.getConnectedPeripherals = function(serviceUUIDs) {
        return new Promise((fulfill, reject) => {
            //   bleManager.getConnectedPeripherals(serviceUUIDs, (error, result) => {
            //     if (error) {
            //       reject(error);
            //     } else {

            var result = this.fakeConnectedPeripherals;

            if (result != null) {
                fulfill(result);
            } else {
                fulfill([]);
            }
            //     }
            //   });
        });
    }

    exports.getBondedPeripherals = function() {
        return new Promise((fulfill, reject) => {
            //   bleManager.getBondedPeripherals((error, result) => {
            //     if (error) {
            //       reject(error);
            //     } else {
            var result = this.fakeBondedPeripherals;

            if (result != null) {
                fulfill(result);
            } else {
                fulfill([]);
            }
            //     }
            //   });
        });
    }

    exports.getDiscoveredPeripherals = function() {
        return new Promise((fulfill, reject) => {
            //   bleManager.getDiscoveredPeripherals((error, result) => {
            //     if (error) {
            //       reject(error);
            //     } else {
            var result = this.fakeDiscoveredPeripherals;

            if (result != null) {
                fulfill(result);
            } else {
                fulfill([]);
            }
            //     }
            //   });
        });
    }

    exports.removePeripheral = function(peripheralId) {
        return new Promise((fulfill, reject) => {
            //   bleManager.removePeripheral(peripheralId, (error) => {
            //     if (error) {
            //       reject(error);
            //     } else {
            this.fakeConnectedPeripherals = this.fakeConnectedPeripherals.filter((item) => {
                return item.id !== peripheralId;
            });
            fulfill();
            //     }
            //   });
        });
    }

    exports.isPeripheralConnected = function(peripheralId, serviceUUIDs) {
        // return this.getConnectedPeripherals(serviceUUIDs).then((result) => {
        if (this.fakeConnectedPeripherals.find((p) => { return p.id === peripheralId; })) {
            return true;
        } else {
            return false;
        }
        // });
    }

    exports.requestMTU = function(peripheralId, mtu) {
        return new Promise((fulfill, reject) => {
            //   bleManager.requestMTU(peripheralId, mtu, (error) => {
            //     if (error) {
            //       reject(error);
            //     } else {
            fulfill();
            //     }
            //   });
        });
        //  }
    }
 
