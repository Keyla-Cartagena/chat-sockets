package server;

import common.Protocolo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Atiende a UN cliente. Se ejecuta en su propio hilo (del ExecutorService).
 *
 * SOCKET: este objeto es dueño del Socket de su cliente. Lee con readLine() (bloquea
 * hasta que el cliente envía algo) y escribe con send().
 *
 * HILOS: run() corre en el hilo de este cliente, pero send() puede ser llamado desde
 * el hilo de OTRO cliente (cuando hace broadcast). Por eso send() es synchronized:
 * así dos hilos nunca mezclan sus líneas en el mismo socket.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final ClientRegistry registry;
    private final BufferedReader in;
    private final PrintWriter out;
    private volatile String nick;   // null hasta que el cliente se registra

    public ClientHandler(Socket socket, ClientRegistry registry) throws IOException {
        this.socket = socket;
        this.registry = registry;
        this.in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    @Override
    public void run() {
        try {
            if (!autenticar()) {
                return;
            }
            registry.broadcast(Protocolo.armar(Protocolo.INFO, nick + " se unió al chat"), nick);
            registry.log(nick + " se unió (" + registry.total() + " conectados)");
            System.out.println("[Servidor] " + nick + " conectado. Total: " + registry.total());

            bucleComandos();
        } catch (IOException e) {
            // Desconexión abrupta (cerraron la ventana, se cayó la red): es normal, no es un bug.
        } finally {
            limpiar();   // SIEMPRE se ejecuta, pase lo que pase
        }
    }

    /** Espera un JOIN válido. @return false si el cliente se fue antes de registrarse. */
    private boolean autenticar() throws IOException {
        String linea;
        while ((linea = in.readLine()) != null) {
            if (!Protocolo.JOIN.equals(Protocolo.comando(linea))) {
                send(Protocolo.armar(Protocolo.ERROR, "Primero debes registrarte con un nickname"));
                continue;
            }
            String candidato = Protocolo.resto(linea).trim();
            if (!Protocolo.nickValido(candidato)) {
                send(Protocolo.armar(Protocolo.ERROR,
                        "Nickname inválido (1-15 letras, números o _, sin espacios)"));
                continue;
            }
            // Mientras tenemos el monitor de este handler, nadie más puede hacer send() hacia
            // este cliente. Así garantizamos que el OK sea lo PRIMERO que recibe, aunque otro
            // hilo haga un broadcast justo después de que quedemos registrados.
            synchronized (this) {
                if (registry.registrar(candidato, this)) {
                    nick = candidato;
                    send(Protocolo.OK);
                    return true;
                }
            }
            send(Protocolo.armar(Protocolo.ERROR, "Ese nickname ya está en uso"));
        }
        return false;
    }

    private void bucleComandos() throws IOException {
        String linea;
        while ((linea = in.readLine()) != null) {
            String cmd = Protocolo.comando(linea);
            String resto = Protocolo.resto(linea);

            switch (cmd) {
                case Protocolo.MSG -> {
                    if (resto.isBlank()) {
                        break;
                    }
                    registry.broadcast(Protocolo.armar(Protocolo.MSG, nick, resto), nick);
                    registry.log("<" + nick + "> " + resto);
                }
                case Protocolo.PRIV -> {
                    String[] p = Protocolo.dividir(resto, 2);   // destino | texto
                    if (p.length < 2 || p[1].isBlank()) {
                        send(Protocolo.armar(Protocolo.ERROR, "Uso: /msg usuario texto"));
                    } else if (p[0].equals(nick)) {
                        send(Protocolo.armar(Protocolo.ERROR, "No puedes enviarte un privado a ti mismo"));
                    } else if (!registry.enviarPrivado(p[0], Protocolo.armar(Protocolo.PRIV, nick, p[1]))) {
                        send(Protocolo.armar(Protocolo.ERROR, "El usuario '" + p[0] + "' no está conectado"));
                    }
                }
                case Protocolo.LIST ->
                        send(Protocolo.armar(Protocolo.USERS, registry.listaUsuarios()));
                case Protocolo.QUIT -> {
                    return;   // el finally de run() hace la limpieza
                }
                default ->
                        send(Protocolo.armar(Protocolo.ERROR, "Comando desconocido: " + cmd));
            }
        }
        // readLine() devolvió null: el cliente cerró la conexión
    }

    /** Escritura segura entre hilos. @return false si falló (el cliente ya no está). */
    public synchronized boolean send(String linea) {
        out.println(linea);
        return !out.checkError();   // PrintWriter no lanza excepciones, hay que preguntar
    }

    /** Cierra el socket; también desbloquea el readLine() de este handler. */
    public void cerrar() {
        try {
            socket.close();
        } catch (IOException ignorada) {
            // ya estaba cerrado
        }
    }

    private void limpiar() {
        // Solo anunciamos la salida si este cliente llegó a registrarse
        if (nick != null && registry.eliminar(nick, this)) {
            registry.broadcast(Protocolo.armar(Protocolo.INFO, nick + " salió del chat"), null);
            registry.log(nick + " salió (" + registry.total() + " conectados)");
            System.out.println("[Servidor] " + nick + " desconectado. Total: " + registry.total());
        }
        cerrar();
    }
}