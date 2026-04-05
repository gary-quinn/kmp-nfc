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
            url: "https://github.com/gary-quinn/kmp-nfc/releases/download/v0.1.0/KmpNfc.xcframework.zip",
            checksum: "0000000000000000000000000000000000000000000000000000000000000000"
        ),
    ]
)
