#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAVABIN="${JAVA_HOME:+$JAVA_HOME/bin/}"
mkdir -p "$ROOT/build/tests"
"${JAVABIN}javac" -d "$ROOT/build/tests" \
  "$ROOT/app/src/com/griffin/solhome/CincinnatiSky.java" "$ROOT/tests/CincinnatiSkyTest.java" \
  "$ROOT/app/src/com/griffin/solhome/FeedParser.java" "$ROOT/app/src/com/griffin/solhome/SnowForecast.java" "$ROOT/tests/WidgetDataTest.java" \
  "$ROOT/app/src/com/griffin/solhome/WeatherScene.java" "$ROOT/tests/WeatherSceneTest.java" \
  "$ROOT/app/src/com/griffin/solhome/EventPolicy.java" "$ROOT/tests/EventPolicyTest.java" \
  "$ROOT/app/src/com/griffin/solhome/StreamingPolicy.java" "$ROOT/tests/StreamingPolicyTest.java" \
  "$ROOT/app/src/com/griffin/solhome/SpotifyLinks.java" "$ROOT/tests/SpotifyLinksTest.java" \
  "$ROOT/app/src/com/griffin/solhome/GhostMotion.java" "$ROOT/tests/GhostMotionTest.java" \
  "$ROOT/app/src/com/griffin/solhome/WeatherValues.java" "$ROOT/app/src/com/griffin/solhome/WinterCountdown.java" "$ROOT/tests/WeatherValuesTest.java"
"${JAVABIN}java" -cp "$ROOT/build/tests" com.griffin.solhome.CincinnatiSkyTest
"${JAVABIN}java" -cp "$ROOT/build/tests" com.griffin.solhome.WidgetDataTest
"${JAVABIN}java" -cp "$ROOT/build/tests" com.griffin.solhome.WeatherSceneTest
"${JAVABIN}java" -cp "$ROOT/build/tests" com.griffin.solhome.EventPolicyTest
"${JAVABIN}java" -cp "$ROOT/build/tests" com.griffin.solhome.StreamingPolicyTest
"${JAVABIN}java" -cp "$ROOT/build/tests" com.griffin.solhome.SpotifyLinksTest
"${JAVABIN}java" -cp "$ROOT/build/tests" com.griffin.solhome.GhostMotionTest
"${JAVABIN}java" -cp "$ROOT/build/tests" com.griffin.solhome.WeatherValuesTest
