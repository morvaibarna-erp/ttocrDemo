import UIKit

@objc(OCRViewManager)
class OCRViewManager: RCTViewManager {
  override class func requiresMainQueueSetup() -> Bool {
    return true
  }

  override func view() -> SimpleViewProxy? {
    return SimpleViewProxy()
  }
}
