//
//  Detector.swift
//  OCRNativeIOS
//
//  Created by Barna on 03/09/2024.
//

import AVFoundation
import MLImage
import MLKit
import UIKit
import Vision

extension ViewController {
  func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
    if !gyariSzamTalalat.contains(gyariSzam), ((DispatchTime.now().uptimeNanoseconds - startTime!.uptimeNanoseconds) / 1000000000) < ocrSeconds {
      getGyariSzam(sampleBuffer: sampleBuffer, gyariSzam: gyariSzam)
    }
    else if !gyariSzamTalalat.contains(gyariSzam), ((DispatchTime.now().uptimeNanoseconds - startTime!.uptimeNanoseconds) / 1000000000) < ocrSeconds * 2 {
      getBarCode(sampleBuffer: sampleBuffer, gyariSzam: gyariSzam)
    }
    else if gyariSzamTalalat.contains(gyariSzam) {
      if setOnce == 0 {
        DispatchQueue.main.async {
          self.statGyariSzam = "done"
          self.state = "mero"
        }
        setOnce = 1
      }
      if OCRResults.count <= 15, statGyariSzam == "done" {
        //            print("Találat: ", gyariSzamTalalat)

        let imageToProcess = convertImage(sampleBuffer: sampleBuffer, rect: rect)

        runDisplayDetector(imageToProcess: imageToProcess)
        if predictions.count > 0 {
          let detectedRect = createScaledCGRect(
            x1: CGFloat(predictions[0].xyxy.x1),
            y1: CGFloat(predictions[0].xyxy.y1),
            x2: CGFloat(predictions[0].xyxy.x2),
            y2: CGFloat(predictions[0].xyxy.y2),
            from: 640,
            to: imageToProcess.size.width)
          let detectedImageCheck = cropImage(image: imageToProcess, rect: detectedRect)
          if predictions[0].classIndex == 1 {
            recognizeText(from: detectedImageCheck)
          }
          else if predictions[0].classIndex == 0 {
            OCRForBlackRed(image: detectedImageCheck)
          }
          predictions.removeAll()
        }
        //      print("Iteration: ", OCRResults.count)
      }
      else if mostFrequentStringWithPercentage(strings: OCRResults) != nil, meroErtek == "" {
        meroErtek = mostFrequentStringWithPercentage(strings: OCRResults)!.0
        DispatchQueue.main.async {
          self.statMero = "done"
        }
        //      print(meroErtek)
        //      print(mostFrequentStringWithPercentage(strings: OCRResults)!.1, " %")
        let imageToSave = convertImage(sampleBuffer: sampleBuffer, rect: rect)
        let data = imageToSave.jpegData(compressionQuality: 0.8)
        let filename = getDocumentsDirectory().appendingPathComponent("mero.png")
        try? data!.write(to: filename)

        RNData.shared.onSuccess(["meroErtek": meroErtek, "filePath": filename.absoluteString])
      }
      else if statMero == "error" {
        print("Hiba történt a leolvasás során!")
      }
    }
    else {
//            print("Hiba történt a gyári szám leolvasása során")
      if setOnce == 0 {
        DispatchQueue.main.async {
          self.statGyariSzam = "error"
          if !self.showAlert {
            self.showAlert = true
          }
        }
        setOnce = 1
      }
    }
  }

  func getDocumentsDirectory() -> URL {
    let paths = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)
    return paths[0]
  }

  func convertImage(sampleBuffer: CMSampleBuffer, rect: CGRect) -> UIImage {
    var cropRect = rect
//      Meg kell fordítani, mert a szenzor fektetett
    let viewWidth = CGFloat(sampleBuffer.cgImage?.height ?? 1080)
    let viewHeight = CGFloat(sampleBuffer.cgImage?.width ?? 1920)

    let rectWidth = calculateRectangleWidth(viewWidth: viewWidth, diagonalInInches: diagonalInInches())
    let rectHeight = rectWidth
    let left = (Int(viewWidth) - Int(rectWidth)) / 2
    let top = (Int(viewHeight) - Int(rectHeight)) / 2

    let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer)

    let ciImage = CIImage(cvPixelBuffer: pixelBuffer!)

    let orientedImage = ciImage.transformed(by: orientationTransform)
    let cgImage = context.createCGImage(orientedImage, from: orientedImage.extent)
    var uiImage = UIImage(cgImage: cgImage!)
    cropRect = CGRect(x: left, y: top, width: Int(rectWidth), height: Int(rectHeight))
    uiImage = cropImage(image: uiImage, rect: cropRect)

    return uiImage
  }

  func cropImage(image: UIImage, rect: CGRect) -> UIImage {
    let cgImage = image.cgImage
    let croppedCGImage = cgImage?.cropping(to: rect)
    if croppedCGImage != nil {
      return UIImage(cgImage: croppedCGImage!, scale: image.scale, orientation: image.imageOrientation)
    }
    else
    { return image }
  }

  func OCRForBlackRed(image: UIImage) {
    let visionImage = VisionImage(image: image)
    textRecognizer!.process(visionImage) { result, error in
      if let error = error {
        print("Error recognizing text: \(error)")
        return
      }
      guard let result = result else { return }
      var exported_text = ""
      for i in 0 ..< result.text.count {
        if result.text[i] == "O" || result.text[i] == "o" {
          exported_text.append("0")
        }
        else if result.text[i] == "I" || result.text[i] == "i" {
          exported_text.append("1")
        }
        else if self.alphabets.contains(result.text[i]) {
          exported_text.append(result.text[i])
        }
      }

      self.OCRResults.append(exported_text)
    }
  }

  func getGyariSzam(sampleBuffer: CMSampleBuffer, gyariSzam: String) {
    let uiImage = convertImage(sampleBuffer: sampleBuffer, rect: rect)
    let visionImage = VisionImage(image: uiImage)
    textRecognizer!.process(visionImage) { result, error in
      if let error = error {
        print("Error recognizing text: \(error)")
        return
      }
      guard let result = result else { return }
      self.gyariSzamTalalat = result.text
    }
  }

  func getBarCode(sampleBuffer: CMSampleBuffer, gyariSzam: String) {
    let uiImage = convertImage(sampleBuffer: sampleBuffer, rect: rect)
    let visionImage = VisionImage(image: uiImage)
    barcodeScanner!.process(visionImage) { barcodes, error in
      guard error == nil, let barcodes = barcodes, !barcodes.isEmpty else {
        return
      }
      for barcode in barcodes {
        let displayValue = barcode.displayValue
        self.gyariSzamTalalat = String(displayValue!)
      }
    }
  }

  private func calculateRectangleWidth(viewWidth: CGFloat, diagonalInInches: CGFloat) -> CGFloat {
    return diagonalInInches >= 7 ? viewWidth * 0.5 : viewWidth * 0.7
  }

  private func diagonalInInches() -> CGFloat {
    let screenSize = UIScreen.main.bounds.size
    let diagonal = sqrt(screenSize.width * screenSize.width + screenSize.height * screenSize.height)
    let scale = UIScreen.main.scale
    return diagonal / scale / 160.0
  }

  func mostFrequentStringWithPercentage(strings: [String]) -> (String, Double)? {
    guard !strings.isEmpty else { return nil }

    // Step 1: Count occurrences of each string
    let stringCounts = Dictionary(strings.map { ($0, 1) }, uniquingKeysWith: +)

    // Step 2: Find the string with the maximum count
    guard let mostFrequentString = stringCounts.max(by: { $0.value < $1.value })?.key else {
      return nil
    }

    // Step 3: Calculate the percentage
    let totalStrings = strings.count
    let mostFrequentCount = stringCounts[mostFrequentString] ?? 0
    let percentage = (Double(mostFrequentCount) / Double(totalStrings)) * 100

    return (mostFrequentString, percentage)
  }
}

extension String {
  subscript(_ index: Int) -> String {
    get {
      String(self[self.index(startIndex, offsetBy: index)])
    }

    set {
      remove(at: self.index(startIndex, offsetBy: index))
      insert(Character(newValue), at: self.index(startIndex, offsetBy: index))
    }
  }
}
