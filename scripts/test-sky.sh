#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAVABIN="${JAVA_HOME:+$JAVA_HOME/bin/}"
mkdir -p "$ROOT/build/tests"
"${JAVABIN}javac" -d "$ROOT/build/tests" \
  "$ROOT/app/src/com/griffin/solhome/CincinnatiSky.java" "$ROOT/tests/CincinnatiSkyTest.java"
"${JAVABIN}java" -cp "$ROOT/build/tests" com.griffin.solhome.CincinnatiSkyTest
