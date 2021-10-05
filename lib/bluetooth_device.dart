import 'package:flutter/material.dart';

class BluetoothDevice {
  String name;
  int rssi;

  BluetoothDevice({
    @required this.name,
    @required this.rssi,
  });

  @override
  String toString() {
    return "$name 📶 $rssi";
  }
}
