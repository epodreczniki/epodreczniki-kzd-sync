#!/bin/bash
set -e
set -u
cd "$( dirname "${BASH_SOURCE[0]}" )"
java -jar kzd-sync.jar $@

