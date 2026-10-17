# Arbeitsplan Deadzone MC (Stand 17.10.2026, 22:05)

## Offen (Reihenfolge)
1. Ergebnis des Testlaufs prüfen: Weltgenerierung jetzt Lost Cities + Biomes O' Plenty (Commit „Weltgenerierung: nur noch …“),
   eigene Stadtgebäude (werkzeuge/stadtgebaeude.py), Tod-Ausrüstungsschaden. Fehler beheben.
2. Walking-Dead-Gefühl: Stadt-Spawner (auch tagsüber Gruppen von Wanderern in Lost-Cities-Städten, Obergrenze ~25 im Umkreis 64).
   Stadterkennung über die Lost-Cities-API (mcjty.lostcities.api, Chunk isCity) oder Heuristik.
3. Skills/Leveln ohne Magie: Pufferfish's Skills (datengetriebene Skillbäume: Kampf, Schusswaffen, Überleben, Handwerk,
   Bewegung) + ParCool! (Parkour; Aktionen per Skill freischalten über ParCool-Limitations). Verfügbarkeit für NeoForge 1.21.1
   mit werkzeuge/modcheck.py prüfen.
4. Bau-Mods: Handcrafted, Macaw's (Möbel, Dächer, Fenster, Lampen, Wege, Brücken), Another Furniture, Decorative Blocks,
   Chipped/Rechiseled, Every Compat, evtl. Create; Tiere (Naturalist ist drin, weitere prüfen). Nur gut bewertet, NeoForge 1.21.1.
5. 10 neue Monster entwerfen (werkzeuge/monster/entwuerfe.py), Bilder an Lina schicken, nach OK einbauen.
6. Zufluchtsorte: befestigte Siedlungen mit vielen NPCs, Händler (kaufen/verkaufen), Questgeber, sicherer Bereich.
7. Quest-NPCs und Quest-System.

## Hinweise
- Lina testet mit der CurseForge-App; Pack-Link: Release „modpack“.
- YUNG-Mods entfernt (CurseForge lieferte Forge-Dateien).
