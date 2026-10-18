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
  "$ROOT/app/src/com/griffin/solhome/WeatherValues.java" "$ROOT/app/src/com/griffin/solhome/WinterCountdown.java" "$ROOT/tests/WeatherValuesTest.java" \
  "$ROOT/app/src/com/griffin/solhome/WaterMotion.java" "$ROOT/tests/WaterMotionTest.java" \
  "$ROOT/app/src/com/griffin/solhome/SceneCalendar.java" "$ROOT/app/src/com/griffin/solhome/SeasonPalette.java" "$ROOT/tests/SceneCalendarTest.java" \
  "$ROOT/app/src/com/griffin/solhome/WorldMotion.java" "$ROOT/tests/WorldMotionTest.java" \
  "$ROOT/app/src/com/griffin/solhome/CompanionServer.java" "$ROOT/tests/CompanionServerTest.java" \
  "$ROOT/app/src/com/griffin/solhome/HomeNavigation.java" "$ROOT/tests/HomeNavigationTest.java" \
  "$ROOT/app/src/com/griffin/solhome/CodingUsage.java" "$ROOT/app/src/com/griffin/solhome/UsageImportPolicy.java" "$ROOT/tests/CodingUsageTest.java"
"${JAVABIN}java" -cp "$ROOT/build/tests" com.griffin.solhome.CincinnatiSkyTest
"${JAVABIN}java" -cp "$ROOT/build/tests" com.griffin.solhome.WidgetDataTest
"${JAVABIN}java" -cp "$ROOT/build/tests" com.griffin.solhome.WeatherSceneTest
"${JAVABIN}java" -cp "$ROOT/build/tests" com.griffin.solhome.EventPolicyTest
"${JAVABIN}java" -cp "$ROOT/build/tests" com.griffin.solhome.StreamingPolicyTest
"${JAVABIN}java" -cp "$ROOT/build/tests" com.griffin.solhome.SpotifyLinksTest
"${JAVABIN}java" -cp "$ROOT/build/tests" com.griffin.solhome.GhostMotionTest
"${JAVABIN}java" -cp "$ROOT/build/tests" com.griffin.solhome.WeatherValuesTest
"${JAVABIN}java" -cp "$ROOT/build/tests" com.griffin.solhome.WaterMotionTest
"${JAVABIN}java" -cp "$ROOT/build/tests" com.griffin.solhome.SceneCalendarTest

"${JAVABIN}java" -cp "$ROOT/build/tests" com.griffin.solhome.WorldMotionTest
"${JAVABIN}java" -cp "$ROOT/build/tests" com.griffin.solhome.CompanionServerTest

"${JAVABIN}java" -cp "$ROOT/build/tests" com.griffin.solhome.CodingUsageTest
python3 "$ROOT/tests/test_coding_usage.py"

"${JAVABIN}java" -cp "$ROOT/build/tests" com.griffin.solhome.HomeNavigationTest
