#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
TOOLS="${ANDROID_BUILD_TOOLS:-${SDK:+$SDK/build-tools/34.0.0}}"
ANDROID="${ANDROID_JAR:-${SDK:+$SDK/platforms/android-34/android.jar}}"
if [[ -z "$TOOLS" || -z "$ANDROID" ]]; then
  echo 'Set ANDROID_SDK_ROOT, or both ANDROID_BUILD_TOOLS and ANDROID_JAR.' >&2
  exit 1
fi
JAVABIN="${JAVA_HOME:+$JAVA_HOME/bin/}"
BUILD="$ROOT/build/app"
mkdir -p "$BUILD/classes" "$BUILD/dex" "$BUILD/gen"
find "$BUILD/classes" -name '*.class' -delete
"$TOOLS/aapt2" compile --dir "$ROOT/app/res" -o "$BUILD/resources.zip"
"$TOOLS/aapt2" link -o "$BUILD/unsigned.apk" -I "$ANDROID" \
  --manifest "$ROOT/app/AndroidManifest.xml" -R "$BUILD/resources.zip" \
  -A "$ROOT/app/assets" --java "$BUILD/gen"
"${JAVABIN}javac" -source 8 -target 8 -classpath "$ANDROID" -d "$BUILD/classes" \
  "$ROOT"/app/src/com/griffin/solhome/*.java
"${JAVABIN}jar" cf "$BUILD/classes.jar" -C "$BUILD/classes" .
"${JAVABIN}java" -cp "$TOOLS/lib/d8.jar" com.android.tools.r8.D8 \
  --lib "$ANDROID" --min-api 26 --output "$BUILD/dex" "$BUILD/classes.jar"
"${JAVABIN}jar" uf "$BUILD/unsigned.apk" -C "$BUILD/dex" classes.dex
"$TOOLS/zipalign" -f 4 "$BUILD/unsigned.apk" "$BUILD/aligned.apk"
# This is a disposable development identity, never the private TV installation key.
KEYSTORE="$ROOT/.local/signing/debug.p12"
mkdir -p "$(dirname "$KEYSTORE")"
if [[ ! -f "$KEYSTORE" ]]; then
  "${JAVABIN}keytool" -genkeypair -noprompt -keystore "$KEYSTORE" -storetype PKCS12 \
    -storepass android -keypass android -alias androiddebugkey \
    -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Vista Development'
fi
"${JAVABIN}java" -jar "$TOOLS/lib/apksigner.jar" sign --ks "$KEYSTORE" \
  --ks-pass pass:android --out "$ROOT/build/sol-home-debug.apk" "$BUILD/aligned.apk"
"${JAVABIN}java" -jar "$TOOLS/lib/apksigner.jar" verify "$ROOT/build/sol-home-debug.apk"
printf 'Built %s\n' "$ROOT/build/sol-home-debug.apk"
