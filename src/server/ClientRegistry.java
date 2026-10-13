package server;

import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lista de clientes conectados. Es el ÚNICO estado compartido del servidor, y lo
 * tocan todos los hilos ClientHandler al mismo tiempo.
 *
 * SINCRONIZACIÓN: ConcurrentHashMap es thread-safe. putIfAbsent hace "revisar si el
 * nick existe + registrarlo" en un solo paso atómico, así dos clientes no pueden
 * quedarse con el mismo nick.
 */
public class ClientRegistry {

    private final ConcurrentHashMap<String, ClientHandler> clientes = new ConcurrentHashMap<>();
    private final HistoryLogger logger;

    public ClientRegistry(HistoryLogger logger) {
        this.logger = logger;
    }

    /** @return true si el nick estaba libre y quedó registrado. */
    public boolean registrar(String nick, ClientHandler handler) {
        return clientes.putIfAbsent(nick, handler) == null;
    }

    /** @return true si ese handler estaba registrado con ese nick y fue removido. */
    public boolean eliminar(String nick, ClientHandler handler) {
        return clientes.remove(nick, handler);
    }

    /** Envía la línea a todos, menos a 'excluirNick' (puede ser null para no excluir a nadie). */
    public void broadcast(String linea, String excluirNick) {
        for (Map.Entry<String, ClientHandler> e : clientes.entrySet()) {
            if (e.getKey().equals(excluirNick)) {
                continue;
            }
            if (!e.getValue().send(linea)) {
                // Falló el envío: cerramos su socket. Su propio hilo detectará la
                // desconexión y hará la limpieza completa en su bloque finally.
                e.getValue().cerrar();
            }
        }
    }

    /** @return false si el destinatario no existe o no se pudo enviar. */
    public boolean enviarPrivado(String destino, String linea) {
        ClientHandler h = clientes.get(destino);
        return h != null && h.send(linea);
    }

    /** Nicks ordenados y separados por coma: "ana,juan,luis". */
    public String listaUsuarios() {
        return String.join(",", new TreeSet<>(clientes.keySet()));
    }

    public int total() {
        return clientes.size();
    }

    public void log(String texto) {
        if (logger != null) {
            logger.log(texto);
        }
    }
}