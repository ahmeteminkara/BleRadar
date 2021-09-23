//
//  File.swift
//  bleScan
//
//  Created by developer on 19.01.2021.
//  Copyright © 2021 ahmet. All rights reserved.
//

import Foundation
import AudioToolbox
import CoreBluetooth

class BleRadar:NSObject, CBCentralManagerDelegate,CBPeripheralDelegate {
    weak open var delegate:BleRadarDelegate?
    weak open var errorDelegate:BleRadarErrorDelegate?
    
    private var timerScanner : Timer?
    private var filter:[CBUUID]!
    private var maxRssi:Int32!
    var isActivedBluetooth:Bool = false
    var isScanning:Bool = false
    
    var vibration:Bool = false
    
    private var centralManager: CBCentralManager!
    private var myPeripheral: CBPeripheral!
    private var myPeripheralRssi: Int32!
    
    var myService: CBService!
    
    var gattServiceCharacteristic = [CBUUID:[CBCharacteristic]]()
    
    
    init(_ delegate: Any) {
        super.init()
        
        self.delegate = delegate as? BleRadarDelegate
        self.errorDelegate = delegate as? BleRadarErrorDelegate
        
        self.centralManager = CBCentralManager(delegate: self, queue:nil)
    }
    
    func startTimer () {
      guard timerScanner == nil else { return }

      timerScanner =  Timer.scheduledTimer(
        timeInterval: TimeInterval(0.7),
          target      : self,
          selector    : #selector(BleRadar.restart),
          userInfo    : nil,
          repeats     : true)
    }

    func stoptimerScanner() {
      timerScanner?.invalidate()
      timerScanner = nil
    }
    
    @objc private func restart(){
        self.stopScan()
        self.gattServiceCharacteristic.removeAll()
        self.startScan(filter: self.filter, maxRssi: self.maxRssi, vibrate: self.vibration)
    }
    
    func startScan(filter:[CBUUID] = [],maxRssi:Int32, vibrate:Bool) {
       //print("BleRadar -> Started Scan")
        if !isActivedBluetooth {
            return
        }
        self.filter = filter
        self.maxRssi = maxRssi
        self.vibration = vibrate
        centralManager.scanForPeripherals(withServices: filter,options: [CBCentralManagerScanOptionAllowDuplicatesKey : true])
        isScanning = true
        delegate?.isScanning(status:true)
        
        self.startTimer()
    }
    
    func stopScan(){
       //print("BleRadar -> Stoped Scan")
        centralManager.stopScan()
        isScanning = false
        delegate?.isScanning(status:false)
    }
    
    func connectDevice(){
        self.stoptimerScanner()
        self.stopScan()
        self.myPeripheral.delegate = self
        self.centralManager.connect(self.myPeripheral, options: nil)
    }
    
    
    internal func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        self.delegate?.onConnectedDevice(status: true, device: peripheral)
        self.myPeripheral.discoverServices(nil)

    }
    
    
    
    internal func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
       print("BleRadar -> servis keşfedildi")
        self.delegate?.onDiscoverService(services: peripheral.services ?? [])

    }
    
    
    func connectService(serviceUUID:CBUUID) {
        if  let i = self.myPeripheral.services?.firstIndex(
                where: {$0.uuid == serviceUUID}) {
           print("BleRadar -> connectService")
            
            self.myService = self.myPeripheral.services![i]
            self.myPeripheral.discoverCharacteristics(nil, for: self.myService)
        }
        
    }
    
    func disconnectDevice() {
        
        if(self.myPeripheral != nil){
            centralManager.cancelPeripheralConnection(self.myPeripheral)
        }
    }
    
    func getCharacteristic( characteristicUUID:CBUUID) -> CBCharacteristic! {
        let characteristics = self.myService.characteristics ?? []
        if let i = characteristics.firstIndex(where: {$0.uuid == characteristicUUID}){
           return characteristics[i]
        }
        return nil

    }
    
    func readCharacteristicValue(characteristicUUID:CBUUID) -> Bool{
       print("BleRadar -> readCharacteristicValue")
        
        let characteristic:CBCharacteristic = self.getCharacteristic(
            characteristicUUID: characteristicUUID
        )
        
        if characteristic.properties.contains(.read) {
            self.myPeripheral.readValue(for: characteristic)
            return true
        }else{
            return false
        }
    }
    
    func writeCharacteristicValue(characteristicUUID:CBUUID,data:String) -> Bool{
        
        let characteristic:CBCharacteristic = self.getCharacteristic(
            characteristicUUID: characteristicUUID
        )
        
        
        if characteristic.properties.contains(.write) {
            
            let _data:Data = data.data(using: String.Encoding.utf8)!
            self.myPeripheral.writeValue(_data, for: characteristic, type: .withResponse)
            return true
        }else{
            return false
        }
        
    }
    
    internal func centralManagerDidUpdateState(_ central: CBCentralManager) {
        isActivedBluetooth = central.state == CBManagerState.poweredOn
        
        
        switch central.state {
        case .unauthorized,.unsupported:
            errorDelegate?.onDeviceError(deviceError: .UNSUPPORT_BLUETOOTH_LE)
        case .poweredOff:
            delegate?.isOpenBluetooth(status: false)
            errorDelegate?.onDeviceError(deviceError: .BLUETOOTH_NOT_ACTIVE)
            break
        case .poweredOn:
            delegate?.isOpenBluetooth(status: true)
            break
        default:
            break
        }
    }
    
    internal func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
       print("BleRadar -> okuma isteği gitti")
        
        self.delegate?.onReadCharacteristic(
            characteristic: characteristic,
            stringData: String(data: characteristic.value!, encoding: String.Encoding.utf8) ?? ""
        )
    }
    
    
    
    func peripheral(_ peripheral: CBPeripheral, didWriteValueFor characteristic: CBCharacteristic, error: Error?) {
       print("BleRadar -> yazma isteği gitti")
        
        
        self.delegate?.onWriteCharacteristic(characteristic: characteristic)
    }
    
    internal func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber) {
        
        
        if (RSSI.int64Value < 0 && RSSI.int64Value > self.maxRssi) {
            let name = peripheral.name ?? "Bilinmeyen"
           print("BleRadar -> Cihaz --> ",String(name),", RSSI: ",RSSI)
            
            let isConnectDevice:Bool = delegate?.onDetectDevice(device: peripheral, rssi: RSSI) ?? false
            
            
            self.myPeripheral = peripheral
            
            if(self.vibration){
                print("vibration : \(self.vibration)")
                AudioServicesPlayAlertSound(kSystemSoundID_Vibrate)

            }
            
            if(isConnectDevice){
                self.connectDevice()
            }
        }
    }
    
    
    
    internal func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
       print("BleRadar -> didDiscoverCharacteristicsFor")
        self.delegate?.onDiscoverCharacteristic(characteristic: service.characteristics ?? [])
    }
      
    
    internal func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
        self.delegate?.onConnectedDevice(status: false, device: peripheral)
       print("BleRadar -> bağlantı iptal oldu")

    }
}
