#!/data/data/com.termux/files/usr/bin/bash
# ==============================================================================
# Dark Player — Automated Termux GitHub Push & Auto-Build Trigger Script
# Crafted for: ㅤᏞᴀᴢʏㅤᎷꫝʏᴀɴк まやんく (@xboyrex)
# Repository : https://github.com/xboyrex/DarkPlayer
# ==============================================================================

set -e

GREEN="\033[1;32m"
YELLOW="\033[1;33m"
CYAN="\033[1;36m"
RED="\033[1;31m"
RESET="\033[0m"

echo -e "${CYAN}========================================================${RESET}"
echo -e "${YELLOW}   Dark Player — Termux Auto-Push & CI Release Script   ${RESET}"
echo -e "${CYAN}   Developer: ㅤᏞᴀᴢʏㅤᎷꫝʏᴀɴк まやんく (@xboyrex)        ${RESET}"
echo -e "${CYAN}========================================================${RESET}"

# 1. Ensure required packages are installed in Termux
for pkg in git unzip; do
    if ! command -v "$pkg" >/dev/null 2>&1; then
        echo -e "${YELLOW}[*] Installing $pkg in Termux...${RESET}"
        pkg update -y && pkg install -y "$pkg"
    fi
done

# 2. Locate DarkPlayer-main.zip
ZIP_PATH="${1:-}"
if [ -z "$ZIP_PATH" ]; then
    for candidate in \
        "./DarkPlayer-main.zip" \
        "$HOME/storage/downloads/DarkPlayer-main.zip" \
        "/sdcard/Download/DarkPlayer-main.zip" \
        "/storage/emulated/0/Download/DarkPlayer-main.zip"; do
        if [ -f "$candidate" ]; then
            ZIP_PATH="$candidate"
            break
        fi
    done
fi

if [ -z "$ZIP_PATH" ] || [ ! -f "$ZIP_PATH" ]; then
    echo -e "${RED}[!] DarkPlayer-main.zip not found automatically.${RESET}"
    read -r -p "Enter full path to DarkPlayer-main.zip: " ZIP_PATH
    if [ ! -f "$ZIP_PATH" ]; then
        echo -e "${RED}[!] Error: File not found at '$ZIP_PATH'${RESET}"
        exit 1
    fi
fi

echo -e "${GREEN}[+] Found archive:${RESET} $ZIP_PATH"

# 3. Ask for GitHub Personal Access Token (or use GITHUB_TOKEN env var if set)
REPO_OWNER="xboyrex"
REPO_NAME="DarkPlayer"
BRANCH="main"

if [ -z "${GITHUB_TOKEN:-}" ]; then
    echo -e "${YELLOW}[?] Enter your GitHub Personal Access Token (PAT) for @xboyrex${RESET}"
    echo -e "    (Leave blank if your Termux git credentials/SSH are already saved):"
    read -r -s GITHUB_TOKEN
    echo ""
fi

if [ -n "${GITHUB_TOKEN:-}" ]; then
    REMOTE_URL="https://${REPO_OWNER}:${GITHUB_TOKEN}@github.com/${REPO_OWNER}/${REPO_NAME}.git"
else
    REMOTE_URL="https://github.com/${REPO_OWNER}/${REPO_NAME}.git"
fi

# 4. Extract DarkPlayer-main.zip into clean staging directory
WORK_DIR="$HOME/darkplayer_push_workspace"
rm -rf "$WORK_DIR"
mkdir -p "$WORK_DIR"

echo -e "${CYAN}[*] Extracting $ZIP_PATH...${RESET}"
unzip -q "$ZIP_PATH" -d "$WORK_DIR"

if [ -d "$WORK_DIR/DarkPlayer-main" ]; then
    SRC_DIR="$WORK_DIR/DarkPlayer-main"
else
    SRC_DIR="$WORK_DIR"
fi

cd "$SRC_DIR"

# 5. Ensure gradlew has executable permission in Git index
chmod +x gradlew

# 6. Initialize Git, commit all updated files, and push to main
echo -e "${CYAN}[*] Initializing Git repository and staging all updates...${RESET}"
rm -rf .git
git init -b "$BRANCH"
git config user.name "xboyrex"
git config user.email "xboyrex496@gmail.com"

# Mark gradlew executable explicitly for GitHub Actions runner
git add .
git update-index --chmod=+x gradlew

COMMIT_MSG="feat: Dark Player v2.0 Liquid Glass Nav & Player, 200% Volume Boost, Studio Settings & OTA Auto-Updater"
git commit -m "$COMMIT_MSG"

git remote add origin "$REMOTE_URL"

echo -e "${CYAN}[*] Pushing to https://github.com/${REPO_OWNER}/${REPO_NAME} ($BRANCH)...${RESET}"
git push --force origin "$BRANCH"

echo -e "${GREEN}========================================================${RESET}"
echo -e "${GREEN}[SUCCESS] Pushed to GitHub (${REPO_OWNER}/${REPO_NAME})!${RESET}"
echo -e "${YELLOW}[*] GitHub Actions CI is now automatically building:${RESET}"
echo -e "    -> https://github.com/${REPO_OWNER}/${REPO_NAME}/actions"
echo -e "${YELLOW}[*] Signed Release APKs & OTA Update will publish to:${RESET}"
echo -e "    -> https://github.com/${REPO_OWNER}/${REPO_NAME}/releases/latest"
echo -e "${GREEN}========================================================${RESET}"
