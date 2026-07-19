<img width="128" height="128" src="https://i.imgur.com/jKAoNAt.png" alt="icon_square">

# Android-Audio-Channel-QS-Tile

Android app which adds a Quick Settings Tile to control the “Audio Channel” option (Mono/Stereo) in Accessibility Settings -> Audio Adjustment.

This is a fork of [VarunS2002/Android-Audio-Channel-QS-Tile](https://github.com/VarunS2002/Android-Audio-Channel-QS-Tile) that puts Mono on a timer, working just like LineageOS's Caffeine tile: Mono automatically switches back to Stereo when the timer runs out.

<img src="screenshots/tile_stereo.png" alt="Quick Settings tile in Stereo (off) state" width="244">
<br>
<img src="screenshots/tile_mono.png" alt="Quick Settings tile while Mono is on, counting down from 04:58" width="244">

## Features

- Tapping the tile enables Mono and starts a countdown, shown live on the tile
- Tapping again within 5 seconds cycles the duration: **1 min → 5 min → 10 min → ∞ → off**
- Tapping after 5 seconds simply toggles Mono off
- **Long-pressing** the tile jumps straight to **∞** (no timer)
- When the timer expires, Mono reliably reverts to Stereo — even if the app's process was killed in the background
- After a reboot, Mono reverts to Stereo (timers don't survive reboots)
- Tile is highlighted while Mono is on and dimmed when in Stereo

| Gesture | Effect |
|---|---|
| Tap (from off) | Mono, 1:00 countdown |
| Tap again within 5s | Cycle: 5:00 → 10:00 → ∞ → off |
| Tap after 5s | Toggle off |
| Long-press | Straight to ∞ |

## Downloads

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

Grab the APK from the [Releases](https://github.com/ethanm6/Android-Audio-Channel-QS-Tile/releases/) page, or add this repo to Obtainium to get updates automatically:

[<img src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png" alt="Get it on Obtainium" height="80">](https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://add/https://github.com/ethanm6/Android-Audio-Channel-QS-Tile)

> [!IMPORTANT]
> On **Android 14+** the normal package installer refuses apps targeting SDK < 23, so tapping the APK won't work. Install via adb (`adb install --bypass-low-target-sdk-block <apk>`), or use **Obtainium with the Shizuku or root install method**, which applies the bypass automatically. Android 7–13 installs normally.

## Setup

1. Install the APK
2. Add the tile: pull down Quick Settings → edit tiles → drag in **Audio Channel**
3. Tap the tile once — you'll be asked to grant the *Modify system settings* permission

## Requirements

- Android 7.0+ (Nougat/SDK 24)

## Notes

- Regarding **Play Protect** / *"built for an older version of Android"* warnings:
  - These appear because this app targets Android 5.1 (Lollipop/SDK 22).
  - This is intentional: apps targeting Android 6 (Marshmallow/SDK 23) and above are not allowed to modify secure system settings such as "Audio Channel".
  - The app is safe to install and use, and no data is collected.

- Long-pressing the tile closes the notification shade — this is enforced by Android (long-press launches an activity) and cannot be avoided.

- This app may not work on all devices due to ROM specific issues.

- If you face any issue or have a suggestion then feel free to open an issue.

## Support

If you find this fork useful, you can support me on Ko-fi:

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/ethanm6)
