    import Flutter
    import UIKit
    import CoreBluetooth
    
    public class SwiftBleRadarPlugin: NSObject, FlutterPlugin,
                                      BleRadarDelegate,
                                      BleRadarErrorDelegate{
        
        private var actionType:Int!
        private let ACTION_READ:Int = 0
        private let ACTION_WRITE:Int = 1
        
        var callArguments:[String:Any]!
        
        var filterUUID:[CBUUID] = [CBUUID]()
        var maxRssi:Int32 = 0
        var autoConnect:Bool = false
        var isOpenBluetooth:Bool = false
        var isScanning:Bool = false
        
        var bleRadar:BleRadar!
        
        var eventSinkBluetoothStatus:BleEventSink!
        var eventSinkScanningStatus:BleEventSink!
        var eventSinkDetectDevice:BleEventSink!
        var eventSinkConnectedDevice:BleEventSink!
        var eventSinkServicesDiscovered:BleEventSink!
        var eventSinkReadCharacteristic:BleEventSink!
        var eventSinkWriteCharacteristic:BleEventSink!
        
        public override init() {
            super.init()
            
            bleRadar = BleRadar(self)
            
            eventSinkBluetoothStatus = BleEventSink()
            eventSinkScanningStatus = BleEventSink()
            eventSinkDetectDevice = BleEventSink()
            eventSinkConnectedDevice = BleEventSink()
            eventSinkServicesDiscovered = BleEventSink()
            eventSinkReadCharacteristic = BleEventSink()
            eventSinkWriteCharacteristic = BleEventSink()
            
            NotificationCenter.default.addObserver(self, selector: #selector(applicationDidBecomeActive), name: UIApplication.willEnterForegroundNotification, object: nil)
            NotificationCenter.default.addObserver(self, selector: #selector(applicationDidEnterBackground), name: UIApplication.didEnterBackgroundNotification, object: nil)
            
        }
        
        
        @objc private func applicationDidBecomeActive(notification: NSNotification) {
            self.startMethod()
           print("BleRadar -> ACTIVE")
        }
        
        
        @objc private func applicationDidEnterBackground(notification: NSNotification) {
            self.bleRadar.stopScan()
           print("BleRadar -> BACKGROUND")
        }
        
        func startMethod(){
            self.bleRadar.startScan(
                filter: self.filterUUID,
                maxRssi: self.maxRssi)
           print("BleRadar -> startMethod()")
        }
        
        public static func register(with registrar: FlutterPluginRegistrar) {
            let channel = FlutterMethodChannel(name: "ble_radar", binaryMessenger: registrar.messenger())
            let instance = SwiftBleRadarPlugin()
            registrar.addMethodCallDelegate(instance, channel: channel)
            
            FlutterEventChannel(name: "bluetoothStatusStream", binaryMessenger: registrar.messenger())
                .setStreamHandler(instance.eventSinkBluetoothStatus)
            
            FlutterEventChannel(name: "scanningStatusStream", binaryMessenger: registrar.messenger())
                .setStreamHandler(instance.eventSinkScanningStatus)
            
            FlutterEventChannel(name: "detectDeviceStream", binaryMessenger: registrar.messenger())
                .setStreamHandler(instance.eventSinkDetectDevice)
            
            FlutterEventChannel(name: "connectedDeviceStream", binaryMessenger: registrar.messenger())
                .setStreamHandler(instance.eventSinkConnectedDevice)
            
            FlutterEventChannel(name: "servicesDiscoveredStream", binaryMessenger: registrar.messenger())
                .setStreamHandler(instance.eventSinkServicesDiscovered)
            
            FlutterEventChannel(name: "readCharacteristicStream", binaryMessenger: registrar.messenger())
                .setStreamHandler(instance.eventSinkReadCharacteristic)
            
            FlutterEventChannel(name: "writeCharacteristicStream", binaryMessenger: registrar.messenger())
                .setStreamHandler(instance.eventSinkWriteCharacteristic)
        }
        
        
        
        public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
            
            
            switch call.method {
            case "startScan":
                
                
                guard let args = call.arguments else {
                    return
                  }
                if let myArgs = args as? [String: Any],
                   let _autoConnect = myArgs["autoConnect"] as? Bool,
                   let _filterUUID = myArgs["filterUUID"] as? [String],
                   let _maxRssi = myArgs["maxRssi"] as? Int32 {
                    for uuid in _filterUUID {
                        self.filterUUID.append(CBUUID.init(string: uuid))
                    }
                    self.autoConnect=_autoConnect
                    self.maxRssi = _maxRssi
                    self.startMethod()
                    
                }
                
                
                
                break
            case "stopScan":
                bleRadar.stoptimerScanner()
                bleRadar.stopScan()
                break
            case "isOpenBluetooth":
                result(isOpenBluetooth)
                break
            case "isScanning":
                result(isScanning)
                break
            case "connectDevice":
                bleRadar.connectDevice()
                break
            case "disconnectDevice":
                bleRadar.disconnectDevice()
                break
            case "readCharacteristic":
               print("BleRadar -> readCharacteristic")
                
                actionType = ACTION_READ
                
                guard let args = call.arguments else {
                    break
                }
                
                if let myArgs = args as? [String: Any]{
                    callArguments = myArgs
                    let service = myArgs["serviceUUID"] as? String ?? ""
                    self.bleRadar.connectService(serviceUUID: CBUUID.init(string: service))
                }
                
                break
            case "writeCharacteristic":
                actionType = ACTION_WRITE
               print("BleRadar -> writeCharacteristic")
                
                guard let args = call.arguments else {
                    break
                }
                if let myArgs = args as? [String: Any]{
                    callArguments = myArgs
                    let service = myArgs["serviceUUID"] as? String ?? ""
                    self.bleRadar.connectService(serviceUUID: CBUUID.init(string: service))
                }
                
                break
                
            default:
                break
                
            }
            
        }
        
        
        
        func isOpenBluetooth(status: Bool) {
            if isOpenBluetooth != status {
                isOpenBluetooth = status
                eventSinkBluetoothStatus.eventSink?(status);
            }
        }
        
        func isScanning(status: Bool) {
            if isScanning != status{
                isScanning = status
                eventSinkScanningStatus.eventSink?(status);
            }
        }
        
        func onDetectDevice(device: CBPeripheral, rssi: NSNumber) -> Bool {
            let dic = [
                "rssi": rssi.stringValue,
                "name": device.name,
                "mac": device.identifier.uuidString
            ]
            let encoder = JSONEncoder()
            if let jsonData = try? encoder.encode(dic) {
                if let jsonString = String(data: jsonData, encoding: .utf8) {
                    eventSinkDetectDevice.eventSink?(jsonString);
                }
            }
            return self.autoConnect
        }
        
        func onConnectedDevice(status:Bool ,device: CBPeripheral) {
            eventSinkConnectedDevice.eventSink?(status)
        }
        
        func onDiscoverService(services: [CBService]) {
            
            var servicesUUIDList = [String]()
            
            services.forEach { (service) in
                servicesUUIDList.append(service.uuid.uuidString)
            }
            
            let sendData = stringify(json: servicesUUIDList)
           print("BleRadar -> sendData: " + sendData)
            
            eventSinkServicesDiscovered.eventSink?(sendData)
            
            
        }
        
        func onDiscoverCharacteristic(characteristic: [CBCharacteristic]) {
            
            switch actionType {
            case ACTION_READ:
                
                let characteristic = callArguments["characteristicUUID"] as? String ?? ""
                
                
                let status = bleRadar.readCharacteristicValue(
                    characteristicUUID: CBUUID.init(string: characteristic))
                
               print("BleRadar -> readCharacteristic ",status)
                
                break
            case ACTION_WRITE:
                let characteristic = callArguments["characteristicUUID"] as? String ?? ""
                
                let data = callArguments["data"] as? String ?? ""
                
                
                let status = bleRadar.writeCharacteristicValue(
                    characteristicUUID: CBUUID.init(string: characteristic),
                    data: data
                )
               print("BleRadar -> readCharacteristic ",status)
                
                
                
                break
            default:
                break
            }
            
        }
        
        func onReadCharacteristic(characteristic: CBCharacteristic, stringData: String) {
            eventSinkReadCharacteristic.eventSink?(stringData);
        }
        
        func onWriteCharacteristic(characteristic: CBCharacteristic) {
            eventSinkWriteCharacteristic.eventSink?(true);
        }
        
        func onDeviceError(deviceError: BleDeviceErrors) {
            
        }
        
        func onScanError(scanError: BleScanErrors) {
            
        }
        
        func stringify(json: Any, prettyPrinted: Bool = false) -> String {
            var options: JSONSerialization.WritingOptions = []
            if prettyPrinted {
                options = JSONSerialization.WritingOptions.prettyPrinted
            }
            
            do {
                let data = try JSONSerialization.data(withJSONObject: json, options: options)
                if let string = String(data: data, encoding: String.Encoding.utf8) {
                    return string
                }
            } catch {
                print(error)
            }
            
            return ""
        }
        
    }
    
    
    
    class BleEventSink: NSObject, FlutterStreamHandler {
        public var eventSink: FlutterEventSink?;
        
        func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
            eventSink = events;
            return nil;
        }
        
        func onCancel(withArguments arguments: Any?) -> FlutterError? {
            eventSink = nil;
            return nil;
        }
        
    }
