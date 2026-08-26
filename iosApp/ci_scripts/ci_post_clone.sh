#!/bin/sh
set -euo pipefail

cd "$CI_PRIMARY_REPOSITORY_PATH"
./gradlew :sample:linkReleaseFrameworkIosArm64
