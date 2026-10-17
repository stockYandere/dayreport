#!/bin/bash
set -euo pipefail
PROJECT_DIR=$(cd "$(dirname "$0")" && pwd)
TOOLS_DIR="$PROJECT_DIR/../toolchain"
mkdir -p "$TOOLS_DIR"
if [ ! -d "$TOOLS_DIR/android-15" ]; then
 curl -fsSL https://dl.google.com/android/repository/build-tools_r35_linux.zip -o "$TOOLS_DIR/build-tools.zip"
 unzip -q "$TOOLS_DIR/build-tools.zip" -d "$TOOLS_DIR"
fi
if [ ! -f "$TOOLS_DIR/android-35/android.jar" ]; then
 curl -fsSL https://dl.google.com/android/repository/platform-35_r02.zip -o "$TOOLS_DIR/platform.zip"
 unzip -q "$TOOLS_DIR/platform.zip" -d "$TOOLS_DIR"
fi
if [ ! -f "$TOOLS_DIR/ecj.jar" ]; then
 curl -fsSL https://repo.maven.apache.org/maven2/org/eclipse/jdt/ecj/3.39.0/ecj-3.39.0.jar -o "$TOOLS_DIR/ecj.jar"
fi
