#!/bin/bash
set -euo pipefail
TASK_DIR=$(cd "$(dirname "$0")" && pwd)
BUILD_DIR=$(cd "$TASK_DIR/toolchain/android-15" && pwd)
PLATFORM_JAR="$TASK_DIR/toolchain/android-35/android.jar"
mkdir -p "$TASK_DIR/classes" "$TASK_DIR/dex" "$TASK_DIR/gen" "$TASK_DIR/output"
python3 - "$TASK_DIR" <<'CLEAN'
from pathlib import Path
import sys
for name in ['classes','dex','gen']:
 for item in (Path(sys.argv[1])/name).rglob('*'):
  if item.is_file():item.unlink()
CLEAN
"$BUILD_DIR/aapt" package -f -m -J "$TASK_DIR/gen" -M "$TASK_DIR/AndroidManifest.xml" -S "$TASK_DIR/res" -I "$PLATFORM_JAR"
java -jar "$TASK_DIR/toolchain/ecj.jar" -nowarn -8 -bootclasspath "$PLATFORM_JAR:$BUILD_DIR/core-lambda-stubs.jar" -d "$TASK_DIR/classes" "$TASK_DIR"/src/com/dongwon/sleeptimeline/*.java "$TASK_DIR"/gen/com/dongwon/sleeptimeline/R.java
"$BUILD_DIR/d8" --lib "$PLATFORM_JAR" --min-api 26 --output "$TASK_DIR/dex" $(find "$TASK_DIR/classes" -name "*.class")
"$BUILD_DIR/aapt" package -f -M "$TASK_DIR/AndroidManifest.xml" -S "$TASK_DIR/res" -A "$TASK_DIR/assets" -I "$PLATFORM_JAR" -F "$TASK_DIR/unsigned.apk"
python3 - "$TASK_DIR" <<'PY'
import sys,zipfile
from pathlib import Path
p=Path(sys.argv[1])
with zipfile.ZipFile(p/'unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:z.write(p/'dex/classes.dex','classes.dex')
PY
"$BUILD_DIR/zipalign" -f 4 "$TASK_DIR/unsigned.apk" "$TASK_DIR/aligned.apk"
if [ ! -f "$TASK_DIR/signing.jks" ]; then
 /usr/lib/jvm/java-17-openjdk-amd64/bin/keytool -genkeypair -keystore "$TASK_DIR/signing.jks" -storepass sleep-timeline-local -keypass sleep-timeline-local -alias sleeptimeline -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=dayreport'
fi
"$BUILD_DIR/apksigner" sign --ks "$TASK_DIR/signing.jks" --ks-pass pass:sleep-timeline-local --key-pass pass:sleep-timeline-local --out "$TASK_DIR/output/v3.2.0-dayreport.apk" "$TASK_DIR/aligned.apk"
"$BUILD_DIR/apksigner" verify --verbose "$TASK_DIR/output/v3.2.0-dayreport.apk"
"$BUILD_DIR/aapt" dump badging "$TASK_DIR/output/v3.2.0-dayreport.apk"
