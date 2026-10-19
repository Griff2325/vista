#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
TOOLS="${ANDROID_BUILD_TOOLS:-${SDK:+$SDK/build-tools/34.0.0}}"
ANDROID="${ANDROID_JAR:-${SDK:+$SDK/platforms/android-34/android.jar}}"
if [[ -z "$TOOLS" || -z "$ANDROID" ]]; then echo 'Set Android SDK paths as for build-app.sh.' >&2; exit 1; fi
JAVABIN="${JAVA_HOME:+$JAVA_HOME/bin/}"
BUILD="$ROOT/build/picture"
mkdir -p "$BUILD/classes"
"${JAVABIN}javac" -source 8 -target 8 -classpath "$ANDROID" -d "$BUILD/classes" \
  "$ROOT/app/src/com/griffin/solhome/PicturePolicy.java" "$ROOT/bridge/picture/PictureBridge.java"
"${JAVABIN}jar" cf "$BUILD/classes.jar" -C "$BUILD/classes" .
"${JAVABIN}java" -cp "$TOOLS/lib/d8.jar" com.android.tools.r8.D8 \
  --lib "$ANDROID" --min-api 26 --output "$BUILD/vista-picture.jar" "$BUILD/classes.jar"
printf 'Built %s\n' "$BUILD/vista-picture.jar"
