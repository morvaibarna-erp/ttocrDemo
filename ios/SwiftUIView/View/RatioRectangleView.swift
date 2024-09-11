import SwiftUI

struct RatioRectangleView: View {
    @State private var widthRatio: CGFloat = 1.0
    @State private var heightRatio: CGFloat = 1.0
    
    private let cornerLength: CGFloat = 40.0
    private let borderRadius: CGFloat = 50.0
    private let strokeWidth: CGFloat = 10.0
    
    @ObservedObject var rectangleData = RectangleData.shared

    var body: some View {
        GeometryReader { geometry in
            let viewWidth = geometry.size.width
            let viewHeight = geometry.size.height
            
            // Calculate the rectangle size based on the aspect ratio and screen size
            let rectWidth = calculateRectangleWidth(viewWidth: viewWidth, diagonalInInches: diagonalInInches())
            let rectHeight = rectWidth * (heightRatio / widthRatio)
            let left = (viewWidth - rectWidth) / 2
            let top = (viewHeight - rectHeight) / 2
            let rect = CGRect(x: left, y: top, width: rectWidth, height: rectHeight)

            ZStack {
                // Draw the transparent overlay
                Color.black.opacity(0.5)
                    .mask(
                        Rectangle()
                            .fill(Color.black)
                            .frame(width: viewWidth, height: viewHeight)
                            .overlay(
                                RoundedRectangle(cornerRadius: borderRadius)
                                    .frame(width: rectWidth, height: rectHeight)
                                    .blendMode(.destinationOut)
                                    .position(x: rect.midX, y: rect.midY)
                            )
                    )
                
                // Draw the custom corners
                drawCorners(rect: rect)
            }
            .compositingGroup() // Needed for the destinationOut blend mode
            .onAppear {
                rectangleData.rect = CGRect(x: rect.midX, y: rect.midY, width: rectWidth, height: rectHeight)
            }
        }
        .edgesIgnoringSafeArea(.all)
//        .frame(width: .infinity, height: .infinity)
    }
    
    // Calculate the rectangle width based on the screen size and diagonal inches
    private func calculateRectangleWidth(viewWidth: CGFloat, diagonalInInches: CGFloat) -> CGFloat {
        return diagonalInInches >= 7 ? viewWidth * 0.5 : viewWidth * 0.8
    }
    
    // Calculate the diagonal size in inches (simplified)
    private func diagonalInInches() -> CGFloat {
        let screenSize = UIScreen.main.bounds.size
        let diagonal = sqrt(screenSize.width * screenSize.width + screenSize.height * screenSize.height)
        let scale = UIScreen.main.scale
        return diagonal / scale / 160.0
    }
    
    // Draw the corner decorations
    @ViewBuilder
    private func drawCorners(rect: CGRect) -> some View {
        // Top-left corner
        drawCornerArc(center: CGPoint(x: rect.minX + borderRadius, y: rect.minY + borderRadius), startAngle: .degrees(180), endAngle: .degrees(270))
        drawCornerLine(from: CGPoint(x: rect.minX + borderRadius, y: rect.minY), to: CGPoint(x: rect.minX + borderRadius + cornerLength, y: rect.minY))
        drawCornerLine(from: CGPoint(x: rect.minX, y: rect.minY + borderRadius), to: CGPoint(x: rect.minX, y: rect.minY + borderRadius + cornerLength))
        
        // Top-right corner
        drawCornerArc(center: CGPoint(x: rect.maxX - borderRadius, y: rect.minY + borderRadius), startAngle: .degrees(270), endAngle: .degrees(0))
        drawCornerLine(from: CGPoint(x: rect.maxX - borderRadius, y: rect.minY), to: CGPoint(x: rect.maxX - borderRadius - cornerLength, y: rect.minY))
        drawCornerLine(from: CGPoint(x: rect.maxX, y: rect.minY + borderRadius), to: CGPoint(x: rect.maxX, y: rect.minY + borderRadius + cornerLength))
        
        // Bottom-left corner
        drawCornerArc(center: CGPoint(x: rect.minX + borderRadius, y: rect.maxY - borderRadius), startAngle: .degrees(90), endAngle: .degrees(180))
        drawCornerLine(from: CGPoint(x: rect.minX + borderRadius, y: rect.maxY), to: CGPoint(x: rect.minX + borderRadius + cornerLength, y: rect.maxY))
        drawCornerLine(from: CGPoint(x: rect.minX, y: rect.maxY - borderRadius), to: CGPoint(x: rect.minX, y: rect.maxY - borderRadius - cornerLength))
        
        // Bottom-right corner
        drawCornerArc(center: CGPoint(x: rect.maxX - borderRadius, y: rect.maxY - borderRadius), startAngle: .degrees(0), endAngle: .degrees(90))
        drawCornerLine(from: CGPoint(x: rect.maxX - borderRadius, y: rect.maxY), to: CGPoint(x: rect.maxX - borderRadius - cornerLength, y: rect.maxY))
        drawCornerLine(from: CGPoint(x: rect.maxX, y: rect.maxY - borderRadius), to: CGPoint(x: rect.maxX, y: rect.maxY - borderRadius - cornerLength))
    }
    
    // Helper function to draw corner arcs
    @ViewBuilder
    private func drawCornerArc(center: CGPoint, startAngle: Angle, endAngle: Angle) -> some View {
        Path { path in
            path.addArc(center: center, radius: borderRadius, startAngle: startAngle, endAngle: endAngle, clockwise: false)
        }
        .stroke(Color.white, lineWidth: strokeWidth)
    }
    
    // Helper function to draw corner lines
    @ViewBuilder
    private func drawCornerLine(from startPoint: CGPoint, to endPoint: CGPoint) -> some View {
        Path { path in
            path.move(to: startPoint)
            path.addLine(to: endPoint)
        }
        .stroke(Color.white, style: StrokeStyle(lineWidth: strokeWidth, lineCap: .round))
    }
}

// #Preview {
//    RatioRectangleView()
//        .background(.gray)
// }
