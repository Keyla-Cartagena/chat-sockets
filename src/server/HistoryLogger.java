package server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Guarda el historial en un archivo con timestamp.
 *
 * HILO: tiene su propio hilo escritor (consumidor). Los ClientHandler (productores)
 * solo ponen el texto en la cola con log(), que es instantáneo, así escribir en disco
 * nunca frena el chat. BlockingQueue ya es thread-safe: no necesita locks extra.
 */
public class HistoryLogger implements Runnable {

    private static final String FIN = "\u0000__FIN__";
    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BlockingQueue<String> cola = new LinkedBlockingQueue<>();
    private final Path archivo;
    private final Thread hilo;

    public HistoryLogger(String ruta) {
        this.archivo = Path.of(ruta);
        this.hilo = new Thread(this, "history-logger");
        this.hilo.setDaemon(true);
        this.hilo.start();
    }

    /** Lo llaman muchos hilos a la vez; solo encola. */
    public void log(String texto) {
        cola.offer("[" + LocalDateTime.now().format(FORMATO) + "] " + texto);
    }

    @Override
    public void run() {
        try (BufferedWriter w = Files.newBufferedWriter(archivo, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            while (true) {
                String linea = cola.take();          // bloquea hasta que haya algo
                if (FIN.equals(linea)) {
                    break;
                }
                w.write(linea);
                w.newLine();
                w.flush();
            }
        } catch (IOException e) {
            System.err.println("No se pudo escribir el historial: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Pide al hilo que termine (después de vaciar la cola) y lo espera un momento. */
    public void cerrar() {
        cola.offer(FIN);
        try {
            hilo.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}