import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'bluetooth_device.dart';

class BleRadar {
  final List<String> uuidFilterList;

  StreamSubscription subLocationStatusStream;
  StreamSubscription subBluetoothStatusStream;
  StreamSubscription subScanningStatusStream;
  StreamSubscription subDetectDeviceStream;
  StreamSubscription subConnectedDeviceStream;
  StreamSubscription subServicesDiscoveredStream;
  StreamSubscription subReadCharacteristicStream;
  StreamSubscription subWriteCharacteristicStream;

  BleRadar({this.uuidFilterList = const []}) {
    _loadListeners();
  }

  _loadListeners() async {
    if (Platform.isAndroid) {
      _isEnableLocationController.add(await _channel.invokeMethod('isOpenLocation'));

      subLocationStatusStream = EventChannel("locationStatusStream").receiveBroadcastStream().listen((e) {
        _isEnableLocationController.add(e);
      });
    }
    _isEnableBluetoothController.add(await _channel.invokeMethod('isOpenBluetooth'));
    _isScanningController.add(await _channel.invokeMethod('isScanning'));

    subBluetoothStatusStream = EventChannel("bluetoothStatusStream").receiveBroadcastStream().listen((e) {
      _isEnableBluetoothController.add(e);
    });

    subScanningStatusStream = EventChannel("scanningStatusStream").receiveBroadcastStream().listen((e) {
      _isScanningController.add(e);
    });

    subDetectDeviceStream = EventChannel("detectDeviceStream").receiveBroadcastStream().listen((e) {
      Map json = jsonDecode(e);

      _onDetectDeviceController.add(BluetoothDevice(
        name: json["name"],
        rssi: int.parse(json["rssi"].toString()),
      ));
    });

    subConnectedDeviceStream = EventChannel("connectedDeviceStream").receiveBroadcastStream().listen((e) {
      _isConnectedDeviceController.add(e);
    });

    subServicesDiscoveredStream = EventChannel("servicesDiscoveredStream").receiveBroadcastStream().listen((e) {
      List<String> list = [];
      for (var item in jsonDecode(e)) {
        list.add(item);
      }
      _onServicesDiscoveredController.add(list);
    });

    subReadCharacteristicStream = EventChannel("readCharacteristicStream").receiveBroadcastStream().listen((e) {
      _onReadCharacteristicController.add(e);
    });

    subWriteCharacteristicStream = EventChannel("writeCharacteristicStream").receiveBroadcastStream().listen((e) {
      _onWriteCharacteristicController.add(e);
    });
  }

  final MethodChannel _channel = const MethodChannel('ble_radar');

  Future start({@required int maxRssi, @required bool autoConnect, bool vibration = false, List<String> filterUUID = const []}) async {
    await _channel.invokeMethod("startScan", {
      "maxRssi": maxRssi,
      "autoConnect": autoConnect,
      "vibration": vibration,
      "filterUUID": filterUUID,
    });
  }

  Future<void> stop() async {
    await _channel.invokeMethod("stopScan");
  }

  Future<void> connectDevice() async {
    await _channel.invokeMethod("connectDevice");
  }

  Future<void> disconnectDevice() async {
    await _channel.invokeMethod("disconnectDevice");
  }

  Future<List<String>> getServices() async {
    return await _channel.invokeMethod("getServices");
  }

  Future<List<String>> getCharacteristics(String serviceUUID) async {
    return await _channel.invokeMethod("getCharacteristics", {"serviceUUID": serviceUUID});
  }

  Future<bool> writeCharacteristic(String serviceUUID, String characteristicUUID, String data) async {
    return await _channel.invokeMethod("writeCharacteristic", {
      "serviceUUID": serviceUUID,
      "characteristicUUID": characteristicUUID,
      "data": data,
    });
  }

  Future<void> readCharacteristic(String serviceUUID, String characteristicUUID) async {
    await _channel.invokeMethod("readCharacteristic", {
      "serviceUUID": serviceUUID,
      "characteristicUUID": characteristicUUID,
    });
  }

  Stream<bool> get isEnableBluetooth => _isEnableBluetoothController.stream;
  final _isEnableBluetoothController = StreamController<bool>();

  Stream<bool> get isEnableLocation => _isEnableLocationController.stream;
  final _isEnableLocationController = StreamController<bool>();

  Stream<bool> get isScanning => _isScanningController.stream;
  final _isScanningController = StreamController<bool>();

  Stream<BluetoothDevice> get onDetectDevice => _onDetectDeviceController.stream;
  final _onDetectDeviceController = StreamController<BluetoothDevice>();

  Stream<bool> get isConnectedDevice => _isConnectedDeviceController.stream;
  final _isConnectedDeviceController = StreamController<bool>();

  Stream<List<String>> get onServicesDiscovered => _onServicesDiscoveredController.stream;
  final _onServicesDiscoveredController = StreamController<List<String>>();

  Stream<String> get onReadCharacteristic => _onReadCharacteristicController.stream;
  final _onReadCharacteristicController = StreamController<String>();

  Stream<bool> get onWriteCharacteristic => _onWriteCharacteristicController.stream;
  final _onWriteCharacteristicController = StreamController<bool>();

  void dispose() {
    if (subLocationStatusStream != null) subLocationStatusStream.cancel();
    if (subBluetoothStatusStream != null) subBluetoothStatusStream.cancel();
    if (subScanningStatusStream != null) subScanningStatusStream.cancel();
    if (subDetectDeviceStream != null) subDetectDeviceStream.cancel();
    if (subConnectedDeviceStream != null) subConnectedDeviceStream.cancel();
    if (subServicesDiscoveredStream != null) subServicesDiscoveredStream.cancel();
    if (subReadCharacteristicStream != null) subReadCharacteristicStream.cancel();
    if (subWriteCharacteristicStream != null) subWriteCharacteristicStream.cancel();

    _isEnableBluetoothController.close();
    _isEnableLocationController.close();
    _isScanningController.close();
    _onDetectDeviceController.close();
    _isConnectedDeviceController.close();
    _onServicesDiscoveredController.close();
    _onReadCharacteristicController.close();
    _onWriteCharacteristicController.close();
  }
}
