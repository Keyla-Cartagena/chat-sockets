package common;

/**
 * Protocolo de texto del chat. Cada mensaje es UNA línea: COMANDO|campo1|campo2...
 * Lo usan el servidor y el cliente, así los dos hablan el mismo "idioma".
 */
public final class Protocolo {

    private Protocolo() { }

    public static final String HOST = "localhost";
    public static final int PUERTO = 5000;
    public static final String SEP = "|";

    // Cliente -> Servidor
    public static final String JOIN = "JOIN";   // JOIN|nick
    public static final String MSG = "MSG";     // MSG|texto
    public static final String PRIV = "PRIV";   // PRIV|destino|texto
    public static final String LIST = "LIST";   // LIST
    public static final String QUIT = "QUIT";   // QUIT

    // Servidor -> Cliente
    public static final String OK = "OK";           // OK
    public static final String ERROR = "ERROR";     // ERROR|motivo
    public static final String USERS = "USERS";     // USERS|ana,luis,juan
    public static final String INFO = "INFO";       // INFO|texto
    // MSG|origen|texto  y  PRIV|origen|texto  (mismo nombre, el servidor agrega el origen)

    /** Une los campos con el separador: armar("MSG", "ana", "hola") -> "MSG|ana|hola" */
    public static String armar(String... campos) {
        return String.join(SEP, campos);
    }

    /** Devuelve el comando (lo que está antes del primer '|'). */
    public static String comando(String linea) {
        int i = linea.indexOf('|');
        return i < 0 ? linea : linea.substring(0, i);
    }

    /** Devuelve todo lo que está después del primer '|' (o "" si no hay). */
    public static String resto(String linea) {
        int i = linea.indexOf('|');
        return i < 0 ? "" : linea.substring(i + 1);
    }

    /** Divide "a|b|c" en máximo 'max' partes; el texto del mensaje puede contener '|'. */
    public static String[] dividir(String texto, int max) {
        return texto.split("\\|", max);
    }

    /** Nick válido: 1 a 15 letras, números o guion bajo (sin espacios ni '|'). */
    public static boolean nickValido(String nick) {
        return nick != null && nick.matches("[\\p{L}\\p{N}_]{1,15}");
    }
}