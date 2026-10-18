package de.deadzonemc.waffen.feuer;

/** Die wenigen, klaren Kaliber der Deadzone – jede gefundene Munition passt zu einer Waffe, die man finden kann. */
public enum Kaliber {
    NEUN_MM("9mm"),
    MAG357("357"),
    SCHROT("schrot"),
    F556("556"),
    F762("762"),
    ROHR("rohr"),
    BOLZEN("bolzen");

    public final String id;

    Kaliber(String id) {
        this.id = id;
    }

    public String itemId() {
        return "munition_" + id;
    }
}
