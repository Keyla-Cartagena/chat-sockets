package client;

import common.Protocolo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Capa de red del cliente gráfico. No sabe nada de ventanas: solo maneja el socket y
 * avisa lo que pasa a través de un Oyente. Así la interfaz y los sockets quedan separados.
 *
 * SOCKET: un único Socket hacia el servidor.
 *
 * HILOS:
 *   - "chat-receptor": lee del socket (readLine bloquea) y avisa al Oyente.
 *   - "chat-enviador": un ExecutorService de UN hilo que escribe en el socket, para que la
 *     ventana nunca se congele si la red está lenta. Al ser un solo hilo, los mensajes
 *     salen en orden y nunca se mezclan.
 *   - La conexión inicial (conectar) bloquea, así que la ventana la llama desde un hilo
 *     de fondo (SwingWorker).
 */
public class ConexionChat {

    /** Eventos que llegan del servidor. Se invocan desde el hilo receptor, NO desde el de Swing. */
    public interface Oyente {
        void alRecibirMensaje(String origen, String texto);
        void alRecibirPrivado(String origen, String texto);
        void alActualizarUsuarios(List<String> usuarios);
        void alRecibirInfo(String texto);
        void alRecibirError(String texto);
        void alDesconectar(String motivo);
    }

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final ExecutorService enviador = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "chat-enviador");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean cerrada = new AtomicBoolean(false);
    private volatile Oyente oyente;

    private ConexionChat(Socket socket, BufferedReader in, PrintWriter out) {
        this.socket = socket;
        this.in = in;
        this.out = out;
    }

    /**
     * Abre el socket y se registra con el nickname. BLOQUEA: llamar fuera del hilo de Swing.
     * Si algo falla lanza una IOException con un mensaje listo para mostrarle al usuario.
     */
    public static ConexionChat conectar(String host, int puerto, String nick) throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, puerto), 4000);
            socket.setSoTimeout(5000);   // solo durante el registro
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            out.println(Protocolo.armar(Protocolo.JOIN, nick));
            String respuesta = in.readLine();
            if (respuesta == null) {
                throw new IOException("El servidor cerró la conexión.");
            }
            if (!Protocolo.OK.equals(Protocolo.comando(respuesta))) {
                throw new IOException(Protocolo.resto(respuesta));   // ERROR|motivo
            }
            socket.setSoTimeout(0);   // ya registrados: esperar mensajes sin límite de tiempo
            return new ConexionChat(socket, in, out);

        } catch (UnknownHostException e) {
            cerrarSilencioso(socket);
            throw new IOException("No se encontró el servidor '" + host + "'.");
        } catch (ConnectException e) {
            cerrarSilencioso(socket);
            throw new IOException("No se pudo conectar a " + host + ":" + puerto
                    + ". ¿Está encendido el servidor?");
        } catch (SocketTimeoutException e) {
            cerrarSilencioso(socket);
            throw new IOException("El servidor en " + host + ":" + puerto + " no respondió a tiempo.");
        } catch (IOException e) {
            cerrarSilencioso(socket);
            throw e;
        }
    }

    /** Arranca el hilo receptor y pide la lista de usuarios. */
    public void iniciar(Oyente oyente) {
        this.oyente = oyente;
        Thread receptor = new Thread(this::escuchar, "chat-receptor");
        receptor.setDaemon(true);
        receptor.start();
        pedirUsuarios();
    }

    // ---------------------------------------------------------------- enviar

    public void enviarMensaje(String texto) {
        enviar(Protocolo.armar(Protocolo.MSG, texto));
    }

    public void enviarPrivado(String destino, String texto) {
        enviar(Protocolo.armar(Protocolo.PRIV, destino, texto));
    }

    public void pedirUsuarios() {
        enviar(Protocolo.LIST);
    }

    private void enviar(String linea) {
        if (cerrada.get()) {
            return;
        }
        try {
            enviador.execute(() -> {
                out.println(linea);
                if (out.checkError()) {
                    desconectado("Se perdió la conexión con el servidor");
                }
            });
        } catch (RejectedExecutionException ignorada) {
            // la conexión ya se estaba cerrando
        }
    }

    /** Salida voluntaria: avisa al servidor con QUIT y cierra el socket. */
    public void cerrar() {
        if (!cerrada.compareAndSet(false, true)) {
            return;
        }
        try {
            enviador.execute(() -> {
                out.println(Protocolo.QUIT);
                cerrarSilencioso(socket);
            });
        } catch (RejectedExecutionException e) {
            cerrarSilencioso(socket);
        }
        enviador.shutdown();
    }

    // --------------------------------------------------------------- recibir

    private void escuchar() {
        try {
            String linea;
            while ((linea = in.readLine()) != null) {
                procesar(linea);
            }
            desconectado("El servidor cerró la conexión");
        } catch (IOException e) {
            desconectado("Se perdió la conexión con el servidor");
        }
    }

    private void procesar(String linea) {
        Oyente o = oyente;
        String resto = Protocolo.resto(linea);
        switch (Protocolo.comando(linea)) {
            case Protocolo.MSG -> {
                String[] p = Protocolo.dividir(resto, 2);   // origen | texto
                o.alRecibirMensaje(p[0], p.length > 1 ? p[1] : "");
            }
            case Protocolo.PRIV -> {
                String[] p = Protocolo.dividir(resto, 2);
                o.alRecibirPrivado(p[0], p.length > 1 ? p[1] : "");
            }
            case Protocolo.USERS ->
                    o.alActualizarUsuarios(resto.isEmpty() ? List.of() : List.of(resto.split(",")));
            case Protocolo.INFO -> {
                o.alRecibirInfo(resto);
                pedirUsuarios();   // alguien entró o salió: refrescamos la lista
            }
            case Protocolo.ERROR -> o.alRecibirError(resto);
            default -> { /* OK u otros: nada que mostrar */ }
        }
    }

    /** Desconexión NO voluntaria. compareAndSet evita avisar dos veces desde hilos distintos. */
    private void desconectado(String motivo) {
        if (cerrada.compareAndSet(false, true)) {
            cerrarSilencioso(socket);
            enviador.shutdown();
            Oyente o = oyente;
            if (o != null) {
                o.alDesconectar(motivo);
            }
        }
    }

    private static void cerrarSilencioso(Socket s) {
        try {
            s.close();
        } catch (IOException ignorada) {
            // ya estaba cerrado
        }
    }
}