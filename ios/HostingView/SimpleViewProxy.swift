import SwiftUI
class RNData: ObservableObject {
  static let shared = RNData()

  @Published var gyariSzam: NSString = ""
  @Published var onCancelPress: RCTBubblingEventBlock = {_ in }
  @Published var onSuccess: RCTBubblingEventBlock = {_ in }
}
class SimpleViewProxy: UIView {
  
  var returningView: UIView?
  let dataStore: RNData = .init()
  
  override init(frame: CGRect) {
    super.init(frame: frame)
    let vc = UIHostingController(rootView: OCRView())
    vc.view.frame = bounds
    self.addSubview(vc.view)
    self.returningView = vc.view
  }
  
  required init?(coder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }
  
  @objc var gyariSzam: NSString = "" {
    didSet{
      RNData.shared.gyariSzam = gyariSzam
    }
  }
  
  @objc var onCancelPress: RCTBubblingEventBlock = {_ in} {
    didSet{
      RNData.shared.onCancelPress = onCancelPress
    }
  }
  
  @objc var onSuccess: RCTBubblingEventBlock = {_ in} {
    didSet{
      RNData.shared.onSuccess = onSuccess
    }
  }
  
  override func layoutSubviews() {
    super.layoutSubviews()
    self.returningView?.frame = bounds
  }
  
}
