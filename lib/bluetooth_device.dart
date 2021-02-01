import 'package:flutter/material.dart';

class BluetoothDevice {
  final String name;
  final int rssi;

  BluetoothDevice({
    @required this.name,
    @required this.rssi,
  });

  @override
  String toString() {
    return "NAME: $name, RSSI: $rssi";
  }
}
