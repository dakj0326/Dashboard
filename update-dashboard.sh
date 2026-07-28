#!/usr/bin/env bash

set -u

PROJECT_PATH="${1:?Project path is required}"
APP_PID="${2:?Application PID is required}"
UPDATE_LOG="$PROJECT_PATH/dashboard-update.log"
LAUNCH_LOG="$PROJECT_PATH/dashboard-launch.log"

report_failure() {
    local message="$1"
    printf '%s\n' "$message" >> "$UPDATE_LOG"
    if command -v notify-send >/dev/null 2>&1; then
        notify-send "Dashboard update failed" "$message" || true
    fi
}

while kill -0 "$APP_PID" >/dev/null 2>&1; do
    sleep 0.5
done

if ! cd -- "$PROJECT_PATH"; then
    report_failure "The Dashboard project directory could not be opened."
    exit 1
fi

printf '\n[%s] Starting Dashboard update\n' "$(date --iso-8601=seconds)" >> "$UPDATE_LOG"

if ! git pull --ff-only origin main >> "$UPDATE_LOG" 2>&1; then
    report_failure "Git could not fast-forward the local checkout."
    exit 1
fi

if ! bash ./gradlew clean test >> "$UPDATE_LOG" 2>&1; then
    report_failure "The downloaded version did not compile successfully."
    exit 1
fi

nohup bash ./gradlew run >> "$LAUNCH_LOG" 2>&1 </dev/null &
