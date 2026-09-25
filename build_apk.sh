#!/bin/bash
set -e

ANDROID_JAR="${ANDROID_SDK_ROOT}/platforms/android-34/android.jar"
BUILD_TOOLS="${ANDROID_SDK_ROOT}/build-tools/34.0.0"
APP_DIR="/app/android_app/app/src/main"

mkdir -p /app/build/gen /app/build/obj /app/build/apk

echo "1. Compiling resources with aapt2..."
$BUILD_TOOLS/aapt2 compile --dir "$APP_DIR/res" -o /app/build/compiled_res.zip

echo "2. Linking resources and manifest..."
$BUILD_TOOLS/aapt2 link -I "$ANDROID_JAR" \
    /app/build/compiled_res.zip \
    --manifest "$APP_DIR/AndroidManifest.xml" \
    --java /app/build/gen \
    -o /app/build/apk/unaligned.apk

echo "3. Compiling Java sources..."
javac -source 17 -target 17 \
    -cp "$ANDROID_JAR" \
    -d /app/build/obj \
    /app/build/gen/com/sour/delivery/R.java \
    "$APP_DIR/java/com/sour/delivery/MainActivity.java"

echo "4. Converting bytecode to DEX..."
$BUILD_TOOLS/d8 --release --min-api 21 --output /app/build/apk/ /app/build/obj/com/sour/delivery/*.class

echo "5. Adding DEX to APK..."
cd /app/build/apk
$BUILD_TOOLS/aapt add unaligned.apk classes.dex

echo "6. Aligning APK..."
$BUILD_TOOLS/zipalign -f -p 4 unaligned.apk aligned.apk

echo "7. Generating keystore and signing APK..."
keytool -genkey -v -keystore /app/build/debug.keystore -alias androiddebugkey -keypass android -storepass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=SourDelivery,O=Sour,C=DZ"

$BUILD_TOOLS/apksigner sign --ks /app/build/debug.keystore --ks-pass pass:android --ks-key-alias androiddebugkey --key-pass pass:android --out /app/SGdelivery.apk aligned.apk
cp /app/SGdelivery.apk /app/sour-delivery.apk

echo "=== SGdelivery APK BUILT AND SIGNED SUCCESSFULLY ==="
$BUILD_TOOLS/apksigner verify -v /app/SGdelivery.apk
ls -lh /app/SGdelivery.apk
