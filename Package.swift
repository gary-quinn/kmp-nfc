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
            url: "https://github.com/gary-quinn/kmp-nfc/releases/download/v0.0.5/KmpNfc.xcframework.zip",
            checksum: "db30b5a54974d71fe1187e5bdc29d8043e44a311d2f3a9f57047d6c7e7b7de3d"
        ),
    ]
)
