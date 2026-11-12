#!/usr/bin/env bash
set -Eeuo pipefail

# Entry point intended for users who just extracted the ZIP.
HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"

# Older ZIPs may have been extracted with an extra DarkPlayer-README-Pack/ layer.
if [ ! -f "$HERE/README.md" ] && [ -d "$HERE/DarkPlayer-README-Pack" ]; then
  HERE="$HERE/DarkPlayer-README-Pack"
fi

if [ ! -f "$HERE/README.md" ] || [ ! -f "$HERE/scripts/push-darkplayer-readme.sh" ]; then
  printf 'ERROR: README pack files are not together in this folder.\n' >&2
  printf 'Expected README.md and scripts/push-darkplayer-readme.sh beside this launcher.\n' >&2
  printf 'Extract the complete DarkPlayer README ZIP into a new folder and retry.\n' >&2
  exit 1
fi

if ! command -v git >/dev/null 2>&1; then
  if command -v pkg >/dev/null 2>&1; then
    printf 'Git is required. Installing it with Termux package manager...\n'
    pkg install git -y
  else
    printf 'ERROR: Install Git, then rerun this script.\n' >&2
    exit 1
  fi
fi

exec bash "$HERE/scripts/push-darkplayer-readme.sh"
