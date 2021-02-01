#import "BleRadarPlugin.h"
#if __has_include(<ble_radar/ble_radar-Swift.h>)
#import <ble_radar/ble_radar-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "ble_radar-Swift.h"
#endif

@implementation BleRadarPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftBleRadarPlugin registerWithRegistrar:registrar];
}
@end
