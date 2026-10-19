#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# Android's SDK jar has stub methods; use an Android-compatible org.json host runtime for these fixtures.
: "${JSON_JAR:?Set JSON_JAR to an Android-compatible org.json runtime jar (android-json)}"
JAVABIN="${JAVA_HOME:+$JAVA_HOME/bin/}"
mkdir -p "$ROOT/build/catalog-tests"
"${JAVABIN}javac" -cp "$JSON_JAR" -d "$ROOT/build/catalog-tests" \
  "$ROOT/app/src/com/griffin/solhome/StreamingCatalog.java" \
  "$ROOT/app/src/com/griffin/solhome/StreamingPolicy.java" \
  "$ROOT/tests/StreamingCatalogTest.java" "$ROOT/tests/StreamingPolicyTest.java"
"${JAVABIN}java" -cp "$ROOT/build/catalog-tests:$JSON_JAR" com.griffin.solhome.StreamingPolicyTest
"${JAVABIN}java" -cp "$ROOT/build/catalog-tests:$JSON_JAR" com.griffin.solhome.StreamingCatalogTest

if [[ -n "${ANDROID_JAR:-}" ]]; then
  "${JAVABIN}javac" -cp "$ANDROID_JAR" -d "$ROOT/build/catalog-tests" \
    "$ROOT/app/src/com/griffin/solhome/StreamingArtwork.java" "$ROOT/tests/StreamingArtworkTest.java"
  "${JAVABIN}java" -cp "$ROOT/build/catalog-tests:$ANDROID_JAR" com.griffin.solhome.StreamingArtworkTest
fi
