package server;

import common.Protocolo;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Punto de entrada del servidor.
 *
 * SOCKET: el ServerSocket escucha en un puerto. accept() bloquea hasta que llega un
 * cliente y devuelve un Socket nuevo, exclusivo para hablar con ese cliente.
 *
 * HILOS: el hilo main SOLO hace accept(). Cada cliente se entrega a un ClientHandler
 * que corre en un hilo del pool, así el main siempre está libre para aceptar al siguiente.
 */
public class ChatServer {

    public static void main(String[] args) {
        int puerto = args.length > 0 ? Integer.parseInt(args[0]) : Protocolo.PUERTO;

        HistoryLogger logger = new HistoryLogger("historial.txt");
        ClientRegistry registry = new ClientRegistry(logger);
        ExecutorService pool = Executors.newCachedThreadPool();

        // Al presionar Ctrl+C: vaciar y cerrar el historial
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Servidor] Apagando...");
            pool.shutdownNow();
            logger.cerrar();
        }));

        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            System.out.println("[Servidor] Escuchando en el puerto " + puerto + " (Ctrl+C para detener)");
            logger.log("Servidor iniciado en el puerto " + puerto);

            while (true) {
                Socket socket = serverSocket.accept();   // bloquea hasta que llegue un cliente
                try {
                    pool.execute(new ClientHandler(socket, registry));
                } catch (IOException e) {
                    System.err.println("[Servidor] No se pudo atender a un cliente: " + e.getMessage());
                    socket.close();
                }
            }
        } catch (IOException e) {
            System.err.println("[Servidor] Error en el puerto " + puerto + ": " + e.getMessage());
        }
    }
}