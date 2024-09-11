//
//  FlashLightButton.swift
//  OCRNativeIOS
//
//  Created by Barna on 04/09/2024.
//

import SwiftUI

struct FlashLightButton: View {
    var size: CGFloat
    @ObservedObject var OCR: ViewController

    var body: some View {
        Button {
            OCR.torchIsOn = !OCR.torchIsOn
            OCR.toggleTorch(on: OCR.torchIsOn)
        } label: {
            ZStack {
                RoundedRectangle(cornerRadius: 100)
                    .frame(width: size, height: size)
                    .foregroundColor(.white)
                Image(!OCR.torchIsOn ? "flashlightOff" : "flashlightOn")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(height: 70)
            }
        }
        
        .buttonStyle(ScaleButtonStyle())
    }
}

struct NoFlashLight: View {
    var size: CGFloat
    var body: some View {
            ZStack {
                RoundedRectangle(cornerRadius: 100)
                    .frame(width: size, height: size)
                    .foregroundColor(.gray)
                Image(systemName: "lightbulb.slash.fill")
                    .foregroundColor(.black)
            }
    }
}
