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
            url: "https://github.com/gary-quinn/kmp-nfc/releases/download/v0.0.4/KmpNfc.xcframework.zip",
            checksum: "48fb4da93e2338b3471e24297fd9151294918604a65bd05db4e0ef1d9f30161f"
        ),
    ]
)
