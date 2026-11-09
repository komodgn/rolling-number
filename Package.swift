// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "RollingNumber",
    platforms: [
        .iOS(.v15),
    ],
    products: [
        .library(name: "RollingNumber", targets: ["RollingNumber"]),
    ],
    targets: [
        .target(
            name: "RollingNumber",
            path: "Sources/RollingNumber"
        ),
        .testTarget(
            name: "RollingNumberTests",
            dependencies: ["RollingNumber"],
            path: "Tests/RollingNumberTests"
        ),
    ]
)
