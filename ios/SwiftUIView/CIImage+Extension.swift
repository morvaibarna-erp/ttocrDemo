//
//  CIImage+Extension.swift
//  OCRNativeIOS
//
//  Created by Barna on 05/09/2024.
//

import CoreImage

extension CIImage {
    
    var cgImage: CGImage? {
        let ciContext = CIContext()
        
        guard let cgImage = ciContext.createCGImage(self, from: self.extent) else {
            return nil
        }
        
        return cgImage
    }
    
}
