//
//  BleRadarErrorDelegate.swift
//  bleScan
//
//  Created by developer on 20.01.2021.
//  Copyright © 2021 ahmet. All rights reserved.
//

import Foundation

enum BleDeviceErrors {
    case BLUETOOTH_NOT_ACTIVE
    case UNSUPPORT_BLUETOOTH_LE
}

enum BleScanErrors {
    case NOT_START_SCANNER
    case NOT_CONNECTED_DEVICE
}

protocol BleRadarErrorDelegate: class {
    
    func onDeviceError(deviceError:BleDeviceErrors)
    
    func onScanError(scanError:BleScanErrors)
    
}
