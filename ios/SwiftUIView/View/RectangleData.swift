//
//  RectangleData.swift
//  OCRNativeIOS
//
//  Created by Barna on 05/09/2024.
//

import Combine
import SwiftUI

class RectangleData: ObservableObject {
    static let shared = RectangleData()
    @Published var rect: CGRect = .init(
        x: UIScreen.screenWidth * 0.25,
        y: UIScreen.screenHeight * 0.25,
        width: UIScreen.screenWidth * 0.5,
        height: UIScreen.screenHeight * 0.5)
}

extension UIScreen {
    static let screenWidth = UIScreen.main.bounds.size.width
    static let screenHeight = UIScreen.main.bounds.size.height
    static let screenSize = UIScreen.main.bounds.size
}
