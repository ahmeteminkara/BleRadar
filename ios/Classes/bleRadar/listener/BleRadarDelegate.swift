//
//  BleRadarDelegate.swift
//  bleScan
//
//  Created by developer on 20.01.2021.
//  Copyright © 2021 ahmet. All rights reserved.
//

import Foundation
import CoreBluetooth

protocol BleRadarDelegate: class {
    
    func isOpenBluetooth(status:Bool)
    
    func isScanning(status:Bool)
    
    func onDetectDevice( device: CBPeripheral,rssi: NSNumber) -> Bool
    
    func onConnectedDevice(status:Bool,device:CBPeripheral)
    
    func onDiscoverService(services:[CBService])
    
    func onDiscoverCharacteristic(characteristic:[CBCharacteristic])
    
    func onReadCharacteristic(characteristic:CBCharacteristic,stringData:String)
    
    func onWriteCharacteristic(characteristic:CBCharacteristic)
    
}
