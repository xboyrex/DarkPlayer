#!/data/data/com.termux/files/usr/bin/bash
set -e

echo "====================================================="
echo "  Dark Player v1.2.0 — Termux GitHub Release Script"
echo "  Developer: ㅤᏞᴀᴢʏㅤᎷꫝʏᴀɴк まやんく (@xboyrex)"
echo "====================================================="

pkg update -y && pkg install -y git unzip gh || true

ZIP_PATH=""
for dir in "/sdcard/Download" "$HOME/storage/downloads" "."; do
  if [ -d "$dir" ]; then
    found=$(ls -t "$dir"/DarkPlayer*.zip 2>/dev/null | head -n 1 || true)
    if [ -n "$found" ] && [ -f "$found" ]; then
      ZIP_PATH="$found"
      break
    fi
  fi
done

if [ -z "$ZIP_PATH" ]; then
  echo "[ERROR] DarkPlayer.zip not found in /sdcard/Download or current folder."
  exit 1
fi

WORK_DIR="$HOME/DarkPlayer_Build"
rm -rf "$WORK_DIR"
mkdir -p "$WORK_DIR"

echo "[1/3] Extracting newest archive: $ZIP_PATH ..."
unzip -q "$ZIP_PATH" -d "$WORK_DIR"
cd "$WORK_DIR"
chmod +x gradlew

echo "[2/3] Initializing Git repository & committing v1.2.0 release ..."
git init
git config user.name "xboyrex"
git config user.email "xboyrex@users.noreply.github.com"
git branch -M main
git add -A
git commit -m "feat(v1.2.0): 1000% Dolby DSP Volume Booster, Liquid Glass/Solid UI switcher, compact Hub & auto-release CI"

REPO_URL="https://github.com/xboyrex/DarkPlayer.git"
echo "[3/3] Pushing to $REPO_URL ..."
git remote remove origin 2>/dev/null || true
git remote add origin "$REPO_URL"
git push -u origin main --force

echo "====================================================="
echo "  Successfully pushed Dark Player v1.2.0 to GitHub!"
echo "  GitHub Actions will now build & auto-publish APKs to:"
echo "  https://github.com/xboyrex/DarkPlayer/releases/latest"
echo "====================================================="
