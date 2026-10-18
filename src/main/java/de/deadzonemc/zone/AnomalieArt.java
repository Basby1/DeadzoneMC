package de.deadzonemc.zone;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** Die vier Arten von Anomalien. Jede hat ihr eigenes Artefakt. */
public enum AnomalieArt implements StringRepresentable {
    /** Gravitationsstrudel: zieht alles an und schleudert es zerquetscht hoch. */
    STRUDEL,
    /** Glutfeld: unsichtbare Hitze, die bei Berührung in Flammen aufgeht. */
    GLUTFELD,
    /** Entladung: knisternde Luft, schlägt mit Blitzen zu. */
    ENTLADUNG,
    /** Säurenebel: ätzende Schwaden, vergiften und zersetzen Rüstung. */
    SAEURE;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String getSerializedName() {
        return id();
    }

    public ArtefaktItem.Art artefakt() {
        return switch (this) {
            case STRUDEL -> ArtefaktItem.Art.QUALLE;
            case GLUTFELD -> ArtefaktItem.Art.FEUERKUGEL;
            case ENTLADUNG -> ArtefaktItem.Art.BLITZKERN;
            case SAEURE -> ArtefaktItem.Art.STEINBLUT;
        };
    }
}
