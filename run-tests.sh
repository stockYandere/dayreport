#!/bin/bash
# JVM 검사는 android.jar 스텁 위에서 순수 로직만 실행한다. UI 검사는 Playwright(Chromium) 필요.
set -euo pipefail
DIR=$(cd "$(dirname "$0")" && pwd)
JAR="$DIR/toolchain/android-35/android.jar"
OUT="$DIR/classes-test"
rm -rf "$OUT";mkdir -p "$OUT"
"$DIR/toolchain/android-15/aapt" package -f -m -J "$DIR/gen" -M "$DIR/AndroidManifest.xml" -S "$DIR/res" -I "$JAR"
java -jar "$DIR/toolchain/ecj.jar" -nowarn -8 -cp "$JAR" -d "$OUT" "$DIR"/src/com/dongwon/sleeptimeline/*.java "$DIR"/gen/com/dongwon/sleeptimeline/R.java "$DIR"/tests/*.java
for t in SleepLogicTest UnusedGapsTest RestoredUsageTest HourSegmentsTest UseBlocksTest; do java -cp "$OUT:$JAR" com.dongwon.sleeptimeline.$t; done
export TZ=Asia/Seoul NODE_PATH=${NODE_PATH:-/opt/node-tools/node_modules}
mkdir -p "$DIR/output"
for t in logic-test records-update-test ui-test day-test; do node "$DIR/tests/$t.cjs"; done
