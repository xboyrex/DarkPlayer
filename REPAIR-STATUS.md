# DarkPlayer repair status

## Applied source repairs

1. Fixed both `schemeForSeed` call sites to match the real API: `schemeForSeed(context, seedHex, dark)`.
2. Added the Material Icons dependency directly to `feature:settings`, which imports `androidx.compose.material.icons.*`.
3. Fixed the `VolumeState` suspend-call path:
   - `updateVolumeBoostState` is now suspend.
   - Uses the actual suspend extension `getIsLoudnessGainSupported()` present in `CustomCommands.kt` (the old imported function did not exist).
   - Media3 audio-session callbacks dispatch it through a Main coroutine scope.
   - The listener is removed and the scope cancelled when the composition effect is disposed.

## Remaining blockers from the supplied archive

The ZIP does not contain implementations for the following referenced packages/APIs:

- `dev.xboyrex.darkplayer.core.ui.glass.*`
- `dev.xboyrex.darkplayer.core.ui.updater.*`

Examples of missing declarations include `GlassSettings`, `GlassQuality`, `LocalGlassBackdrop`, `Motion`, `liquidGlass`, `glassSpec`, `glassOverlayFill`, `glassFill`, `pressable`, `RollingText`, `rememberReducedMotion`, `DampedDragAnimation`, `InteractiveHighlight`, `navHaptic`, `rememberUpdateState`, and `UpdateOverlayDialog`.

These are not ordinary missing imports. The source implementations are absent from this archive. Replacing them with empty/dummy declarations would remove or degrade Liquid Glass, navigation interactions, and update-check behavior, so they have deliberately not been fabricated.

## Verification

- ZIP archive integrity: passed before and after repair.
- Static verification of the three applied changes: passed.
- Gradle compilation: NOT RUN successfully. The wrapper requires Gradle 9.7.1, but the build environment could not resolve `services.gradle.org` (DNS/network unavailable).

This archive is a partially repaired source package, not a verified build. A previous working DarkPlayer source ZIP or the missing `core/ui/glass` and `core/ui/updater` source directories are required to complete the restoration without changing product behavior.
