
#import <Foundation/Foundation.h>
#import "React/RCTViewManager.h"
#import <React/RCTBridgeModule.h>

@interface
  RCT_EXTERN_MODULE(OCRViewManager, RCTViewManager)
  RCT_EXPORT_VIEW_PROPERTY(gyariSzam, NSString)
  RCT_EXPORT_VIEW_PROPERTY(onCancelPress, RCTBubblingEventBlock)
  RCT_EXPORT_VIEW_PROPERTY(onSuccess, RCTBubblingEventBlock)
@end
