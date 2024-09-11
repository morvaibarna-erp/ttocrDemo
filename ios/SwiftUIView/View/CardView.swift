//
//  CardView.swift
//  OCRNativeIOS
//
//  Created by Barna on 03/09/2024.
//

import SwiftUI

struct CardView: View {
    let gyariSzam: String
    @ObservedObject var OCR: ViewController

    var body: some View {
        ZStack(alignment: .leading) {
            RoundedRectangle(cornerRadius: 25.0)
                .fill(.white)
                .shadow(radius: 10)
            VStack {
                if OCR.state == "init" {
                    CardText(title: "Inicializálás", text: "", status: OCR.statInit)
                }
                else if OCR.state == "gyari" {
                    CardText(title: "Gyári szám: ", text: gyariSzam, status: OCR.statGyariSzam)
                }
                else if OCR.state == "mero" {
                    CardText(title: "Gyári szám: ", text: gyariSzam, status: OCR.statGyariSzam)
                    CardText(title: "Mérő állás leolvasása", text: "", status: OCR.statMero)
                }
            }
            .padding()
        }
        .frame(maxWidth: .infinity, maxHeight: 100, alignment: .topLeading)
        .padding()
    }
}

struct CardText: View {
    let title: String
    let text: String
    let fontSize = 20.0
    var status: String

    var body: some View {
        HStack {
            Text(self.title)
                .foregroundStyle(.black)
            Spacer()
            Text(self.text)
                .foregroundStyle(.black)
            Spacer()

            if self.status == "init" {
                ProgressView()
                    .tint(.black)
            }
            else if self.status == "done" {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundStyle(.green)
            }
            else {
                Image(systemName: "xmark")
                    .foregroundStyle(.red)
            }
        }
        .font(.custom("Montserrat", fixedSize: fontSize))
        .padding()
    }
}

// #Preview {
//    CardView(gyariSzam: "4353452345324543", initial: true)
// }
