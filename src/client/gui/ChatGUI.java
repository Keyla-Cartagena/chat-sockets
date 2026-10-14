package client.gui;

import client.ConexionChat;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Ventana principal del cliente gráfico.
 *
 * HILOS (regla de oro de Swing: la interfaz solo se toca desde el hilo de Swing, el EDT):
 *   - EDT: dibuja y atiende clics/teclado. Nunca debe bloquearse.
 *   - SwingWorker: abre el socket en segundo plano (conectar() bloquea) y devuelve el
 *     resultado al EDT en done().
 *   - chat-receptor (ConexionChat): lee del servidor. Cada evento que llega se pasa a la
 *     interfaz con SwingUtilities.invokeLater, para que se ejecute en el EDT.
 *   - chat-enviador (ConexionChat): escribe en el socket sin congelar la ventana.
 *
 * Uso: java -cp out client.gui.ChatGUI
 */
public class ChatGUI extends JFrame {

    private static final String PANTALLA_LOGIN = "login";
    private static final String PANTALLA_CHAT = "chat";

    private final CardLayout cartas = new CardLayout();
    private final JPanel raiz = new JPanel(cartas);
    private final PanelLogin login = new PanelLogin(this::entrar);

    private ConexionChat conexion;
    private PanelChat chat;

    public ChatGUI() {
        super("Chat en línea");
        setIconImage(Componentes.icono(64));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(820, 560));
        setSize(1060, 700);
        setLocationRelativeTo(null);

        raiz.add(login, PANTALLA_LOGIN);
        setContentPane(raiz);

        // Al cerrar la ventana, despedirse del servidor con QUIT
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (conexion != null) {
                    conexion.cerrar();
                }
            }

            @Override
            public void windowOpened(WindowEvent e) {
                login.enfocar();
            }
        });
    }

    /** Se llama en el EDT cuando el usuario pulsa "Entrar". */
    private void entrar(PanelLogin.DatosLogin datos) {
        login.setCargando(true);

        new SwingWorker<ConexionChat, Void>() {
            @Override
            protected ConexionChat doInBackground() throws IOException {
                // Hilo de fondo: aquí sí podemos bloquear (abrir socket + registrarse)
                return ConexionChat.conectar(datos.host(), datos.puerto(), datos.nick());
            }

            @Override
            protected void done() {
                // De vuelta en el EDT
                try {
                    abrirChat(get(), datos.nick());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause();
                    login.mostrarError(causa != null ? causa.getMessage() : "No se pudo conectar.");
                    login.setCargando(false);
                    login.enfocar();
                }
            }
        }.execute();
    }

    private void abrirChat(ConexionChat nueva, String nick) {
        conexion = nueva;
        PanelChat panel = new PanelChat(nick,
                texto -> nueva.enviarMensaje(texto),
                (para, texto) -> nueva.enviarPrivado(para, texto),
                this::salir);
        chat = panel;
        raiz.add(panel, PANTALLA_CHAT);
        cartas.show(raiz, PANTALLA_CHAT);
        panel.enfocar();

        // Cada callback llega desde el hilo receptor: lo movemos al EDT
        nueva.iniciar(new ConexionChat.Oyente() {
            @Override
            public void alRecibirMensaje(String origen, String texto) {
                SwingUtilities.invokeLater(() -> panel.agregarMensaje(origen, texto));
            }

            @Override
            public void alRecibirPrivado(String origen, String texto) {
                SwingUtilities.invokeLater(() -> panel.agregarPrivado(origen, texto));
            }

            @Override
            public void alActualizarUsuarios(List<String> usuarios) {
                SwingUtilities.invokeLater(() -> panel.actualizarUsuarios(usuarios));
            }

            @Override
            public void alRecibirInfo(String texto) {
                SwingUtilities.invokeLater(() -> panel.agregarInfo(texto));
            }

            @Override
            public void alRecibirError(String texto) {
                SwingUtilities.invokeLater(() -> panel.agregarError(texto));
            }

            @Override
            public void alDesconectar(String motivo) {
                SwingUtilities.invokeLater(() -> panel.mostrarDesconexion(motivo));
            }
        });
    }

    /** Botón "Salir": cierra la conexión y vuelve a la pantalla de inicio. */
    private void salir() {
        if (conexion != null) {
            conexion.cerrar();
            conexion = null;
        }
        if (chat != null) {
            raiz.remove(chat);
            chat = null;
        }
        login.setCargando(false);
        cartas.show(raiz, PANTALLA_LOGIN);
        login.enfocar();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatGUI().setVisible(true));
    }
}