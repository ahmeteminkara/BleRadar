# BleRadar

Flutter library that scans BLE devices and connects to Gatt services.

## Getting Started
```yaml
dependencies:
    ble_radar:
        git:
            url: git://github.com/ahmeteminkara/BleRadar.git
```
```dart
import 'package:ble_radar/ble_radar.dart';
```

### Variables
```java
private BleRadar bleRadar;

BluetoothDevice bluetoothDevice;
List<String> servicesUUID = [];
bool isEnableBluetooth = false,
     isEnableLocation = false,
     isConnectedDevice = false,
     isScanning = false;
```

### Methods
```dart
bleRadar.start(
    maxRssi: -45,
    autoConnect: false,
    filterUUID: ["99999999-8888-7777-6666-555555555555"],
);
bleRadar.stop();
bleRadar.connectDevice();
bleRadar.disconnectDevice();
```

### Listeners
```java
bleRadar = BleRadar(context);

bleRadar.isEnableBluetooth.listen((bool status) {});

if (Platform.isAndroid) {
    //Why is that? Location is not required for iOS
    bleRadar.isEnableLocation.listen((bool status) {});
}

bleRadar.isScanning.listen((bool status) {});

bleRadar.onDetectDevice.listen((BluetoothDevice device) {
    //device.name, device.rssi
});

bleRadar.isConnectedDevice.listen((bool status) {});

bleRadar.onServicesDiscovered.listen((List<String> list) {
    servicesUUID.addAll(list);
    // after read & write
});

bleRadar.onReadCharacteristic.listen((String data) {
    //readable characteristic value
});

bleRadar.onWriteCharacteristic.listen((bool status) {
    //write request result
});

```

### Read Characteristic
```java 
String serviceUUID = "Target Service UUID;";
String characteristicUUID = "Target Characteristic UUID";
bleRadar.readCharacteristic(serviceUUID, characteristicUUID);

```

### Write Characteristic
```java 
String serviceUUID = "Target Service UUID;";
String characteristicUUID = "Target Characteristic UUID";
String data = "yourtext";
bleRadar.writeCharacteristic(serviceUUID, characteristicUUID,data);

```

### Dispose
```java
@override
void dispose() {
    bleRadar.dispose();
    super.dispose();
}
```