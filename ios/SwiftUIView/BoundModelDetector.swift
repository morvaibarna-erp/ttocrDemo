////
////  BoundModelDetector.swift
////  OCRWrapperTest
////
////  Created by Barna on 13/09/2024.
////
//
//import Combine
//import CoreML
//import PhotosUI
//import SwiftUI
//import TensorFlowLite
//import Vision
//
//
//extension ViewController {
//    func runBoundDisplayDetector(imageToProcess: UIImage) {
////        NSLog("Start inference using TFLite")
//        
//        let modelFilePath = Bundle.main.url(
//            forResource: "train_with_bound_boxes_float16",
//            withExtension: "tflite"
//        )!.path
//        
//        let interpreter: Interpreter
//        do {
//            interpreter = try Interpreter(
//                modelPath: modelFilePath,
//                delegates: []
//            )
//                
//            try interpreter.allocateTensors()
//        } catch {
//            return
//        }
//            
//        let input: Tensor
//        do {
//            input = try interpreter.input(at: 0)
//        } catch {
//            return
//        }
//            
//        let inputSize = CGSize(
//            width: input.shape.dimensions[1],
//            height: input.shape.dimensions[2]
//        )
//            
//        guard let data = imageToProcess.resized(to: inputSize).normalizedDataFromImage() else {
//            return
//        }
//            
//        let boxesOutputTensor: Tensor
//        do {
//            try interpreter.copy(data, toInputAt: 0)
//            try interpreter.invoke()
//                
//            boxesOutputTensor = try interpreter.output(at: 0)
//        } catch {
//            return
//        }
//            
//        let boxesOutputShapeDim = boxesOutputTensor.shape.dimensions
//        
//        let numClasses = boxesOutputShapeDim[1] - 5 // assuming last dimension is [class_scores]
//        let numBoxes = boxesOutputShapeDim[2]
//                        
//        let boxesOutput = [Float](unsafeData: boxesOutputTensor.data)
//
//        // Convert output to array of predictions
//        var predictions = self.getPredictionsFromOutput(
//            output: boxesOutput! as [NSNumber],
//            numBoxes: numBoxes,
//            numClasses: numClasses,
//            inputImgSize: inputSize
//        )
//                        
//        // Remove predictions with confidence score lower than threshold
//        predictions.removeAll { $0.score < confidenceThreshold }
//                        
//        guard !predictions.isEmpty else {
//            return
//        }
//        // Group predictions by class
//        let groupedPredictions = Dictionary(grouping: predictions) { prediction in
//            prediction.classIndex
//        }
//            
//        var nmsPredictions: [Prediction] = []
//        _ = groupedPredictions.mapValues { predictions in
//            nmsPredictions.append(
//                contentsOf: self.nonMaximumSuppression(
//                    predictions: predictions,
//                    iouThreshold: iouThreshold,
//                    limit: 1
//                ))
//        }
//        guard !nmsPredictions.isEmpty else {
//            return
//        }
//            
//        // Scale boxes to input size
//        nmsPredictions = nmsPredictions.map { prediction in
//            Prediction(
//                classIndex: prediction.classIndex,
//                score: prediction.score,
//                xyxy: (
//                    prediction.xyxy.x1 * Float(inputSize.width),
//                    prediction.xyxy.y1 * Float(inputSize.height),
//                    prediction.xyxy.x2 * Float(inputSize.width),
//                    prediction.xyxy.y2 * Float(inputSize.height)
//                ),
//                inputImgSize: prediction.inputImgSize
//            )
//        }
//            
//        self.predictions = nmsPredictions
//            
//    }
//
//    func getPredictionsFromOutput(
//        output: [NSNumber],
//        numBoxes: Int,
//        numClasses: Int,
//        inputImgSize: CGSize
//    ) -> [Prediction] {
//        guard !output.isEmpty else {
//            return []
//        }
//        var predictions = [Prediction]()
//        let boxSize = 4 + 1 + numClasses // [center_x, center_y, width, height, confidence, class_scores...]
//
//        for i in 0 ..< numBoxes {
//            let centerX = Float(truncating: output[i * boxSize + 0])
//            let centerY = Float(truncating: output[i * boxSize + 1])
//            let width = Float(truncating: output[i * boxSize + 2])
//            let height = Float(truncating: output[i * boxSize + 3])
//                
//            let confidence = Float(truncating: output[i * boxSize + 4])
//                
//            let (classIndex, score) = {
//                var classIndex = 0
//                var highestScore: Float = 0
//                for j in 0 ..< numClasses {
//                    let score = Float(truncating: output[i * boxSize + 5 + j])
//                    if score > highestScore {
//                        highestScore = score
//                        classIndex = j
//                    }
//                }
//                return (classIndex, highestScore * confidence)
//            }()
//                
//            // Convert box from center-width-height to xyxy
//            let left = centerX - width / 2
//            let top = centerY - height / 2
//            let right = centerX + width / 2
//            let bottom = centerY + height / 2
//                
//            let prediction = Prediction(
//                classIndex: classIndex,
//                score: score,
//                xyxy: (left, top, right, bottom),
//                inputImgSize: inputImgSize
//            )
//            predictions.append(prediction)
//        }
//            
//        return predictions
//    }
//    
//    private func IOU(a: XYXY, b: XYXY) -> Float {
//        // Calculate the intersection coordinates
//        let x1 = max(a.x1, b.x1)
//        let y1 = max(a.y1, b.y1)
//        let x2 = min(a.x2, b.x2)
//        let y2 = min(a.y2, b.y2)
//        
//        // Calculate the intersection area
//        let intersection = max(x2 - x1, 0) * max(y2 - y1, 0)
//        
//        // Calculate the union area
//        let area1 = (a.x2 - a.x1) * (a.y2 - a.y1)
//        let area2 = (b.x2 - b.x1) * (b.y2 - b.y1)
//        let union = area1 + area2 - intersection
//        
//        // Calculate the IoU score
//        let iou = intersection / union
//        
//        return iou
//    }
//    
//    func nonMaximumSuppression(
//        predictions: [Prediction],
//        iouThreshold: Float,
//        limit: Int
//    ) -> [Prediction] {
//        guard !predictions.isEmpty else {
//            return []
//        }
//            
//        let sortedIndices = predictions.indices.sorted {
//            predictions[$0].score > predictions[$1].score
//        }
//            
//        var selected: [Prediction] = []
//        var active = [Bool](repeating: true, count: predictions.count)
//        var numActive = active.count
//
//        // The algorithm is simple: Start with the box that has the highest score.
//        // Remove any remaining boxes that overlap it more than the given threshold
//        // amount. If there are any boxes left (i.e. these did not overlap with any
//        // previous boxes), then repeat this procedure, until no more boxes remain
//        // or the limit has been reached.
//        outer: for i in 0 ..< predictions.count {
//            if active[i] {
//                let boxA = predictions[sortedIndices[i]]
//                selected.append(boxA)
//                    
//                if selected.count >= limit { break }
//
//                for j in i + 1 ..< predictions.count {
//                    if active[j] {
//                        let boxB = predictions[sortedIndices[j]]
//                            
//                        if self.IOU(a: boxA.xyxy, b: boxB.xyxy) > iouThreshold {
//                            active[j] = false
//                            numActive -= 1
//                               
//                            if numActive <= 0 { break outer }
//                        }
//                    }
//                }
//            }
//        }
//        return selected
//    }
//}
