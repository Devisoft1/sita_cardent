#!/bin/bash

# Configuration
SIMULATOR_NAME="iPhone 17"
APP_NAME="SITACardent"
BUNDLE_ID="com.example.sitacardent.SITACardent9VVJS5KJ46"
PROJECT_PATH="iosApp/iosApp.xcodeproj"
SCHEME="iosApp"

# Set JAVA_HOME to Android Studio's bundled JDK
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

echo "🚀 preparing to run $APP_NAME on $SIMULATOR_NAME..."

# 1. Find the Simulator UDID
echo "🔍 Looking for $SIMULATOR_NAME..."
SIMULATOR_ID=$(xcrun simctl list devices available | grep "$SIMULATOR_NAME" | head -n 1 | grep -oE '[0-9A-F]{8}-([0-9A-F]{4}-){3}[0-9A-F]{12}')

if [ -z "$SIMULATOR_ID" ]; then
    echo "❌ Could not find simulator: $SIMULATOR_NAME"
    exit 1
fi

echo "✅ Found Simulator ID: $SIMULATOR_ID"

# 2. Boot the Simulator
echo "📱 Booting simulator..."
xcrun simctl boot "$SIMULATOR_ID" 2>/dev/null || echo "   (Simulator already booted)"
open -a Simulator

# 3. Build the App
echo "🔨 Building app..."
xcodebuild -project "$PROJECT_PATH" \
    -scheme "$SCHEME" \
    -destination "platform=iOS Simulator,id=$SIMULATOR_ID" \
    -configuration Debug \
    clean build \
    | xcpretty && exit ${PIPESTATUS[0]}

if [ $? -ne 0 ]; then
    echo "❌ Build failed. Running without xcpretty to show errors..."
     xcodebuild -project "$PROJECT_PATH" \
        -scheme "$SCHEME" \
        -destination "platform=iOS Simulator,id=$SIMULATOR_ID" \
        -configuration Debug \
        build
    exit 1
fi

echo "✅ Build succeeded!"

# 4. Install the App
echo "📦 Installing app..."
APP_PATH=$(find ~/Library/Developer/Xcode/DerivedData -name "$APP_NAME.app" | grep "$APP_NAME" | head -n 1)

if [ -z "$APP_PATH" ]; then
    echo "❌ Could not find built app bundle."
    exit 1
fi

xcrun simctl install "$SIMULATOR_ID" "$APP_PATH"

# 5. Launch the App
echo "🚀 Launching app..."
xcrun simctl launch "$SIMULATOR_ID" "$BUNDLE_ID"

echo "✨ Done! App should be running on $SIMULATOR_NAME"
