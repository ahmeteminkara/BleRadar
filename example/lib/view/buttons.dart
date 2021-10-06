import 'dart:io';

import 'package:ble_radar/ble_radar.dart';
import 'package:ble_radar/bluetooth_device.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

class Buttons extends StatefulWidget {
  const Buttons({Key key}) : super(key: key);

  @override
  State<Buttons> createState() => _ButtonsState();
}

class _ButtonsState extends State<Buttons> {
  BleRadar bleRadar;
  ValueNotifier<bool> writeStatus = ValueNotifier(null);
  ValueNotifier<BluetoothDevice> bluetoothDevice = ValueNotifier(null);
  ValueNotifier<List<String>> servicesUUID = ValueNotifier(null);
  ValueNotifier<bool> isConnectedDevice = ValueNotifier(null);
  ValueNotifier<bool> isScanning = ValueNotifier(null);
  ValueNotifier<bool> isEnableBluetooth = ValueNotifier(null);
  ValueNotifier<bool> isEnableLocation = ValueNotifier(null);

  @override
  void initState() {
    super.initState();
    initBle();
  }

  @override
  void dispose() {
    bleRadar.dispose();
    super.dispose();
  }

  void initBle() {
    bleRadar = BleRadar();
    bleRadar.isEnableBluetooth.listen((status) {
      if (status == null) return;
      isEnableBluetooth.value = status;
    });
    if (Platform.isAndroid) {
      bleRadar.isEnableLocation.listen((status) {
        if (status == null) return;
        isEnableLocation.value = status;
      });
    }

    bleRadar.isScanning.listen((status) {
      if (status == null) return;
      isScanning.value = status;
    });

    bleRadar.onDetectDevice.listen((BluetoothDevice device) {
      if (device == null) return;
      bluetoothDevice.value = device;
      bleRadar.stop();
    });

    bleRadar.isConnectedDevice.listen((status) {
      if (status == null) return;
      isConnectedDevice.value = status;
      if (!status) {
        bluetoothDevice.value = null;
      }
    });
    bleRadar.onServicesDiscovered.listen((List<String> list) {
      servicesUUID.value = list;
      //writeUserId();
    });
    bleRadar.onWriteCharacteristic.listen((status) {
      if (status == null) return;
      writeStatus.value = status;
      /*
      ScaffoldMessenger.of(context).hideCurrentSnackBar();
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: Row(children: [
          status ? Icon(Icons.done, color: Colors.white) : Icon(Icons.error, color: Colors.white),
          SizedBox(height: 10),
          Expanded(
              child: Text(
            status ? "İşlem başarılı" : "Bir hata meydana geldi",
            style: TextStyle(color: Colors.white),
          )),
        ]),
        backgroundColor: status ? Colors.green : Colors.red,
        duration: Duration(seconds: 2),
      ));
      */
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text("Ble Radar Kontrol")),
      body: Column(
        children: [
          Expanded(
            child: ListView.separated(
                itemBuilder: (context, index) => _tiles.elementAt(index),
                separatorBuilder: (context, index) => Divider(),
                itemCount: _tiles.length),
          ),
          Row(mainAxisAlignment: MainAxisAlignment.spaceEvenly, children: [
            _buttonStart,
            _buttonStop,
            _buttonConnect,
            _buttonWrite,
            _buttonDisconnect,
          ])
        ],
      ),
    );
  }

  get _buttonStart => IconButton(
      icon: Icon(Icons.play_arrow),
      onPressed: () {
        writeStatus.value = null;
        bluetoothDevice.value = null;
        servicesUUID.value = null;
        isConnectedDevice.value = null;
        isScanning.value = null;

        bleRadar.start(maxRssi: -55, vibration: true, autoConnect: false, filterUUID: ["99999999-8888-7777-6666-555555555555"]);
      });

  get _buttonStop => IconButton(
        icon: Icon(Icons.pause),
        onPressed: () => bleRadar.stop(),
      );

  get _buttonConnect => IconButton(
        icon: Icon(Icons.connect_without_contact),
        onPressed: () => bleRadar.connectDevice(),
      );

  get _buttonWrite => IconButton(
        icon: Icon(Icons.send),
        onPressed: () => sendData(),
      );

  get _buttonDisconnect => IconButton(
        icon: Icon(Icons.close),
        onPressed: () => bleRadar.disconnectDevice(),
      );

  void sendData() {
    final serviceUUID = "A6C33970-6C90-49F8-BF3B-47D149400B9C";
    final characteristicUUID = "0C5CE913-5432-440D-8ACD-4E301006682D";

    if (servicesUUID.value == null) {
      return;
    }

    for (var item in servicesUUID.value) {
      print("service uuid item: $item");
    }

    if (servicesUUID.value.contains(serviceUUID) || servicesUUID.value.contains(serviceUUID.toLowerCase())) {
      bleRadar.writeCharacteristic(serviceUUID, characteristicUUID, "042C806A486280");
    } else {
      showDialog(
        context: context,
        builder: (context) => AlertDialog(
          title: Text("Servis bulunamadı"),
          content: Text("Bağlantıyı koparın"),
        ),
      );
    }
  }

  List<Widget> get _tiles => [
        _bluetoothStatus,
        _scanningStatus,
        _detectStatus,
        _connectStatus,
        _writeStatus,
      ];

  _loading(String s) => ListTile(
        title: Text(s),
        leading: Platform.isAndroid ? CircularProgressIndicator(strokeWidth: 2) : CupertinoActivityIndicator(radius: 13),
      );

  _status(String s, bool value) => ListTile(
        title: Text(s),
        leading: Icon(Icons.lens, color: value ? Colors.green : Colors.red),
      );

  get _bluetoothStatus => ValueListenableBuilder(
        valueListenable: isEnableBluetooth,
        builder: (context, value, child) {
          if (value == null) return _loading("Bluetooth durumu bekleniyor");
          return _status("Bluetooth", value);
        },
      );

  get _scanningStatus => ValueListenableBuilder(
        valueListenable: isScanning,
        builder: (context, value, child) {
          if (value == null) return _loading("Tarama durumu bekleniyor");
          return _status("Tarama", value);
        },
      );

  get _detectStatus => ValueListenableBuilder<BluetoothDevice>(
        valueListenable: bluetoothDevice,
        builder: (context, value, child) {
          if (value == null) return _loading("Cihaz tespit edilmedi");
          return _status(value.toString(), true);
        },
      );

  get _connectStatus => ValueListenableBuilder(
        valueListenable: isConnectedDevice,
        builder: (context, value, child) {
          if (value == null) return _loading("Bağlantı durumu bekleniyor");
          return _status("Bağlantı durumu", value);
        },
      );
  get _writeStatus => ValueListenableBuilder(
        valueListenable: writeStatus,
        builder: (context, value, child) {
          if (value == null) return _loading("Yazma durumu bekleniyor");
          return _status("Yazma işlemi", value);
        },
      );
}
