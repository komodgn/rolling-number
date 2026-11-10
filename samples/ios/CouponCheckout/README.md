# CouponCheckout (iOS sample)

쿠폰 적용 결제 화면 데모입니다. 저장소 루트의 `RollingNumber` Swift 패키지를 로컬 경로로 참조합니다.

Xcode 프로젝트 파일은 저장소에 넣지 않고 [XcodeGen](https://github.com/yonaskolb/XcodeGen)으로 만듭니다. macOS에서:

```bash
brew install xcodegen
cd samples/ios/CouponCheckout
xcodegen generate
open CouponCheckout.xcodeproj
```

최소 배포 대상은 iOS 15입니다.
