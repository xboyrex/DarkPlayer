#!/usr/bin/env bash
set -Eeuo pipefail

# Dark Player README publisher for Termux/Linux/macOS Bash.
# It uses a fresh clone by default so failed earlier attempts or dirty old
# deployment folders cannot block this run. Only explicit README-pack paths
# are staged. Ignored screenshot folders are intentionally force-added.

PACK_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)"
DEFAULT_REPO="https://github.com/xboyrex/DarkPlayer.git"
STAMP="$(date +%Y%m%d-%H%M%S)"

fail() { printf '\nERROR: %s\n' "$*" >&2; exit 1; }
info() { printf '\n==> %s\n' "$*"; }

for required in README.md README-PUSH-TERMUX.md START-TERMUX.sh \
  docs/assets/dark-player/dark-player-mark.svg \
  docs/assets/dark-player/dark-player-showcase.gif \
  assets/app-icons/icon-kitchen/android/play_store_512.png; do
  [ -e "$PACK_DIR/$required" ] || fail "Package file missing: $required. Extract the complete ZIP first."
done
[ -x "$PACK_DIR/scripts/push-darkplayer-readme.sh" ] || chmod +x "$PACK_DIR/scripts/push-darkplayer-readme.sh"

command -v git >/dev/null 2>&1 || fail "Git is missing. In Termux run: pkg update && pkg install git unzip"

REPO_URL="${DARKPLAYER_REPO_URL:-$DEFAULT_REPO}"
printf 'GitHub repository URL [%s]: ' "$REPO_URL"
IFS= read -r input_repo || true
REPO_URL="${input_repo:-$REPO_URL}"

case "$REPO_URL" in
  https://github.com/*|ssh://git@github.com/*|git@github.com:*) ;;
  *) printf 'Warning: remote is not a standard GitHub URL: %s\n' "$REPO_URL" ;;
esac

info "Checking repository branch"
REMOTE_DEFAULT_BRANCH="$(git ls-remote --symref "$REPO_URL" HEAD 2>/dev/null | sed -n 's|^ref: refs/heads/\([^[:space:]]*\)[[:space:]]\+HEAD$|\1|p' | head -n 1 || true)"
[ -n "$REMOTE_DEFAULT_BRANCH" ] || REMOTE_DEFAULT_BRANCH="main"
printf 'Branch [%s]: ' "$REMOTE_DEFAULT_BRANCH"
IFS= read -r input_branch || true
BRANCH="${input_branch:-$REMOTE_DEFAULT_BRANCH}"

TARGET_DIR="${DARKPLAYER_WORKDIR:-$HOME/DarkPlayer-readme-publish-$STAMP}"
[ ! -e "$TARGET_DIR" ] || fail "Deployment directory already exists: $TARGET_DIR. Remove it only if it is disposable, or rerun with DARKPLAYER_WORKDIR set to a new path."

info "Cloning $REPO_URL (branch: $BRANCH)"
mkdir -p "$(dirname -- "$TARGET_DIR")"
git clone --single-branch --branch "$BRANCH" "$REPO_URL" "$TARGET_DIR" || fail "Clone failed. Verify the repository URL, branch, network access, and permissions."

BACKUP_DIR="$HOME/DarkPlayer-README-backups/$STAMP"
mkdir -p "$BACKUP_DIR"
if [ -f "$TARGET_DIR/README.md" ]; then
  cp -p "$TARGET_DIR/README.md" "$BACKUP_DIR/README.md"
  info "Current README backed up to $BACKUP_DIR/README.md"
fi
for rel in docs/assets/dark-player assets/app-icons/icon-kitchen scripts/push-darkplayer-readme.sh START-TERMUX.sh README-PUSH-TERMUX.md; do
  if [ -e "$TARGET_DIR/$rel" ]; then
    mkdir -p "$BACKUP_DIR/$(dirname -- "$rel")"
    cp -a "$TARGET_DIR/$rel" "$BACKUP_DIR/$rel"
  fi
done

info "Installing README, screenshots, animated preview, SVG icons, and IconKitchen output"
# These namespaced folders are owned by this pack. Backups above preserve the
# previous versions before replacement, avoiding stale screenshot leftovers.
rm -rf -- "$TARGET_DIR/docs/assets/dark-player" "$TARGET_DIR/assets/app-icons/icon-kitchen"
mkdir -p "$TARGET_DIR/docs/assets" "$TARGET_DIR/assets/app-icons" "$TARGET_DIR/scripts"
cp -a "$PACK_DIR/README.md" "$TARGET_DIR/README.md"
cp -a "$PACK_DIR/docs/assets/dark-player" "$TARGET_DIR/docs/assets/dark-player"
cp -a "$PACK_DIR/assets/app-icons/icon-kitchen" "$TARGET_DIR/assets/app-icons/icon-kitchen"
cp -a "$PACK_DIR/README-PUSH-TERMUX.md" "$TARGET_DIR/README-PUSH-TERMUX.md"
cp -a "$PACK_DIR/START-TERMUX.sh" "$TARGET_DIR/START-TERMUX.sh"
cp -a "$PACK_DIR/scripts/push-darkplayer-readme.sh" "$TARGET_DIR/scripts/push-darkplayer-readme.sh"
chmod +x "$TARGET_DIR/START-TERMUX.sh" "$TARGET_DIR/scripts/push-darkplayer-readme.sh"

cd "$TARGET_DIR"

if ! git config user.name >/dev/null 2>&1; then
  printf 'Git commit author name: '
  IFS= read -r AUTHOR_NAME || true
  [ -n "${AUTHOR_NAME:-}" ] || fail "Git author name required. Set git config user.name and rerun."
  git config user.name "$AUTHOR_NAME"
fi
if ! git config user.email >/dev/null 2>&1; then
  printf 'Git commit author email: '
  IFS= read -r AUTHOR_EMAIL || true
  [ -n "${AUTHOR_EMAIL:-}" ] || fail "Git author email required. Set git config user.email and rerun."
  git config user.email "$AUTHOR_EMAIL"
fi

info "Staging only README pack files (force-add handles ignored docs/assets paths)"
git add -f -- \
  README.md \
  README-PUSH-TERMUX.md \
  START-TERMUX.sh \
  scripts/push-darkplayer-readme.sh \
  docs/assets/dark-player \
  assets/app-icons/icon-kitchen

git diff --cached --check || fail "Whitespace/conflict-marker validation failed. No push attempted."

# Guardrail: never stage unrelated project changes.
while IFS= read -r staged_path; do
  case "$staged_path" in
    README.md|README-PUSH-TERMUX.md|START-TERMUX.sh|scripts/push-darkplayer-readme.sh|docs/assets/dark-player/*|assets/app-icons/icon-kitchen/*) ;;
    *) fail "Unexpected staged path: $staged_path. No push attempted." ;;
  esac
done < <(git diff --cached --name-only)

if git diff --cached --quiet; then
  info "The repository already has the same README pack; nothing new to commit."
  exit 0
fi

printf '\nFiles queued for commit:\n'
git diff --cached --stat
printf '\nRepository: %s\nBranch: %s\nWorking clone: %s\nBackup folder: %s\n' "$REPO_URL" "$BRANCH" "$TARGET_DIR" "$BACKUP_DIR"
printf '\nCommit and push these README/assets changes? [Y/n]: '
IFS= read -r CONFIRM || true
case "${CONFIRM:-Y}" in
  n|N|no|NO) info "Cancelled before commit/push. Staged changes remain in $TARGET_DIR."; exit 0 ;;
esac

git commit -m "docs: refresh Dark Player README and screenshots" || fail "Commit failed. Review the Git error above."
info "Pushing to origin/$BRANCH"
git push origin "$BRANCH" || fail "Push failed. The commit is saved locally at $TARGET_DIR. Check write access and GitHub authentication, then run: git -C '$TARGET_DIR' push origin '$BRANCH'"

printf '\nPUBLISH SUCCESS\nRepository: %s\nBranch: %s\nWorking clone: %s\nBackup folder: %s\n' "${REPO_URL%.git}" "$BRANCH" "$TARGET_DIR" "$BACKUP_DIR"
printf 'Open: %s\n' "${REPO_URL%.git}"
