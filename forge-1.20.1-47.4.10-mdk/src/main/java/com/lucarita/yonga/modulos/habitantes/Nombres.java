package com.lucarita.yonga.modulos.habitantes;

import net.minecraft.util.RandomSource;

/**
 * Nombres por época y sexo. Nunca se muestran sobre la cabeza: el jugador los descubre hablando (fase 3).
 * Fase 7: cada época con los suyos según se van dibujando; las que aún no tienen usan los medievales.
 */
public final class Nombres {

    private Nombres() {}

    private static final String[] MEDIEVAL_HOMBRE = {
            "Rodrigo", "Sancho", "Fernán", "Gonzalo", "Nuño", "Pelayo", "Bermudo", "Ramiro", "Alfonso", "Martín",
            "Diego", "Íñigo", "Lope", "Gil", "Tello", "Fruela", "Ordoño", "Mendo", "Garci", "Pedro", "Juan", "Domingo"};
    private static final String[] MEDIEVAL_MUJER = {
            "Urraca", "Jimena", "Elvira", "Sancha", "Toda", "Mayor", "Teresa", "Berenguela", "Leonor", "Constanza",
            "Aldonza", "María", "Inés", "Catalina", "Mencía", "Juana", "Oria", "Godina", "Estefanía", "Blanca"};

    /** Prehistoria: nombres cortos inventados, de sonido antiguo (algunos tomados de cuevas y leyendas). */
    private static final String[] PREHISTORICA_HOMBRE = {
            "Urko", "Tor", "Arek", "Kael", "Gorn", "Tarn", "Ruk", "Bako", "Dak", "Enok", "Harn", "Ilak", "Joru",
            "Kanu", "Lorr", "Mako", "Nuk", "Taru", "Zorn", "Aitor", "Ekain", "Lur", "Bror", "Odo"};
    private static final String[] PREHISTORICA_MUJER = {
            "Ayla", "Ena", "Iza", "Nara", "Ura", "Tala", "Kira", "Oba", "Lia", "Sura", "Amaia", "Ilta", "Neva",
            "Rasa", "Yara", "Hala", "Eko", "Tasa", "Mana", "Ilun", "Oria", "Sela", "Zuri", "Anu"};

    /** Antigüedad: nombres romanos (y algún hispano, como Viriato). */
    private static final String[] ANTIGUA_HOMBRE = {
            "Marco", "Lucio", "Cayo", "Tito", "Quinto", "Publio", "Décimo", "Sexto", "Aulo", "Gneo", "Servio",
            "Druso", "Fabio", "Julio", "Claudio", "Valerio", "Cornelio", "Emilio", "Flavio", "Antonio", "Casio",
            "Rufo", "Lucano", "Viriato"};
    private static final String[] ANTIGUA_MUJER = {
            "Julia", "Livia", "Claudia", "Cornelia", "Valeria", "Aurelia", "Octavia", "Flavia", "Emilia", "Tulia",
            "Lucrecia", "Antonia", "Domicia", "Agripina", "Fulvia", "Plotina", "Sabina", "Camila", "Lucila", "Marcia",
            "Popea", "Faustina"};

    /** Edad Moderna: nombres del Siglo de Oro. */
    private static final String[] MODERNA_HOMBRE = {
            "Gaspar", "Baltasar", "Melchor", "Lope", "Pedro", "Francisco", "Diego", "Alonso", "Miguel", "Luis",
            "Hernando", "Gonzalo", "Bernardo", "Cristóbal", "Rodrigo", "Íñigo", "Sebastián", "Tomás", "Antonio",
            "Andrés", "Mateo", "Juan", "Gabriel", "Esteban"};
    private static final String[] MODERNA_MUJER = {
            "Isabel", "Ana", "Mariana", "Juana", "Catalina", "Leonor", "Beatriz", "Inés", "Francisca", "Luisa",
            "Margarita", "Teresa", "Antonia", "Jerónima", "Lucía", "Magdalena", "Agustina", "Feliciana", "Violante",
            "Elvira", "Clara", "Paula"};

    /** Época industrial: nombres de finales del siglo XIX. */
    private static final String[] INDUSTRIAL_HOMBRE = {
            "Ramón", "Joaquín", "Benito", "Eusebio", "Fermín", "Anselmo", "Celestino", "Saturnino", "Leandro",
            "Agapito", "Venancio", "Higinio", "Eduardo", "Emilio", "Federico", "Manuel", "José", "Ricardo", "Arturo",
            "Ignacio", "Julián", "Valentín", "Casimiro", "Nicolás"};
    private static final String[] INDUSTRIAL_MUJER = {
            "Dolores", "Encarnación", "Remedios", "Rosario", "Amparo", "Pilar", "Consuelo", "Mercedes", "Engracia",
            "Petra", "Eulalia", "Rosalía", "Emilia", "Concepción", "Asunción", "Manuela", "Carmen", "Josefa",
            "Rafaela", "Matilde", "Eugenia", "Clotilde"};

    /** Hoy: nombres actuales. */
    private static final String[] CONTEMPORANEA_HOMBRE = {
            "Hugo", "Mateo", "Martín", "Lucas", "Leo", "Daniel", "Pablo", "Álvaro", "Adrián", "Javier", "David",
            "Sergio", "Carlos", "Raúl", "Iván", "Rubén", "Óscar", "Marcos", "Alejandro", "Jorge", "Iker", "Unai",
            "Nacho", "Dani"};
    private static final String[] CONTEMPORANEA_MUJER = {
            "Lucía", "Sofía", "Martina", "Paula", "Julia", "Valeria", "Alba", "Claudia", "Laura", "Marta", "Sara",
            "Andrea", "Irene", "Elena", "Noelia", "Cristina", "Nerea", "Ainhoa", "Vega", "Carla", "Olivia", "Daniela"};

    public static String elegir(Epoca epoca, boolean mujer, RandomSource azar) {
        String[] lista = switch (epoca) {
            case PREHISTORICA -> mujer ? PREHISTORICA_MUJER : PREHISTORICA_HOMBRE;
            case ANTIGUA -> mujer ? ANTIGUA_MUJER : ANTIGUA_HOMBRE;
            case MODERNA -> mujer ? MODERNA_MUJER : MODERNA_HOMBRE;
            case INDUSTRIAL -> mujer ? INDUSTRIAL_MUJER : INDUSTRIAL_HOMBRE;
            case CONTEMPORANEA -> mujer ? CONTEMPORANEA_MUJER : CONTEMPORANEA_HOMBRE;
            default -> mujer ? MEDIEVAL_MUJER : MEDIEVAL_HOMBRE;
        };
        return lista[azar.nextInt(lista.length)];
    }
}
