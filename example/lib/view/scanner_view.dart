import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:ble_radar/ble_radar.dart';
import 'package:ble_radar/bluetooth_device.dart';
import 'package:ble_radar_example/view/buttons.dart';
import 'package:ble_radar_example/view/qr_scanner.dart';

import 'package:flutter/material.dart';
import 'package:lottie/lottie.dart';

class ScannerView extends StatefulWidget {
  @override
  _ScannerViewState createState() => _ScannerViewState();
}

class _ScannerViewState extends State<ScannerView> {
  BleRadar bleRadar;
  BluetoothDevice bluetoothDevice;
  List<String> servicesUUID = [];
  bool isEnableBluetooth = false, isEnableLocation = false, isScanning = false, isConnectedDevice = false, isShowLog = true;

  List<String> log = [];
  ScrollController logController;

  bool get isAutoConnect => false;

  @override
  void initState() {
    super.initState();
    addLog("initState");
    logController = ScrollController();
    initBle();
  }

  startMethod() {
    setState(() => log.clear());
    bleRadar.start(
      maxRssi: -55,
      vibration: true,
      autoConnect: isAutoConnect,
      filterUUID: ["99999999-8888-7777-6666-555555555555"],
    );
  }

  void initBle() {
    addLog("initBle");
    bleRadar = BleRadar();
    //Timer(Duration(seconds: 3), startMethod);

    bleRadar.isEnableBluetooth.listen((status) {
      if (status == null) return;
      setState(() {
        isEnableBluetooth = status;
      });
    });


    bleRadar.isScanning.listen((status) {
      if (status == null) return;
      if (status) setState(() => log.clear());

      addLog(status ? " 📡 Tarama başladı" : "  ❌ Tarama sonlandı ");
      setState(() => isScanning = status);
    });

    bleRadar.onDetectDevice.listen((BluetoothDevice device) {
      addLog("    📲 Bulduğu cihaz: ${device.toString()} ");

      setState(() {
        bluetoothDevice = device;
      });

      if (!isAutoConnect) {
        bleRadar.stop();
        bleRadar.connectDevice();
      }
    });

    bleRadar.isConnectedDevice.listen((status) {
      addLog(status ? "     🟢 cihaza bağlandı" : "     🔴 bağlantı koptu ");
      setState(() {
        isConnectedDevice = status;
        if (!status && bluetoothDevice != null) {
          bluetoothDevice = null;
        }
      });
    });

    bleRadar.onServicesDiscovered.listen((List<String> list) {
      if (list.isEmpty) {
        addLog("        ⛔ servis tespit edilemedi !!");
      } else {
        addLog("        🔍 ${list.length} adet servis tespit edildi");
      }
      servicesUUID.addAll(list);

      writeUserId();
    });

    bleRadar.onReadCharacteristic.listen((data) {
      addLog("bleRadar.onReadCharacteristic");
      ScaffoldMessenger.of(context).hideCurrentSnackBar();
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
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
      addLog("✍ Yazma işi bitti");
      bleRadar.disconnectDevice();
      ScaffoldMessenger.of(context).hideCurrentSnackBar();
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
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
        onVisible: () => Timer(Duration(seconds: 2), startMethod),
      ));
    });
  }

  readCustomerInfo() {
    final serviceUUID = "a6c33970-6c90-49f8-bf3b-47d149400b9c";
    final characteristicUUID = "86f774ee-31b7-40c2-adb0-bfbb0550626f";
    bleRadar.readCharacteristic(serviceUUID, characteristicUUID);
    addLog("BleRadar -> readCharacteristic");
  }

  writeUserId() {
    final serviceUUID = "A6C33970-6C90-49F8-BF3B-47D149400B9C";
    final characteristicUUID = "0C5CE913-5432-440D-8ACD-4E301006682D";

    if (servicesUUID.contains(serviceUUID) || servicesUUID.contains(serviceUUID.toLowerCase())) {
      addLog("          🚀 yazma istedi gitti");
      bleRadar.writeCharacteristic(serviceUUID, characteristicUUID, "042C806A486280");
    } else {
      addLog("🚧 yazma servisi bulunamadı");
      bleRadar.disconnectDevice();
    }
  }

  addLog(String s) {
    setState(() => log.add(s));
    try {
      logController.animateTo(logController.position.maxScrollExtent, duration: Duration(milliseconds: 100), curve: Curves.linear);
    } catch (e) {}
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
        appBar: AppBar(
          backgroundColor: Colors.red,
          title: Text('Ble Radar'),
          actions: [
            IconButton(
              icon: Icon(Icons.network_check_outlined),
              onPressed: () {
                Navigator.push(context, MaterialPageRoute(builder: (context) => Buttons()));
              },
            ),
            IconButton(
              icon: Icon(Icons.subject),
              onPressed: () => setState(() => isShowLog = !isShowLog),
            ),
            IconButton(
              icon: Icon(Icons.qr_code_scanner_rounded),
              onPressed: () => startScanQR(),
            )
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
          child: bodyCenter,
        ),
        isShowLog
            ? TextButton(
                onPressed: () => setState(() {
                      log.clear();
                    }),
                child: Text("Log Temizle"))
            : Container(),
        Wrap(
          spacing: 10,
          children: [
            _scannerStart,
            _scannerStop,
            _scannerStatus,
            _bluetoothStatus,
            _locationStatus,
          ],
        )
      ],
    );
  }

  get bodyCenter {
    if (isShowLog) {
      return ListView.separated(
        padding: EdgeInsets.only(bottom: 40),
        controller: logController,
        itemBuilder: (c, i) => Padding(
          padding: const EdgeInsets.all(5),
          child: Text(log.elementAt(i), style: TextStyle(fontSize: 18)),
        ),
        separatorBuilder: (c, i) => Divider(height: 1),
        itemCount: log.length,
      );
    }

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
      return Chip(
        avatar: Icon(Icons.wifi_rounded, size: 20, color: Colors.white),
        backgroundColor: Colors.blue,
        label: Text("Tarama Aktif", style: TextStyle(color: Colors.white)),
      );
    }

    return Chip(
      avatar: Icon(Icons.wifi_off_rounded, size: 20),
      backgroundColor: Colors.grey,
      label: Text("Tarama Devre Dışı"),
    );
  }

  get _scannerStart {
    return InkWell(
      onTap: () => startMethod(),
      child: Chip(
        avatar: Icon(Icons.wifi_off_rounded, size: 20),
        backgroundColor: Colors.grey,
        label: Text("Taramayı Başlat"),
      ),
    );
  }

  get _scannerStop {
    return InkWell(
      onTap: () => bleRadar.stop(),
      child: Chip(
        avatar: Icon(Icons.wifi_rounded, size: 20, color: Colors.white),
        backgroundColor: Colors.blue,
        label: Text("Taramayı Bitir", style: TextStyle(color: Colors.white)),
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

  startScanQR() async {
    final qrCode = await Navigator.push(context, MaterialPageRoute(builder: (_) => QRScanner()));
    if (qrCode == null || qrCode == "") {
      return;
    }

    String base64Text = qrCode.split("").reversed.join("") + "==";
    List<String> content = utf8.decode(base64Decode(base64Text)).split("|");

    if (content.isEmpty) return;
    addLog("qrCode: $content");

    String terminalName = content[0];
    DateTime time = DateTime.parse(content[1]);
    DateTime nowTime = DateTime.now();
    int timeoutSecond = int.parse(content[2]);
    int differenceSeconds = nowTime.difference(time).inSeconds;
    String message = "";
    Icon icon;
    if (differenceSeconds > timeoutSecond) {
      message = "QR kodun süresi dolmuştur!";
      icon = Icon(Icons.timer_off_rounded, color: Colors.red);
    } else {
      message = "Geçiş Başarılı"; //\n //$terminalName\n //${content[1]}";
      icon = Icon(Icons.done_rounded, color: Colors.green[600]);
    }

    print("---time: $time");
    print("nowTime: $nowTime");
    print("differenceSeconds: $differenceSeconds");

    showDialog(
        context: context,
        barrierDismissible: true,
        builder: (c) {
          return AlertDialog(
              content: ListTile(
            leading: icon,
            title: Text(terminalName),
            subtitle: Text(message),
          ));
        });
  }
}
