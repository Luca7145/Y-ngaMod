package com.lucarita.yonga.modulos.habitantes;

/**
 * Las seis épocas de los habitantes. Cada una tendrá su ropa, peinados, oficios y aldeas.
 * Fase 7: cada una con sus 20 ropas, peinados, barbas, complementos y rasgos de cara por sexo (ver Catalogo);
 * las que aún no están dibujadas se ven como la medieval.
 */
public enum Epoca {
    PREHISTORICA("prehistorica"),
    ANTIGUA("antigua"),
    MEDIEVAL("medieval"),
    MODERNA("moderna"),
    INDUSTRIAL("industrial"),
    CONTEMPORANEA("contemporanea");

    private final String id;

    Epoca(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** ¿Tiene ya sus texturas? (ver Catalogo, que genera el dibujante) */
    public boolean dibujada() {
        return Catalogo.DIBUJADA[this.ordinal()];
    }

    public static Epoca de(int indice) {
        Epoca[] todas = values();
        return indice >= 0 && indice < todas.length ? todas[indice] : MEDIEVAL;
    }
}
