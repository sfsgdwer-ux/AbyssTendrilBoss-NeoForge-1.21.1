# Abyss Tendril Boss V6 (NeoForge 1.21.1)

Mod version **1.1.0**. Requires **GeckoLib 4.9.3**, Java 21, NeoForge 21.1.265.

**V6 3D model:** ~25 blocks tall, 26 animated tentacles and 6 eyeless mouths scattered around the body.
Six special attacks including ruin roar, which attacks nearby living creatures and can destroy up to 70 nearby blocks per roar.
Targets survival players, other hostile monsters, villagers and animals; ignores creative/spectator players and other Abyss Titans.
Block breaking respects mobGriefing and NeoForge griefing protection hooks.

The **V6 geometry, animation, and texture** are stored as compressed Git blobs in [assets-v6/](assets-v6/).
GitHub Actions decompresses these into src/main/resources before compiling.
This ensures the downloaded JAR contains the current V6 model rather than the previous V2 model.

Give spawn egg:
```mcfunction
/give @s abysstendril:abyss_tendril_spawn_egg
```
Summon:
```mcfunction
/summon abysstendril:abyss_tendril_beast
```
Disable block damage:
```mcfunction
/gamerule mobGriefing false
```

See Actions > newest successful Build > Artifacts for download.

WARNING: Giant boss may damage terrain and buildings. Test in a backup world.
