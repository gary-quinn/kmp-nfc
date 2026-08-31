#!/bin/sh
# Xcode Cloud post-clone: pre-build KmpNfcSample.framework before xcodebuild archives.
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
GRADLEW="$REPO_DIR/gradlew"
FRAMEWORK_SRC="$REPO_DIR/sample/build/bin/iosArm64/releaseFramework/KmpNfcSample.framework"
FRAMEWORKS_DIR="$REPO_DIR/sample/build/xcode-frameworks"
MARKER="$FRAMEWORKS_DIR/.xcode-cloud-prebuild"

echo "[ci_post_clone] Building KmpNfcSample for Xcode Cloud..."
(
  cd "$REPO_DIR"
  "$GRADLEW" :sample:linkReleaseFrameworkIosArm64 --console=plain --no-daemon
)

SDK_VERSION="$(xcrun --sdk iphoneos --show-sdk-version 2>/dev/null || true)"
if [ -n "$SDK_VERSION" ]; then
  SDK_NAME="iphoneos${SDK_VERSION}"
else
  SDK_NAME="iphoneos"
fi
FRAMEWORK_DEST="$FRAMEWORKS_DIR/Release/$SDK_NAME"

mkdir -p "$FRAMEWORK_DEST"
rm -rf "$FRAMEWORK_DEST/KmpNfcSample.framework"
cp -R "$FRAMEWORK_SRC" "$FRAMEWORK_DEST/"
date -u +%s > "$MARKER"
echo "[ci_post_clone] Framework copied to $FRAMEWORK_DEST"
