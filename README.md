# Oxydian Theme

🌐 **English** · [Italiano](README.it.md)

**Overlay themes** (no Xposed) for OxygenOS 16 — the companion of [Oxydian](https://github.com/tugaia56/Oxydian-xposed).

Born from a suggestion by Luigi (@LC9889): an app without Xposed, where **all the overlays work**. If you need an app that is not in the list, ask in the group and it can be added.

## What it does

- **System colours** — 48 accents and 22 backgrounds, or any colour you like; **pure black**, adjustable saturation and lightness
- **Themes for 88 apps** (Google and third-party) and **Dark Shadow** for Settings, SystemUI, Launcher and OnePlus/OPPO apps
- **Icons** — Wi-Fi (18 styles), mobile signal (42 styles) and navigation bar (9 styles), with size and colour
- **Settings icon packs**
- **Circular progress bar** and **PIN keypad** colours
- **Hide network activity** arrows (in/out)
- **Colour automation** — change the system colours from Tasker, MacroDroid or Automate
- Substratum-style selection: tick the items and press **Enable / Disable / Remove**
- **Backup and restore** of your choices, built-in **update check**

## Requirements

- OxygenOS 16 (tested on OnePlus 12), Android 12+ (minSdk 31)
- **Root** (KernelSU or Magisk)

The app creates the `oxydian_theme` module: the first launch needs one reboot, then changes are immediate. The Wi-Fi and signal icon colours, and the colours read by other parts of Oxydian, use Oxydian (hooks).

## Colour automation (Tasker / MacroDroid / Automate)

Turn it on in **Mods → Colour automation**, apply the system colours at least once from the app, then send a broadcast:

- Action: `it.tugaia56.oxydian.theme.action.APPLY_CONFIG`
- Package: `it.tugaia56.oxydian.theme`
- Extras (send only the ones to change): `accent` and `background` (a preset name, `#RRGGBB` or `default`), `randomColor`, `pitchBlack`, `accentSaturation`, `backgroundSaturation` (0–200), `backgroundLightness` (−10 to 10)

The in-app **Guide** button has the step-by-step instructions.

## Build

```
./gradlew assembleDebug
```

A release needs `keystore.properties` (not versioned) and a tag `vX.Y.Z`: the GitHub workflow builds, publishes the release and (if configured) sends the APK to Telegram.

## Credits

- **Luigi (@LC9889)** — the overlay compiler (aapt2, alignment, signing) and the root/file utilities come from his Oxygen Customizer
- **Dark Shadow Theme** — the overlays and the accent, background, PIN keypad, progress bar and icon presets come from this work
- **Substratum** — the overlay theme format everything started from
- **Claude** (Anthropic) — AI assistant used throughout development
