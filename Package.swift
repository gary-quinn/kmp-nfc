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
            url: "https://github.com/gary-quinn/kmp-nfc/releases/download/v0.0.2/KmpNfc.xcframework.zip",
            checksum: "cad64c3a5f5148817986ca6e76b20e1c0091f5106eb8c130b4795ae8214a0792"
        ),
    ]
)
