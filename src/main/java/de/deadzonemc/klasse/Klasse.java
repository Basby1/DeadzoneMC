package de.deadzonemc.klasse;

/** Die fünf Klassen von Deadzone MC. */
public enum Klasse {
    BRECHER, SCHUETZE, SANITAETER, TECHNIKER, SPAEHER;

    public String id() {
        return name().toLowerCase();
    }

    public static Klasse ausId(String id) {
        for (Klasse k : values()) {
            if (k.id().equals(id)) {
                return k;
            }
        }
        return null;
    }

    /** Abklingzeit der aktiven Fähigkeit in Ticks. */
    public int abklingzeit() {
        return switch (this) {
            case BRECHER -> 600;
            case SCHUETZE -> 400;
            case SANITAETER -> 1200;
            case TECHNIKER -> 1800;
            case SPAEHER -> 1200;
        };
    }
}
