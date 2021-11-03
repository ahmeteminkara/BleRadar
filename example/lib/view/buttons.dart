import 'dart:async';
import 'dart:io';

import 'package:ble_radar/ble_radar.dart';
import 'package:ble_radar/bluetooth_device.dart';
import 'package:device_info/device_info.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

class Buttons extends StatefulWidget {
  const Buttons({Key key}) : super(key: key);

  @override
  State<Buttons> createState() => _ButtonsState();
}

class _ButtonsState extends State<Buttons> {
  final uuidBleDvce = "11111111-1111-1111-1111-111111111111";
  final uuidService = "22222222-2222-2222-2222-222222222222";
  final uuidCharcts = "33333333-3333-3333-3333-333333333333";

  BleRadar bleRadar;
  ValueNotifier<bool> _writeStatus = ValueNotifier(null);
  ValueNotifier<BluetoothDevice> _bluetoothDevice = ValueNotifier(null);
  ValueNotifier<List<String>> _servicesUUID = ValueNotifier(null);
  ValueNotifier<bool> _isConnectedDevice = ValueNotifier(null);
  ValueNotifier<bool> _isScanning = ValueNotifier(null);
  ValueNotifier<bool> _isEnableBluetooth = ValueNotifier(null);

  String deviceName = "";

  @override
  void initState() {
    super.initState();
    initBle();
    getDeviceName();
  }

  @override
  void dispose() {
    bleRadar.dispose();
    super.dispose();
  }

  getDeviceName() async {
    DeviceInfoPlugin deviceInfo = DeviceInfoPlugin();
    if (Platform.isIOS) {
      IosDeviceInfo iosInfo = await deviceInfo.iosInfo;
      print('Running on ${iosInfo.utsname.machine}'); // e.g. "iPod7,1"
      deviceName = iosInfo.utsname.machine;
    } else {
      AndroidDeviceInfo androidInfo = await deviceInfo.androidInfo;
      print('Running on ${androidInfo.model}'); // e.g. "Moto G (4)"
      deviceName = androidInfo.model;
    }
  }

  void initBle() {
    bleRadar = BleRadar();
    bleRadar.isEnableBluetooth.listen((status) {
      if (status == null) return;
      _isEnableBluetooth.value = status;
    });

    bleRadar.isScanning.listen((status) {
      if (status == null) return;
      _isScanning.value = status;
    });

    bleRadar.onDetectDevice.listen((BluetoothDevice device) {
      if (device == null) return;
      _bluetoothDevice.value = device;

      //if (device.name != null && device.name.contains("XP2_")) {
      if (device.rssi < 0 && device.rssi > -50) {
        bleRadar.stop();
        bleRadar.connectDevice();
        print("!!!!!!! device: ${device.toString()}");
      }
    });

    bleRadar.isConnectedDevice.listen((status) {
      if (status == null) return;
      _isConnectedDevice.value = status;
      if (status) {
        Future.delayed(Duration(seconds: 5), () {
          if (_isConnectedDevice.value != null && _isConnectedDevice.value) bleRadar.disconnectDevice();
        });
      } else {
        _bluetoothDevice.value = null;
        _isConnectedDevice.value = null;
      }
    });
    bleRadar.onServicesDiscovered.listen((List<String> list) {
      _servicesUUID.value = list;
      print(list.toString());
      sendData();
    });
    bleRadar.onWriteCharacteristic.listen((status) {
      print("onWriteCharacteristic");
      if (status == null) return;

      bleRadar.disconnectDevice();
      _writeStatus.value = status;

      Timer(Duration(seconds: 4), () {
        _writeStatus.value = null;
      });
      _bluetoothDevice.value = null;
      _servicesUUID.value = null;
      _isScanning.value = null;

      ScaffoldMessenger.of(context).hideCurrentSnackBar();
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: Row(children: [
          status ? Icon(Icons.done, color: Colors.white) : Icon(Icons.error, color: Colors.white, size: 22),
          SizedBox(height: 20),
          Expanded(
              child: Text(
            status ? "İşlem başarılı" : "Bir hata meydana geldi",
            style: TextStyle(color: Colors.white, fontSize: 17, fontWeight: FontWeight.w500),
          )),
        ]),
        backgroundColor: status ? Colors.green : Colors.red,
        duration: Duration(seconds: 2),
      ));
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
            //_buttonDisconnect,
          ])
        ],
      ),
    );
  }

  get _buttonStart => IconButton(
      icon: Icon(Icons.play_arrow),
      onPressed: () {
        start();
      });

  void start() {
    bleRadar.start(
      maxRssi: -100,
      vibration: true,
      autoConnect: false,
      filterUUID: [uuidBleDvce],
    );
  }

  get _buttonStop => IconButton(
        icon: Icon(Icons.pause),
        onPressed: () => bleRadar.stop(),
      );

  get _buttonDisconnect => IconButton(
        icon: Icon(Icons.close),
        onPressed: () => bleRadar.disconnectDevice(),
      );

  void sendData() {
    if (_servicesUUID.value == null) {
      return;
    }

    for (var item in _servicesUUID.value) {
      print("service uuid item: $item");
    }

    if (_servicesUUID.value.contains(uuidService) || _servicesUUID.value.contains(uuidService.toLowerCase())) {
      bleRadar.writeCharacteristic(uuidService, uuidCharcts, deviceName);
    } else {
      bleRadar.disconnectDevice();
      ScaffoldMessenger.of(context).hideCurrentSnackBar();
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: Row(children: [
          Icon(Icons.error, color: Colors.white, size: 22),
          SizedBox(height: 20),
          Expanded(
              child: Text(
            "Servisi bulamadı",
            style: TextStyle(color: Colors.white, fontSize: 17, fontWeight: FontWeight.w500),
          )),
        ]),
        backgroundColor: Colors.red,
        duration: Duration(seconds: 2),
      ));
    }
  }

  List<Widget> get _tiles => [
        _bluetoothStatus,
        _scanningStatus,
        _detectStatus,
        _connectStatus,
        _writeStatusTile,
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
        valueListenable: _isEnableBluetooth,
        builder: (context, value, child) {
          if (value == null) return _loading("Bluetooth durumu bekleniyor");
          return _status("Bluetooth", value);
        },
      );

  get _scanningStatus => ValueListenableBuilder(
        valueListenable: _isScanning,
        builder: (context, value, child) {
          if (value == null) return _loading("Tarama durumu bekleniyor");
          return _status("Tarama", value);
        },
      );

  get _detectStatus => ValueListenableBuilder<BluetoothDevice>(
        valueListenable: _bluetoothDevice,
        builder: (context, value, child) {
          if (value == null) return _loading("Cihaz tespit edilmedi");
          return _status(value.toString(), true);
        },
      );

  get _connectStatus => ValueListenableBuilder(
        valueListenable: _isConnectedDevice,
        builder: (context, value, child) {
          if (value == null) return _loading("Bağlantı durumu bekleniyor");
          return _status("Bağlantı durumu", value);
        },
      );
  get _writeStatusTile => ValueListenableBuilder(
        valueListenable: _writeStatus,
        builder: (context, value, child) {
          if (value == null) return _loading("Yazma durumu bekleniyor");
          return _status("Yazma işlemi", value);
        },
      );
}
