//
//  CloseCameraButton.swift
//  OCRNativeIOS
//
//  Created by Barna on 04/09/2024.
//

import os
import SwiftUI

struct CloseCameraButton: View {
    let size: CGFloat
    let logger = Logger()
    var onTap: () -> Void

    var body: some View {
        Button {
            onTap()
        }
        label: {
            ZStack {
                RoundedRectangle(cornerRadius: size)
                    .foregroundColor(.gray.opacity(0.5))
                Image(systemName: "xmark")
                    .foregroundColor(.white)
            }
            .frame(width: size, height: size)
        }
        .buttonStyle(ScaleButtonStyle())
    }
}

struct ScaleButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.7 : 1)
    }
}

func onTapInit() {
    print("tapped")
}

#Preview {
    CloseCameraButton(size: 50, onTap: onTapInit)
}
