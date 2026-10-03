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
            url: "https://github.com/gary-quinn/kmp-nfc/releases/download/v0.0.6/KmpNfc.xcframework.zip",
            checksum: "790165539fb7b14507b86aada60add44e7a509e9be82b974b14afaa3018a86c7"
        ),
    ]
)
