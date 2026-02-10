#!/bin/sh
set -e
SCRIPT_ABS_PATH=$(readlink -f "$0")
SCRIPT_ABS_DIR=$(dirname "$SCRIPT_ABS_PATH")

exec java $JAVA_OPTS -Xmx4G -cp "$SCRIPT_ABS_DIR/lib/*" io.joern.adasrc2cpg.Main "$@"
