package de.deadzonemc.waffen.wurf;

/** Was geworfen wird: Zünder in Ticks (0 = beim Aufprall), Lärmradius, Wurfkraft. */
public enum Wurfart {
    HANDGRANATE("handgranate", 60, 96, 1.0F),
    BRANDSATZ("brandsatz", 0, 28, 0.9F),
    RAUCHGRANATE("rauchgranate", 30, 12, 1.0F),
    BLENDGRANATE("blendgranate", 40, 40, 1.0F),
    LAERMKOEDER("laermkoeder", 20, 0, 0.85F),
    WURFMESSER("wurfmesser", 0, 0, 1.7F),
    SPORENBOMBE("sporenbombe", 0, 10, 1.0F);

    public final String id;
    public final int zuender;
    public final int laerm;
    public final float kraft;

    Wurfart(String id, int zuender, int laerm, float kraft) {
        this.id = id;
        this.zuender = zuender;
        this.laerm = laerm;
        this.kraft = kraft;
    }
}
