#!/bin/bash -xeu

mkdir -p ~/junit/ /tmp/artifacts

find . -type f -regex ".*/target/test-reports/.*xml" -print -exec cp {} ~/junit/ \;
find . -type d -regex ".*/target/scala-.*/scoverage-report" -print -exec cp -R {} /tmp/artifacts/ \;

