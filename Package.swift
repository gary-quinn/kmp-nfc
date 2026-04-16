// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "KmpNfc",
    platforms: [.iOS(.v15)],
    products: [
        .library(name: "KmpNfc", targets: ["KmpNfc"]),
    ],
    targets: [
        .binaryTarget(
            name: "KmpNfc",
            url: "https://github.com/gary-quinn/kmp-nfc/releases/download/v0.0.3/KmpNfc.xcframework.zip",
            checksum: "6045f9ed58fad295f5821010b5f1c559328531061b2b8999c6b05a45b7023836"
        ),
    ]
)
