#!/usr/bin/env bash

set -u

PROJECT_PATH="${1:?Project path is required}"
APP_PID="${2:?Application PID is required}"
RESTART_LOG="$PROJECT_PATH/dashboard-restart.log"
LAUNCH_LOG="$PROJECT_PATH/dashboard-launch.log"

while kill -0 "$APP_PID" >/dev/null 2>&1; do
    sleep 0.5
done

if ! cd -- "$PROJECT_PATH"; then
    printf '%s\n' "The Dashboard project directory could not be opened." \
        >> "$RESTART_LOG"
    exit 1
fi

nohup bash ./gradlew run >> "$LAUNCH_LOG" 2>&1 </dev/null &
