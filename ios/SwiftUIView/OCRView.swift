import SwiftUI

struct OCRView: View {
  let downSapce: CGFloat = 100
  @StateObject private var rectangleData = RectangleData()
  @StateObject var OCR = ViewController()

  var body: some View {
    ZStack {
      HostedViewController(controller: OCR)
        .ignoresSafeArea()
        .alert(isPresented: $OCR.showAlert) {
          Alert(title: Text("Nem található gyári szám!"), message: Text("Kérjük ellenőrizze, hogy a mérőóra teljesen benne van-e a keretben és a gyári szám, valamint a vonalkód jól olvasható!"), dismissButton: .cancel(Text("Újra")) {
            RNData.shared.onCancelPress(["data": "some data"])
            OCR.torchIsOn = false
            OCR.showAlert = false
          })
        }
      RatioRectangleView()
        .edgesIgnoringSafeArea(.all)
      VStack {
        CardView(gyariSzam: OCR.gyariSzam as String, OCR: OCR)
          .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        HStack {
          ZStack {}
            .frame(width: downSapce, height: downSapce)
          if OCR.deviceHasTorch { FlashLightButton(size: 80, OCR: OCR)
            .frame(width: downSapce, height: downSapce)
          }
          else { NoFlashLight(size: 80)
            .frame(width: downSapce, height: downSapce)
          }
          CloseCameraButton(size: 60, onTap: {
            OCR.torchIsOn = false
            RNData.shared.onCancelPress(["data": "some data"])
          })
          .frame(width: downSapce)
        }
      }
    }
//        .background(Image("mero").resizable().scaledToFill().edgesIgnoringSafeArea(.top).edgesIgnoringSafeArea(.bottom))
  }
}

// #Preview {
//    OCRView()
// }
