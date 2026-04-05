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
            url: "https://github.com/gary-quinn/kmp-nfc/releases/download/v0.0.1/KmpNfc.xcframework.zip",
            checksum: "c0bc3cbe21207fb023e48719b28bdaf1d74fb09c27566b726d23afcf5134fd2f"
        ),
    ]
)
