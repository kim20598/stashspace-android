#!/bin/bash
# Build script for Stashspace APK
# Run this after setting up Android SDK

set -e

echo "🏗️  Building Stashspace APK..."

# Set SDK path - use aminimal setup or detect
export ANDROID_HOME=${ANDROID_HOME:-/data/data/com.termux/files/usr/opt/android-sdk}
export ANDROID_SDK_ROOT=${ANDROID_SDK_ROOT:-$ANDROID_HOME}

# Create minimal SDK structure if needed
mkdir -p "$ANDROID_HOME/platforms/android-35"
mkdir -p "$ANDROID_HOME/build-tools/35.0.0"
mkdir -p "$ANDROID_HOME/platform-tools"

# Copy android.jar if it exists
if [ -f "$HOME/android.jar" ]; then
    cp "$HOME/android.jar" "$ANDROID_HOME/platforms/android-35/"
    echo "✅ Copied android.jar"
fi

# Create local.properties
cat > stashspace-android/local.properties << EOF
# Android SDK location
sdk.dir=$ANDROID_HOME
EOF

echo "🚀 Starting build..."
cd stashspace-android

# Build with memory optimization
ORG_GRADLE_PROJECT_org.gradle.jvmargs=-Xmx1g
gradle assembleDebug -x test \
    --no-daemon \
    --stacktrace \
    --info \
    2>&1 | tail -100

echo "✅ Build complete!"
echo "📍 APK location:"
find . -name "*.apk" -type f | head -5