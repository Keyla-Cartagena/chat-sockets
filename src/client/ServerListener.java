package client;

import common.Protocolo;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Hilo del cliente que RECIBE: lee del socket y muestra en pantalla.
 *
 * Es necesario porque readLine() bloquea: si el hilo main esperara al servidor, no podría
 * leer tu teclado, y si esperara al teclado, no vería los mensajes de los demás.
 */
public class ServerListener implements Runnable {

    private final BufferedReader in;
    private volatile boolean activo = true;   // volatile: lo cambia el hilo main, lo lee este

    public ServerListener(BufferedReader in) {
        this.in = in;
    }

    /** Se llama antes de salir voluntariamente, para no mostrar un falso error de conexión. */
    public void detener() {
        activo = false;
    }

    @Override
    public void run() {
        try {
            String linea;
            while ((linea = in.readLine()) != null) {
                mostrar(linea);
            }
            if (activo) {
                System.out.println("[El servidor cerró la conexión]");
                System.exit(0);   // el hilo main está bloqueado leyendo el teclado
            }
        } catch (IOException e) {
            if (activo) {
                System.out.println("[Se perdió la conexión con el servidor]");
                System.exit(1);
            }
            // si no estaba activo, es el cierre normal del socket al hacer /salir
        }
    }

    /** Traduce una línea del protocolo a algo legible. */
    private void mostrar(String linea) {
        String resto = Protocolo.resto(linea);
        switch (Protocolo.comando(linea)) {
            case Protocolo.MSG -> {
                String[] p = Protocolo.dividir(resto, 2);   // origen | texto
                System.out.println(p[0] + ": " + (p.length > 1 ? p[1] : ""));
            }
            case Protocolo.PRIV -> {
                String[] p = Protocolo.dividir(resto, 2);
                System.out.println("[privado de " + p[0] + "] " + (p.length > 1 ? p[1] : ""));
            }
            case Protocolo.USERS -> {
                String[] nicks = resto.isEmpty() ? new String[0] : resto.split(",");
                System.out.println("Usuarios conectados (" + nicks.length + "): " + String.join(", ", nicks));
            }
            case Protocolo.INFO -> System.out.println("* " + resto);
            case Protocolo.ERROR -> System.out.println("! " + resto);
            default -> { /* OK u otros: no se muestran */ }
        }
    }
}