//
//  ViewController.swift
//  OCRNativeIOS
//
//  Created by Barna on 03/09/2024.
//

import AVFoundation
import Foundation
import MLImage
import MLKit
import SwiftUI
import UIKit

class ViewController: UIViewController, AVCaptureVideoDataOutputSampleBufferDelegate, ObservableObject {
  var confidenceThreshold: Float = 0.1
  var iouThreshold: Float = 0.5
  var predictions: [Prediction] = []

  var OCRResults: [String] = []
  let alphabets = Array("0123456789.")
  var countOfMeasure = 0
  var meroErtek: String = ""
  var startTime: DispatchTime?

  @Published var state: String = "init"
  @Published var statInit: String = "init"
  @Published var statGyariSzam: String = "init"
  @Published var statMero: String = "init"
  @Published var cardState: String = "init"
  @Published var torchIsOn: Bool = false
//  @Published var gyariSzam: String = "init"
  @Published var showAlert: Bool = false
  let gyariSzam = RNData.shared.gyariSzam as String


  var setOnce: Int = 0
  let ocrSeconds: Int = 5

  private var permissionGranted = false
  @Published var deviceHasTorch: Bool = false

  private let captureSession = AVCaptureSession()
  private let sessionQueue = DispatchQueue(label: "sessionQueue")

  private var previewLayer = AVCaptureVideoPreviewLayer()
  var screenRect: CGRect! = nil // For view dimensions

  private var videoOutput = AVCaptureVideoDataOutput()
  let options: CommonTextRecognizerOptions = TextRecognizerOptions()
  var textRecognizer: TextRecognizer?

  var barcodeOptions = BarcodeScannerOptions(formats: BarcodeFormat.all)
  var barcodeScanner: BarcodeScanner?
  var gyariSzamTalalat: String = ""
  var rect = RectangleData.shared.rect

  let orientationTransform = CGAffineTransform(rotationAngle: CGFloat.pi / -2) // Rotate 90 degrees clockwise
  let context = CIContext()

  func scalePoint(x: CGFloat, y: CGFloat, from oldSize: CGFloat, to newSize: CGFloat) -> CGPoint {
    let scaleFactor = newSize / oldSize
    let newX = x * scaleFactor
    let newY = y * scaleFactor
    return CGPoint(x: newX, y: newY)
  }

  func createScaledCGRect(x1: CGFloat, y1: CGFloat, x2: CGFloat, y2: CGFloat, from oldSize: CGFloat, to newSize: CGFloat) -> CGRect {
    // Scale the points
    let scaledPoint1 = scalePoint(x: x1, y: y1, from: oldSize, to: newSize)
    let scaledPoint2 = scalePoint(x: x2, y: y2, from: oldSize, to: newSize)

    // Create the CGRect using the scaled points
    let origin = CGPoint(x: min(scaledPoint1.x, scaledPoint2.x), y: min(scaledPoint1.y, scaledPoint2.y))
    let size = CGSize(width: abs(scaledPoint2.x - scaledPoint1.x), height: abs(scaledPoint2.y - scaledPoint1.y))

    return CGRect(origin: origin, size: size)
  }

  override func viewDidLoad() {
    checkPermission()

    sessionQueue.async { [unowned self] in
      guard permissionGranted else { return }
      self.setupCaptureSession()
      self.captureSession.startRunning()
    }
    rect = RectangleData.shared.rect
    textRecognizer = TextRecognizer.textRecognizer(options: options)
    barcodeScanner = BarcodeScanner.barcodeScanner(options: barcodeOptions)

  }

  func checkPermission() {
    switch AVCaptureDevice.authorizationStatus(for: .video) {
      // Permission has been granted before
      case .authorized:
        permissionGranted = true

      // Permission has not been requested yet
      case .notDetermined:
        requestPermission()

      default:
        permissionGranted = false
    }
  }

  func requestPermission() {
    sessionQueue.suspend()
    AVCaptureDevice.requestAccess(for: .video) { [unowned self] granted in
      self.permissionGranted = granted
      self.sessionQueue.resume()
    }
  }

  var videoDevice: AVCaptureDevice? // Extracted as a class property

  func setupCaptureSession() {
    if videoDevice == nil {
      self.videoDevice = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back)
    }
    // Check if videoDevice is still nil after the assignment
    guard let videoDevice = videoDevice else {
      print("Failed to access video device.")
      return // Exit the method if videoDevice is nil
    }
    guard let videoDeviceInput = try? AVCaptureDeviceInput(device: videoDevice) else { return }

    guard captureSession.canAddInput(videoDeviceInput) else { return }
    captureSession.addInput(videoDeviceInput)

    screenRect = UIScreen.main.bounds

    previewLayer = AVCaptureVideoPreviewLayer(session: captureSession)
    previewLayer.frame = CGRect(x: 0, y: 0, width: screenRect.size.width, height: screenRect.size.height)
    previewLayer.videoGravity = AVLayerVideoGravity.resizeAspectFill // Fill screen
    videoOutput.videoSettings = [kCVPixelBufferPixelFormatTypeKey as String: Int(kCVPixelFormatType_420YpCbCr8BiPlanarFullRange)]
    videoOutput.setSampleBufferDelegate(self, queue: DispatchQueue(label: "sampleBufferQueue"))
    captureSession.addOutput(videoOutput)
    previewLayer.connection?.videoOrientation = .portrait
    DispatchQueue.main.async { [weak self] in
      self!.view.layer.addSublayer(self!.previewLayer)
      self!.state = "gyari"
      self!.deviceHasTorch = videoDevice.hasTorch
    }
    startTime = DispatchTime.now()
    rect = RectangleData.shared.rect
  }

  func toggleTorch(on: Bool) {
    if deviceHasTorch {
      do {
        try videoDevice!.lockForConfiguration()

        if on == true {
          videoDevice!.torchMode = .on

        } else {
          videoDevice!.torchMode = .off
        }

        videoDevice!.unlockForConfiguration()
      } catch {
        print("Torch could not be used")
      }
    } else {
      print("Torch is not available")
    }
  }
}

struct HostedViewController: UIViewControllerRepresentable {
  let controller: ViewController
  
  func makeUIViewController(context: Context) -> UIViewController {
    return controller
  }

  func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
