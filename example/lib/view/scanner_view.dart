import 'dart:io';

import 'package:ble_radar/ble_radar.dart';
import 'package:ble_radar/bluetooth_device.dart';

import 'package:flutter/material.dart';
import 'package:lottie/lottie.dart';

class ScannerView extends StatefulWidget {
  @override
  _ScannerViewState createState() => _ScannerViewState();
}

class _ScannerViewState extends State<ScannerView> {
  final GlobalKey<ScaffoldState> _scaffoldKey = new GlobalKey<ScaffoldState>();

  BleRadar bleRadar;
  List<BluetoothDevice> bluetoothDevices = [];
  List<String> servicesUUID = [];
  bool isEnableBluetooth = false, isEnableLocation = false, isScanning = false, isConnectedDevice = false;

  @override
  void initState() {
    super.initState();

    initBle();
  }

  startMethod() {
    bleRadar.start(maxRssi: -45, autoConnect: false);
    //filterUUID: ["99999999-8888-7777-6666-555555555555"],
  }

  void initBle() {
    bleRadar = BleRadar();
    //Timer(Duration(seconds: 1), startMethod);

    bleRadar.isEnableBluetooth.listen((status) {
      if (status == null) return;
      setState(() {
        isEnableBluetooth = status;
      });
    });

    if (Platform.isAndroid) {
      bleRadar.isEnableLocation.listen((status) {
        if (status == null) return;
        setState(() {
          isEnableLocation = status;
        });
      });
    }

    bleRadar.isScanning.listen((status) {
      if (status == null) return;
      setState(() {
        isScanning = status;
      });
    });

    bleRadar.onDetectDevice.listen((BluetoothDevice device) {
      print("BluetoothDevice : ${device.toString()}");
      setState(() {
        int index = bluetoothDevices.indexWhere((BluetoothDevice item) => item.name == device.name);
        if (index == -1) {
          bluetoothDevices.add(device);
        } else {
          bluetoothDevices.elementAt(index).rssi = device.rssi;
        }

        if (bluetoothDevices.length > 1) bluetoothDevices.sort((a, b) => b.rssi.compareTo(a.rssi));
      });
      //bleRadar.stop();
      //bleRadar.connectDevice();
    });

    bleRadar.isConnectedDevice.listen((status) {
      setState(() {
        isConnectedDevice = status;
        if (!status) {
          //bluetoothDevice = null;
        }
      });
    });

    bleRadar.onServicesDiscovered.listen((List<String> list) {
      servicesUUID.addAll(list);
      writeUserId();
    });
/*
    bleRadar.onReadCharacteristic.listen((data) {
      _scaffoldKey.currentState.hideCurrentSnackBar();
      _scaffoldKey.currentState.showSnackBar(SnackBar(
        content: Row(
          children: [
            Icon(Icons.business, color: Colors.white),
            SizedBox(height: 10),
            Expanded(
                child: Text(
              "Firma: $data",
              style: TextStyle(
                color: Colors.white,
                fontSize: 16,
              ),
            )),
          ],
        ),
        backgroundColor: Colors.grey[800],
        onVisible: () => startMethod(),
      ));
    });

    bleRadar.onWriteCharacteristic.listen((status) {
      bleRadar.disconnectDevice();
      _scaffoldKey.currentState.hideCurrentSnackBar();
      _scaffoldKey.currentState.showSnackBar(SnackBar(
        content: status
            ? Row(
                children: [
                  Icon(Icons.done, color: Colors.white),
                  SizedBox(height: 10),
                  Expanded(child: Text("İşlem başarılı", style: TextStyle(color: Colors.white))),
                ],
              )
            : Row(
                children: [
                  Icon(Icons.error, color: Colors.white),
                  SizedBox(height: 10),
                  Expanded(child: Text("Bir hata meydana geldi", style: TextStyle(color: Colors.white))),
                ],
              ),
        backgroundColor: status ? Colors.green : Colors.red,
        duration: Duration(seconds: 2),
        onVisible: () {
          /*
          Timer(Duration(seconds: 4), () {
            startMethod();
          });
          */
        },
      ));
    });
*/
  }

  readCustomerInfo() {
    final serviceUUID = "a6c33970-6c90-49f8-bf3b-47d149400b9c";
    final characteristicUUID = "86f774ee-31b7-40c2-adb0-bfbb0550626f";
    print("BleRadar -> readCustomerInfo");
    bleRadar.readCharacteristic(serviceUUID, characteristicUUID);
  }

  writeUserId() {
    final serviceUUID = "a6c33970-6c90-49f8-bf3b-47d149400b9c";
    final characteristicUUID = "0c5ce913-5432-440d-8acd-4e301006682d";
    bleRadar.writeCharacteristic(serviceUUID, characteristicUUID, "042C806A486280");
  }

  @override
  void dispose() {
    bleRadar.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        key: _scaffoldKey,
        appBar: AppBar(
          backgroundColor: Colors.red,
          title: Text('Ble Radar'),
          actions: [
            // TextButton.icon(
            //     style: ButtonStyle(foregroundColor: MaterialStateProperty.all(Colors.white)),
            //     onPressed: () => startScanQR(),
            //     icon: Icon(Icons.qr_code_scanner_rounded),
            //     label: Text("QR Tara"))
          ],
        ),
        body: Container(
          child: body,
          width: double.infinity,
          height: double.infinity,
          padding: EdgeInsets.all(10),
        ),
      ),
    );
  }

  get body {
    return Column(
      children: [
        Expanded(
          child: ListView.builder(
            itemCount: bluetoothDevices.length,
            itemBuilder: (context, index) {
              return ListTile(
                title: Text(bluetoothDevices.elementAt(index).name ?? "NoName"),
                leading: CircleAvatar(
                  child: Text(bluetoothDevices.elementAt(index).rssi.toString()),
                ),
              );
            },
          ),
        ),
        _scannerStatus,
        _bluetoothStatus,
        _locationStatus,
      ],
    );
  }

  get bodyCenter {
    if (isScanning) {
      return Stack(
        alignment: Alignment.center,
        children: [
          Positioned.fill(child: Lottie.asset("assets/lottie_scan.json", repeat: true, reverse: false)),
          Positioned(
            width: 70,
            height: 70,
            child: Icon(Icons.wifi_rounded, size: 60, color: Colors.blue),
          )
        ],
      );
    }
    return Lottie.asset("assets/lottie_loading.json", repeat: true, reverse: true);
  }

  get _scannerStatus {
    if (isScanning) {
      return InkWell(
        onTap: () => bleRadar.stop(),
        child: Chip(
          avatar: Icon(Icons.wifi_rounded, size: 20, color: Colors.white),
          backgroundColor: Colors.blue,
          label: Text("Tarama Aktif", style: TextStyle(color: Colors.white)),
        ),
      );
    }

    return InkWell(
      onTap: () => startMethod(),
      child: Chip(
        avatar: Icon(Icons.wifi_off_rounded, size: 20),
        backgroundColor: Colors.grey,
        label: Text("Tarama Devre Dışı"),
      ),
    );
  }

  get _bluetoothStatus {
    if (isEnableBluetooth) {
      return Chip(
        avatar: Icon(Icons.bluetooth, size: 20, color: Colors.white),
        backgroundColor: Colors.blue,
        label: Text("Bluetooth Aktif", style: TextStyle(color: Colors.white)),
      );
    }

    return Chip(
      avatar: Icon(Icons.bluetooth_disabled, size: 20),
      backgroundColor: Colors.grey,
      label: Text("Bluetooth Devre Dışı"),
    );
  }

  get _locationStatus {
    if (!Platform.isAndroid) {
      return Container();
    }

    if (isEnableLocation) {
      return Chip(
        avatar: Icon(Icons.location_on_outlined, size: 20, color: Colors.white),
        backgroundColor: Colors.blue,
        label: Text("Konum Aktif", style: TextStyle(color: Colors.white)),
      );
    }

    return Chip(
      avatar: Icon(Icons.location_off_rounded, size: 20),
      backgroundColor: Colors.grey,
      label: Text("Konum Devre Dışı"),
    );
  }
}
