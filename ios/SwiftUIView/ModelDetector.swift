import Combine
import CoreML
import PhotosUI
import SwiftUI
import TensorFlowLite
import Vision

extension ViewController {
    func runDisplayDetector(imageToProcess: UIImage) {
//        NSLog("Start inference using TFLite")
        
        let modelFilePath = Bundle.main.url(
            forResource: "detect",
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
            
        let input: Tensor
        do {
            input = try interpreter.input(at: 0)
        } catch {
            return
        }
            
        let inputSize = CGSize(
            width: input.shape.dimensions[1],
            height: input.shape.dimensions[2]
        )
            
        guard let data = imageToProcess.resized(to: inputSize).normalizedDataFromImage() else {
            return
        }
            
        let boxesOutputTensor: Tensor
        do {
            try interpreter.copy(data, toInputAt: 0)
            try interpreter.invoke()
                
            boxesOutputTensor = try interpreter.output(at: 0)
        } catch {
            return
        }
            
        let boxesOutputShapeDim = boxesOutputTensor.shape.dimensions
        
        let numClasses = boxesOutputShapeDim[0]
                        
        let boxesOutput = [Float](unsafeData: boxesOutputTensor.data)

        // Convert output to array of predictions
        var predictions = self.getPredictionsFromOutput(
            output: boxesOutput! as [NSNumber],
            rows: boxesOutputShapeDim[1],
            columns: boxesOutputShapeDim[2],
            numberOfClasses: numClasses,
            inputImgSize: inputSize
        )
                        
        // Remove predictions with confidence score lower than threshold
        predictions.removeAll { $0.score < confidenceThreshold }
                        
        guard !predictions.isEmpty else {
            return
        }
        // Group predictions by class
        let groupedPredictions = Dictionary(grouping: predictions) { prediction in
            prediction.classIndex
        }
            
        var nmsPredictions: [Prediction] = []
        _ = groupedPredictions.mapValues { predictions in
            nmsPredictions.append(
                contentsOf: self.nonMaximumSuppression(
                    predictions: predictions,
                    iouThreshold: iouThreshold,
                    limit: 100
                ))
        }
        guard !nmsPredictions.isEmpty else {
            return
        }
            
        // Scale boxes to input size
        nmsPredictions = nmsPredictions.map { prediction in
            Prediction(
                classIndex: prediction.classIndex,
                score: prediction.score,
                xyxy: (
                    prediction.xyxy.x1 * Float(inputSize.width),
                    prediction.xyxy.y1 * Float(inputSize.height),
                    prediction.xyxy.x2 * Float(inputSize.width),
                    prediction.xyxy.y2 * Float(inputSize.height)
                ),
                inputImgSize: prediction.inputImgSize
            )
        }
            
        self.predictions = nmsPredictions
            
    }

    func getPredictionsFromOutput(
        output: [NSNumber],
        rows: Int,
        columns: Int,
        numberOfClasses: Int,
        inputImgSize: CGSize
    ) -> [Prediction] {
        guard !output.isEmpty else {
            return []
        }
        var predictions = [Prediction]()
        for i in 0 ..< columns {
            let centerX = Float(truncating: output[0 * columns + i])
            let centerY = Float(truncating: output[1 * columns + i])
            let width = Float(truncating: output[2 * columns + i])
            let height = Float(truncating: output[3 * columns + i])
                
            let (classIndex, score) = {
                var classIndex = 0
                var heighestScore: Float = 0
                for j in 0 ..< numberOfClasses {
                    let score = Float(truncating: output[(4 + j) * columns + i])
                    if score > heighestScore {
                        heighestScore = score
                        classIndex = j
                    }
                }
                return (classIndex, heighestScore)
            }()
                
            // Convert box from xywh to xyxy
            let left = centerX - width / 2
            let top = centerY - height / 2
            let right = centerX + width / 2
            let bottom = centerY + height / 2
                
            let prediction = Prediction(
                classIndex: classIndex,
                score: score,
                xyxy: (left, top, right, bottom),
                inputImgSize: inputImgSize
            )
            predictions.append(prediction)
        }
            
        return predictions
    }
    
    private func IOU(a: XYXY, b: XYXY) -> Float {
        // Calculate the intersection coordinates
        let x1 = max(a.x1, b.x1)
        let y1 = max(a.y1, b.y1)
        let x2 = max(a.x2, b.x2)
        let y2 = max(a.y1, b.y2)
        
        // Calculate the intersection area
        let intersection = max(x2 - x1, 0) * max(y2 - y1, 0)
        
        // Calculate the union area
        let area1 = (a.x2 - a.x1) * (a.y2 - a.y1)
        let area2 = (b.x2 - b.x1) * (b.y2 - b.y1)
        let union = area1 + area2 - intersection
        
        // Calculate the IoU score
        let iou = intersection / union
        
        return iou
    }
    
    func nonMaximumSuppression(
        predictions: [Prediction],
        iouThreshold: Float,
        limit: Int
    ) -> [Prediction] {
        guard !predictions.isEmpty else {
            return []
        }
            
        let sortedIndices = predictions.indices.sorted {
            predictions[$0].score > predictions[$1].score
        }
            
        var selected: [Prediction] = []
        var active = [Bool](repeating: true, count: predictions.count)
        var numActive = active.count

        // The algorithm is simple: Start with the box that has the highest score.
        // Remove any remaining boxes that overlap it more than the given threshold
        // amount. If there are any boxes left (i.e. these did not overlap with any
        // previous boxes), then repeat this procedure, until no more boxes remain
        // or the limit has been reached.
        outer: for i in 0 ..< predictions.count {
            if active[i] {
                let boxA = predictions[sortedIndices[i]]
                selected.append(boxA)
                    
                if selected.count >= limit { break }

                for j in i + 1 ..< predictions.count {
                    if active[j] {
                        let boxB = predictions[sortedIndices[j]]
                            
                        if self.IOU(a: boxA.xyxy, b: boxB.xyxy) > iouThreshold {
                            active[j] = false
                            numActive -= 1
                               
                            if numActive <= 0 { break outer }
                        }
                    }
                }
            }
        }
        return selected
    }
}

extension UIImage {
    func resized(to newSize: CGSize, scale: CGFloat = 1) -> UIImage {
        let format = UIGraphicsImageRendererFormat.default()
        format.scale = scale
        let renderer = UIGraphicsImageRenderer(size: newSize, format: format)
        let image = renderer.image { _ in
            draw(in: CGRect(origin: .zero, size: newSize))
        }
        return image
    }
    
    func normalized() -> [Float32]? {
        guard let cgImage = self.cgImage else {
            return nil
        }
        let w = cgImage.width
        let h = cgImage.height
        let bytesPerPixel = 4
        let bytesPerRow = bytesPerPixel * w
        let bitsPerComponent = 8
        var rawBytes = [UInt8](repeating: 0, count: w * h * 4)
        rawBytes.withUnsafeMutableBytes { ptr in
            if let cgImage = self.cgImage,
               let context = CGContext(
                   data: ptr.baseAddress,
                   width: w,
                   height: h,
                   bitsPerComponent: bitsPerComponent,
                   bytesPerRow: bytesPerRow,
                   space: CGColorSpaceCreateDeviceRGB(),
                   bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
               )
            {
                let rect = CGRect(x: 0, y: 0, width: w, height: h)
                context.draw(cgImage, in: rect)
            }
        }
        var normalizedBuffer = [Float32](repeating: 0, count: w * h * 3)
        for i in 0 ..< w * h {
            normalizedBuffer[i] = Float32(rawBytes[i * 4 + 0]) / 255.0
            normalizedBuffer[w * h + i] = Float32(rawBytes[i * 4 + 1]) / 255.0
            normalizedBuffer[w * h * 2 + i] = Float32(rawBytes[i * 4 + 2]) / 255.0
        }
        return normalizedBuffer
    }
    
    func transformToUpOrientation() -> UIImage {
        UIGraphicsBeginImageContext(size)
        draw(at: .zero)
        let newImage = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        return newImage ?? self
    }
    
    /// Converts UIImage to Data
    ///
    /// Ignores alpha channel and normalizes pixel values
    ///
    /// - Parameter image: Image that will be converted to data
    func normalizedDataFromImage() -> Data? {
        let imageSize = size
        
        guard let cgImage: CGImage = cgImage else {
            return nil
        }
        guard let context = CGContext(
            data: nil,
            width: cgImage.width, height: cgImage.height,
            bitsPerComponent: 8, bytesPerRow: cgImage.width * 4,
            space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGImageAlphaInfo.noneSkipFirst.rawValue
        ) else {
            return nil
        }

        context.draw(cgImage, in: CGRect(x: 0, y: 0, width: cgImage.width, height: cgImage.height))
        guard let imageData = context.data else { return nil }

        var inputData = Data()
        for row in 0 ..< Int(imageSize.width) {
            for col in 0 ..< Int(imageSize.height) {
                let offset = 4 * (row * context.width + col)
                // (Ignore offset 0, the unused alpha channel)
                let red = imageData.load(fromByteOffset: offset + 1, as: UInt8.self)
                let green = imageData.load(fromByteOffset: offset + 2, as: UInt8.self)
                let blue = imageData.load(fromByteOffset: offset + 3, as: UInt8.self)

                // Normalize channel values to [0.0, 1.0]. This requirement varies
                // by model. For example, some models might require values to be
                // normalized to the range [-1.0, 1.0] instead, and others might
                // require fixed-point values or the original bytes.
                var normalizedRed = Float32(red) / 255.0
                var normalizedGreen = Float32(green) / 255.0
                var normalizedBlue = Float32(blue) / 255.0

                // Append normalized values to Data object in RGB order.
                let elementSize = MemoryLayout.size(ofValue: normalizedRed)
                var bytes = [UInt8](repeating: 0, count: elementSize)
                memcpy(&bytes, &normalizedRed, elementSize)
                inputData.append(&bytes, count: elementSize)
                memcpy(&bytes, &normalizedGreen, elementSize)
                inputData.append(&bytes, count: elementSize)
                memcpy(&bytes, &normalizedBlue, elementSize)
                inputData.append(&bytes, count: elementSize)
            }
        }
        return inputData
    }
    
    func applyFilter(_ filter: CIFilter) -> UIImage {
        let ciImage = (ciImage ?? CIImage(cgImage: cgImage!))
        filter.setValue(ciImage, forKey: kCIInputImageKey)
        guard let outputImage = filter.outputImage else {
            print("Can not apply filter, outputImage is nil")
            return self
        }
        let context = CIContext(options: nil)
        guard let cgImage = context.createCGImage(outputImage, from: outputImage.extent) else {
            print("Can not create CGImage from outputImage created by filter")
            return self
        }
        return UIImage(cgImage: cgImage, scale: self.scale, orientation: self.imageOrientation)
    }
}

extension UIImage {
    func imageWithInsets(insets: UIEdgeInsets) -> UIImage? {
        UIGraphicsBeginImageContextWithOptions(
            CGSize(width: self.size.width + insets.left + insets.right,
                   height: self.size.height + insets.top + insets.bottom), false, self.scale
        )
        _ = UIGraphicsGetCurrentContext()
        let origin = CGPoint(x: insets.left, y: insets.top)
        self.draw(at: origin)
        let imageWithInsets = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        return imageWithInsets
    }
}


extension Array {
    /// Creates a new array from the bytes of the given unsafe data.
    ///
    /// - Warning: The array's `Element` type must be trivial in that it can be copied bit for bit
    ///     with no indirection or reference-counting operations; otherwise, copying the raw bytes in
    ///     the `unsafeData`'s buffer to a new array returns an unsafe copy.
    /// - Note: Returns `nil` if `unsafeData.count` is not a multiple of
    ///     `MemoryLayout<Element>.stride`.
    /// - Parameter unsafeData: The data containing the bytes to turn into an array.
    init?(unsafeData: Data) {
        guard unsafeData.count % MemoryLayout<Element>.stride == 0 else { return nil }
        self = unsafeData.withUnsafeBytes { .init($0.bindMemory(to: Element.self)) }
    }
}
