//
//  ModelRecognition.swift
//  OCRNativeIOS
//
//  Created by Barna on 09/09/2024.
//

import Combine
import CoreML
import PhotosUI
import SwiftUI
import TensorFlowLite
import Vision

extension ViewController {
    func recognizeText(from image: UIImage) {
        // Step 1: Load the model
        let modelFilePath = Bundle.main.url(
            forResource: "recognition",
            withExtension: "tflite"
        )!.path

        let interpreter: Interpreter
        do {
            interpreter = try Interpreter(
                modelPath: modelFilePath,
                delegates: []
            )
            try interpreter.allocateTensors()
        } catch {
            return
        }

        guard let pixelBuffer = preprocessImage(image) else {
            return
        }

        do {
            try interpreter.copy(pixelBuffer, toInputAt: 0)
        } catch {
            return
        }

        do {
            try interpreter.invoke()
        } catch {
            return
        }

        guard let outputTensor = try? interpreter.output(at: 0) else {
            return
        }

        OCRResults.append(postprocessOutput(outputTensor)!)
    }

    // MARK: - Preprocess Image

    private func preprocessImage(_ image: UIImage) -> Data? {
        let targetSize = CGSize(width: 200, height: 31)
        UIGraphicsBeginImageContextWithOptions(targetSize, false, 1.0)
        image.draw(in: CGRect(origin: .zero, size: targetSize))
        let resizedImage = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()

        guard let cgImage = resizedImage?.cgImage else {
            return nil
        }

        let width = cgImage.width
        let height = cgImage.height

        // Allocate buffer for Float32 data (assuming 1 channel for grayscale)
        let byteCount = width * height * 1 * MemoryLayout<Float32>.stride
        var floatData = Data(count: byteCount)

        // Prepare CGContext to extract pixel data as grayscale
        let colorSpace = CGColorSpaceCreateDeviceGray()
        let rawBytesPerRow = width // 1 byte per pixel for grayscale
        var rawPixelData = [UInt8](repeating: 0, count: rawBytesPerRow * height)

        guard let context = CGContext(data: &rawPixelData,
                                      width: width,
                                      height: height,
                                      bitsPerComponent: 8,
                                      bytesPerRow: rawBytesPerRow,
                                      space: colorSpace,
                                      bitmapInfo: CGImageAlphaInfo.none.rawValue)
        else {
            return nil
        }

        // Draw the image onto the context
        context.draw(cgImage, in: CGRect(x: 0, y: 0, width: width, height: height))

        // Convert the raw pixel data to Float32 normalized values
        floatData.withUnsafeMutableBytes { (floatBufferPointer: UnsafeMutableRawBufferPointer) in
            let floatPointer = floatBufferPointer.bindMemory(to: Float32.self)
            for i in 0..<rawPixelData.count {
                floatPointer[i] = Float32(rawPixelData[i]) / 255.0
            }
        }

        return floatData
    }

    // MARK: - Postprocess Output

    private func postprocessOutput(_ outputTensor: Tensor) -> String? {
        let outputData = outputTensor.data
        let recognitionModelOutputSize = outputData.count / MemoryLayout<Int64>.stride // Assuming each output is Int64

        var recognizedText = ""

        for i in 0..<recognitionModelOutputSize {
            // Extract each Int64 value (this will correspond to an index in your alphabet)
            let alphabetIndex = outputData.withUnsafeBytes { (pointer: UnsafeRawBufferPointer) -> Int64 in
                pointer.load(fromByteOffset: i * MemoryLayout<Int64>.stride, as: Int64.self)
            }

            // Map the index to a character in the alphabet
            if alphabetIndex >= 0, alphabetIndex < alphabets.count {
                recognizedText += String(alphabets[Int(alphabetIndex)])
            }
        }

        return recognizedText
    }
}
