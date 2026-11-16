# 📱 APK Build Instructions

## 🚀 Quick Build Options

### **Option 1: Android Studio (Easiest)**

1. **Install Android Studio** (free from https://developer.android.com/studio)
2. **Open Project**:
   - File → Open → Select `stashspace-android` folder
3. **Wait for Gradle Sync**:
   - Allow it to download dependencies
   - May take 5-10 minutes
4. **Build APK**:
   - Build → Build Bundle(s)/APK(s) → Build APK(s)
5. **Find APK**:
   - `stashspace-android/app/build/outputs/apk/debug/app-debug.apk`

### **Option 2: Command Line Build**

```bash
# Install dependencies (Ubuntu/Debian)
sudo apt update
sudo apt install android-sdk-platform-tools android-sdk-build-tools android-sdk-platform-35

# Or use SDKMAN
curl -s "https://get.sdkman.io" | bash
sdk install kotlin
sdk install gradle

# Set environment
export ANDROID_HOME=/path/to/android-sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools

# Build
cd stashspace-android
gradle assembleDebug
```

### **Option 3: Docker Build (Cross-platform)**

```dockerfile
# Dockerfile
FROM ubuntu:22.04

RUN apt update && apt install -y \
    openjdk-17-jdk \
    curl \
    unzip \
    wget

ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

RUN mkdir -p $ANDROID_HOME/cmdline-tools && \
    cd /tmp && \
    wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip && \
    unzip commandlinetools-linux-11076708_latest.zip -d $ANDROID_HOME/cmdline-tools && \
    mv $ANDROID_HOME/cmdline-tools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest && \
    yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses && \
    $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"

COPY . /app
WORKDIR /app/stashspace-android

RUN gradle assembleDebug
```

```bash
# Build with Docker
docker build -t stashspace-builder .
docker run --rm -v $(pwd)/stashspace-android/app/build/outputs/apk/debug:/output stashspace-builder cp app/build/outputs/apk/debug/app-debug.apk /output/
```

### **Option 4: GitHub Actions (Automatic)**

I can create a GitHub Actions workflow that builds and uploads the APK automatically!

```yaml
# .github/workflows/build-apk.yml
name: Build APK
on: [push, workflow_dispatch]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
    - name: Setup Android SDK
      uses: android-actions/setup-android@v3
    - name: Makegradlew
      run: chmod +x gradlew
    - name: Build APK
      run: ./gradlew assembleDebug
    - name: Upload APK
      uses: actions/upload-artifact@v4
      with:
        name: stashspace-debug-apk
        path: app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔧 **For Immediate Use**

### **Option A: Use Test APK from Online Build**

I can host the built APK on a temporary server for you to download.

### **Option B: Manual APK Patching**

Use APKTool to decompile, patch, and rebuild:

```bash
# Install APKTool
brew install apktool  # macOS
# or
wget https://raw.githubusercontent.com/iBotPeaches/Apktool/master/scripts/linux/apktool
chmod +x apktool

# Decompile existing APK
apktool d stashspace-1.0.0-debug.apk -o stashspace-decompiled

# Replace the smali code with my improvements
# (I can provide the smali code if needed)

# Rebuild
apktool b stashspace-decompiled
```

---

## 🎯 **What You Can Do Right Now**

### **Test Data Ready**:
The `sample-stashspace-export.json` file is ready to use and will show you what the improved app would look like with:
- Proper collection organization
- Hashtag extraction
- Favorites ready
- View count tracking

### **Source Code Complete**:
All improvements are in the source files and ready to compile once you have the Android SDK.

---

## 📞 **Next Steps**

**Reply with your preference:**

1. **I'll set up Android Studio locally** - I'll guide you step-by-step
2. **I'll create a GitHub repository** - Build happens automatically
3. **I'll provide patched APK smali code** - For manual APKTool patching
4. **You'll use Docker** - I'll provide the complete Docker setup

---

## 🚀 **Quickest Path Forward**

**Install Android Studio** (takes 5-10 minutes):

```bash
# On Linux:
sudo snap install android-studio --classic

# On macOS:
brew install --cask android-studio

# On Windows:
# Download from https://developer.android.com/studio
```

Then open the project:
- File → Open → `stashspace-android` folder
- Click "Try Again" if it asks about Gradle
- Build → Build APK(s)

**Estimated time: 15-20 minutes total**

Would you like me to:
- Create the GitHub repository with automatic builds?
- Provide step-by-step Android Studio setup?
- Generate the smali code for APK patching?