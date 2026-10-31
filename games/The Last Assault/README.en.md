# The Last Assault

**Version:** 0.0.1  
**Developer:** Stefan  
**Engine:** Official NVGT 0.89.1 beta

*The Last Assault* is a keyboard-controlled 3D audio game and an off-site game project stored in this repository under `games/`, outside the GitHub Pages website. The game loads its maps from `.map` files in `maps/`.

## Menus and language

The main menu provides Play, Documentation, Settings, and Exit. Documentation opens a native NVGT audio form with a read-only, multiline text field. It lets you choose English or Portuguese documentation and then open the README or changelog. Settings is also a native audio form: use Tab and Shift+Tab to move between the language list, menu-wrapping checkbox, and buttons. The game interface supports English, Portuguese, Spanish, Turkish, and Vietnamese, with English as the default. Translation tables are loaded from `languages/`.

## Controls

- Arrow keys: move.
- Left or right Ctrl: fire the selected weapon.
- Keys 1, 2, and 3: select a weapon; weapon 3 unlocks at level 2.
- R: reload.
- Space: jump.
- Alt+Enter: use a manual travel point at the current position.
- Alt+B: open the in-game map builder without pausing the world.
- Alt+Up/Down: adjust map music volume.
- Home/End: adjust master volume.
- Page Up/Page Down in menus: adjust menu music volume.
- A: announce ammunition; H: announce health; B: announce the current zone; C: announce coordinates; T: announce survival time; L: announce level and experience; I: open inventory.
- Tab and Shift+Tab: navigate the quick inventory; Shift+Enter uses the selected item.
- Escape: return to the main menu.

## Maps and audio

Map files use the header and record format used by the Shooter project. Records include map bounds, platforms, named zones, safe zones, stairs, positional sources, area ambience, and manual travel points. `source:` records play positionally in 3D with spatial attenuation. `ambience:` records loop while the player is inside their defined area and pause outside it.

The map builder can create maps and edit map records while the game world continues to update. Map transitions clear the previous map's audio and data, set the destination position, then activate only audio defined by the destination map. A map with no audio records stays silent.

## Project files

- `maps/`: map data files.
- `sounds/`: audio assets.
- `languages/`: language text catalogs.
- `includes/`: game modules and local copies of the native NVGT audio form and speech modules.

On Windows, settings and player progress are stored in `%AppData%/StefanGames/The Last Assault/settings.ini`.
