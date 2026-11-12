# Publish the Dark Player README from Termux

This package updates the root README, screenshot gallery, animated preview, SVG feature icons, and IconKitchen app-icon exports. It does not modify Android app source code or build an APK.

## Recommended: one command

1. Extract the ZIP into a new folder. The fixed ZIP has files directly at the archive root, so it will not create an accidental nested `DarkPlayer-README-Pack/DarkPlayer-README-Pack` directory.
2. In Termux, enter the extracted folder and run:

   ```bash
   bash START-TERMUX.sh
   ```

3. Press Enter to accept the default repository and branch, review the staged summary, then press Enter again to confirm the commit and push.

The launcher checks its location and can also detect the extra nested folder layout created by the previous ZIP.

## Optional setup

```bash
pkg update
pkg install git unzip
termux-setup-storage
```

If Git asks for identity and it is not configured globally, enter the name and email you want recorded on the commit. These are commit metadata, not authentication credentials.

## Repository and branch overrides

The defaults target `https://github.com/xboyrex/DarkPlayer.git` and automatically detect the default branch. To use another repository, enter its URL at the prompt. You can also set the remote before launching:

```bash
export DARKPLAYER_REPO_URL='git@github.com:YOUR-OWNER/YOUR-REPO.git'
bash START-TERMUX.sh
```

The remote must grant your account write access. For HTTPS pushes, use Git's supported token/credential flow; for SSH, configure an SSH key first. Never paste a token into this script or commit one to the repository.

## Safety and recovery

- The script clones into a fresh timestamped working directory by default. It does not reset or clean an existing project clone.
- It backs up the previous README and any previous versions of the package-owned asset paths under `~/DarkPlayer-README-backups/` before replacing them.
- It force-stages only the intended README-pack paths. This specifically handles repositories where a `.gitignore` rule ignores `docs` or other asset folders.
- It validates the staged diff and rejects paths outside the README pack before asking for confirmation.
- If GitHub rejects the push, the committed working copy and backup path are printed. Fix authentication/write access and push the saved commit with the exact `git -C ... push origin ...` command printed by the script.
