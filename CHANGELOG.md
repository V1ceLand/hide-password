# Changelog

## 1.3.0
- Renamed to **Hide Password (by KOZYR)** (mod id `hidepassword`; replaces the old `passwordmask` jar — remove it).
- Now available for Minecraft 1.20–1.20.1, 1.20.2–1.20.4, 1.20.5–1.21.1, 1.21.2–1.21.4, 1.21.5, 1.21.6–1.21.8,
  1.21.9–1.21.10, 1.21.11, 26.1.x, 26.2 and 26.3 (Fabric). Server login dialogs exist since 1.21.6 —
  older versions mask chat commands only.
- Help and bug reports: Discord https://discord.gg/6g6VqENtvU (section HIDE PASSWORD); Mod Menu shows a Discord link.

## 1.2.1
- Fixed: the eye button in login dialogs did not react to clicks
  (the dialog's scroll area was taking the click).
- Chat: the button no longer covers buttons of other mods in the bottom-right corner
  (e.g. No Chat Reports) — it moves to the left of them.

## 1.2.0
- The eye button is now a square icon (eye / crossed-out eye) instead of a text button.
- Login dialogs: the button sits right next to the password field and follows it.
- Chat: the button sits in the bottom-right corner above the input line and only
  appears while a password command (`/login`, `/register`, ...) is being typed.
- Removed debug logging of screen widgets.
- Mod icon, contact links, full MIT license text, Gradle wrapper, CI build.

## 1.1.2
- Masking in server dialogs (AuthMe login screen with a «Password» field).
- Password commands are not saved to chat history or drafts, narrator does not read them.
