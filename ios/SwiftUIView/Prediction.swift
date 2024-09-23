//
//  Prediction.swift
//  OCRNativeIOS
//
//  Created by Barna on 09/09/2024.
//

import Foundation

typealias XYXY = (x1: Float, y1: Float, x2: Float, y2: Float)

// MARK: Prediction
struct Prediction {
    let id = UUID()
    let classIndex: Int
    let score: Float
    let xyxy: XYXY    
    let inputImgSize: CGSize
}
