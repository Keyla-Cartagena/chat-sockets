package client;

import common.Protocolo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Cliente del chat. Usa DOS hilos:
 *   - main: lee tu teclado y envía al servidor.
 *   - ServerListener: lee del socket y muestra lo que llega.
 * Los dos comparten el mismo Socket (uno lee su InputStream, el otro escribe en su OutputStream).
 *
 * Uso: java -cp out client.ChatClient [host] [puerto]
 */
public class ChatClient {

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : Protocolo.HOST;
        int puerto = args.length > 1 ? Integer.parseInt(args[1]) : Protocolo.PUERTO;

        // UTF-8 en entrada y salida para que tildes y ñ se vean bien
        // (en Windows ejecuta antes "chcp 65001" en la terminal)
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        Scanner teclado = new Scanner(System.in, StandardCharsets.UTF_8);

        try (Socket socket = new Socket(host, puerto)) {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            // 1) Registro: aquí todavía no hay hilo receptor, así que leemos la respuesta directo
            if (!registrar(teclado, in, out)) {
                return;
            }

            // 2) Arranca el hilo que recibe (daemon: no impide que el programa termine)
            ServerListener listener = new ServerListener(in);
            Thread hiloReceptor = new Thread(listener, "server-listener");
            hiloReceptor.setDaemon(true);
            hiloReceptor.start();

            mostrarAyuda();

            // 3) Hilo main: teclado -> servidor
            while (teclado.hasNextLine()) {
                String texto = teclado.nextLine().trim();
                if (texto.isEmpty()) {
                    continue;
                }
                if (!texto.startsWith("/")) {
                    out.println(Protocolo.armar(Protocolo.MSG, texto));
                    continue;
                }

                String[] partes = texto.split("\\s+", 3);   // /msg  destino  texto
                switch (partes[0].toLowerCase()) {
                    case "/salir" -> {
                        listener.detener();                       // salida voluntaria
                        out.println(Protocolo.QUIT);
                        return;                                   // try-with-resources cierra el socket
                    }
                    case "/usuarios" -> out.println(Protocolo.LIST);
                    case "/msg" -> {
                        if (partes.length < 3) {
                            System.out.println("! Uso: /msg usuario texto");
                        } else {
                            out.println(Protocolo.armar(Protocolo.PRIV, partes[1], partes[2]));
                        }
                    }
                    case "/ayuda" -> mostrarAyuda();
                    default -> System.out.println("! Comando desconocido. Escribe /ayuda");
                }
            }
            // Si se acaba la entrada (Ctrl+D / Ctrl+Z), salimos limpio
            listener.detener();
            out.println(Protocolo.QUIT);

        } catch (IOException e) {
            System.out.println("No se pudo conectar a " + host + ":" + puerto + " (" + e.getMessage() + ")");
        }
    }

    /** Pide el nick hasta que el servidor responda OK. */
    private static boolean registrar(Scanner teclado, BufferedReader in, PrintWriter out) throws IOException {
        while (true) {
            System.out.print("Elige tu nickname: ");
            if (!teclado.hasNextLine()) {
                return false;
            }
            out.println(Protocolo.armar(Protocolo.JOIN, teclado.nextLine().trim()));

            String respuesta = in.readLine();   // bloquea hasta que el servidor conteste
            if (respuesta == null) {
                System.out.println("El servidor cerró la conexión.");
                return false;
            }
            if (Protocolo.OK.equals(Protocolo.comando(respuesta))) {
                return true;
            }
            System.out.println("! " + Protocolo.resto(respuesta));   // ERROR|motivo
        }
    }

    private static void mostrarAyuda() {
        System.out.println("--- Comandos ---");
        System.out.println("  texto normal        -> mensaje para todos");
        System.out.println("  /msg usuario texto  -> mensaje privado");
        System.out.println("  /usuarios           -> ver quién está conectado");
        System.out.println("  /ayuda              -> mostrar esta ayuda");
        System.out.println("  /salir              -> salir del chat");
        System.out.println("----------------");
    }
}