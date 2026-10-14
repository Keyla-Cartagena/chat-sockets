package client.gui;

import client.gui.Componentes.Avatar;
import client.gui.Componentes.BotonSuave;
import client.gui.Componentes.CampoRedondeado;
import client.gui.Componentes.Logo;
import client.gui.Componentes.PanelRedondeado;
import client.gui.Componentes.Pildora;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Pantalla principal del chat. TODOS sus métodos públicos deben llamarse desde el hilo de
 * Swing (EDT); ChatGUI se encarga de pasarlos por SwingUtilities.invokeLater.
 */
public class PanelChat extends JPanel {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_MENSAJES = 400;

    private final String nick;
    private final Consumer<String> alEnviarPublico;
    private final BiConsumer<String, String> alEnviarPrivado;

    private final Lista lista = new Lista();
    private final JScrollPane scroll = new JScrollPane(lista);
    private final JPanel panelUsuarios = new JPanel();
    private final JLabel etiquetaConectados = new JLabel("EN LÍNEA");
    private final JLabel subtitulo = new JLabel("Conectando…");
    private final CampoRedondeado entrada = new CampoRedondeado("Escribe un mensaje…", 999);
    private final BotonSuave botonEnviar = new BotonSuave("", BotonSuave.Estilo.ENVIAR);
    private final JPanel contenedorChip = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final JLabel textoChip = new JLabel();

    private String destino;   // null = mensaje para todos
    private boolean conectado = true;

    public PanelChat(String nick, Consumer<String> alEnviarPublico,
                     BiConsumer<String, String> alEnviarPrivado, Runnable alSalir) {
        this.nick = nick;
        this.alEnviarPublico = alEnviarPublico;
        this.alEnviarPrivado = alEnviarPrivado;

        setLayout(new BorderLayout());
        setBackground(Tema.FONDO);
        add(crearBarraLateral(alSalir), BorderLayout.WEST);
        add(crearZonaPrincipal(), BorderLayout.CENTER);

        agregarInfo("Te conectaste como " + nick + ". Haz clic en un usuario para escribirle en privado.");
    }

    // ------------------------------------------------------------ construcción

    private JPanel crearBarraLateral(Runnable alSalir) {
        JPanel lateral = new JPanel(new BorderLayout());
        lateral.setBackground(Tema.PANEL);
        lateral.setPreferredSize(new Dimension(270, 10));
        lateral.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Tema.BORDE));

        // Marca
        JPanel marca = new JPanel(new BorderLayout(12, 0));
        marca.setOpaque(false);
        marca.setBorder(new EmptyBorder(22, 20, 18, 20));
        marca.add(new Logo(40), BorderLayout.WEST);
        JLabel nombre = new JLabel("Chat");
        nombre.setFont(Tema.fuente(Font.BOLD, 21f));
        nombre.setForeground(Tema.TEXTO);
        JLabel lema = new JLabel("Hilos y sockets");
        lema.setFont(Tema.fuente(Font.PLAIN, 12f));
        lema.setForeground(Tema.TEXTO_SUAVE);
        JPanel textos = new JPanel(new BorderLayout());
        textos.setOpaque(false);
        textos.add(nombre, BorderLayout.CENTER);
        textos.add(lema, BorderLayout.SOUTH);
        marca.add(textos, BorderLayout.CENTER);
        lateral.add(marca, BorderLayout.NORTH);

        // Lista de usuarios
        JPanel bloqueUsuarios = new JPanel(new BorderLayout());
        bloqueUsuarios.setOpaque(false);
        etiquetaConectados.setFont(Tema.fuente(Font.BOLD, 11.5f));
        etiquetaConectados.setForeground(Tema.TEXTO_SUAVE);
        etiquetaConectados.setBorder(new EmptyBorder(6, 24, 10, 20));
        bloqueUsuarios.add(etiquetaConectados, BorderLayout.NORTH);

        panelUsuarios.setLayout(new BoxLayout(panelUsuarios, BoxLayout.Y_AXIS));
        panelUsuarios.setBackground(Tema.PANEL);
        panelUsuarios.setBorder(new EmptyBorder(0, 12, 0, 12));
        JScrollPane scrollUsuarios = new JScrollPane(panelUsuarios);
        estilizarScroll(scrollUsuarios, Tema.PANEL);
        bloqueUsuarios.add(scrollUsuarios, BorderLayout.CENTER);
        lateral.add(bloqueUsuarios, BorderLayout.CENTER);

        // Perfil propio + salir
        JPanel perfil = new JPanel(new BorderLayout(12, 0));
        perfil.setOpaque(false);
        perfil.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDE), new EmptyBorder(16, 20, 16, 16)));
        JPanel avatarWrap = new JPanel(new BorderLayout());
        avatarWrap.setOpaque(false);
        avatarWrap.add(new Avatar(nick, 40), BorderLayout.CENTER);
        perfil.add(avatarWrap, BorderLayout.WEST);
        JLabel yo = new JLabel(nick);
        yo.setFont(Tema.fuente(Font.BOLD, 14f));
        yo.setForeground(Tema.TEXTO);
        JLabel estado = new JLabel("Conectado");
        estado.setFont(Tema.fuente(Font.PLAIN, 12f));
        estado.setForeground(Tema.EN_LINEA);
        JPanel datos = new JPanel(new BorderLayout());
        datos.setOpaque(false);
        datos.add(yo, BorderLayout.CENTER);
        datos.add(estado, BorderLayout.SOUTH);
        perfil.add(datos, BorderLayout.CENTER);
        BotonSuave salir = new BotonSuave("Salir", BotonSuave.Estilo.FANTASMA);
        salir.setBorder(new EmptyBorder(8, 16, 8, 16));
        salir.setFont(Tema.fuente(Font.BOLD, 13f));
        salir.addActionListener(e -> alSalir.run());
        JPanel salirWrap = new JPanel(new BorderLayout());
        salirWrap.setOpaque(false);
        salirWrap.add(salir, BorderLayout.CENTER);
        perfil.add(salirWrap, BorderLayout.EAST);
        lateral.add(perfil, BorderLayout.SOUTH);

        return lateral;
    }

    private JPanel crearZonaPrincipal() {
        JPanel principal = new JPanel(new BorderLayout());
        principal.setBackground(Tema.FONDO);

        // Encabezado
        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setBackground(Tema.FONDO);
        encabezado.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDE), new EmptyBorder(18, 30, 15, 30)));
        JLabel titulo = new JLabel("Chat general");
        titulo.setFont(Tema.fuente(Font.BOLD, 19f));
        titulo.setForeground(Tema.TEXTO);
        subtitulo.setFont(Tema.fuente(Font.PLAIN, 13f));
        subtitulo.setForeground(Tema.TEXTO_SUAVE);
        encabezado.add(titulo, BorderLayout.CENTER);
        encabezado.add(subtitulo, BorderLayout.SOUTH);
        principal.add(encabezado, BorderLayout.NORTH);

        // Mensajes
        estilizarScroll(scroll, Tema.FONDO);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { ajustarBurbujas(); }
        });
        principal.add(scroll, BorderLayout.CENTER);

        // Entrada
        JPanel zonaEntrada = new JPanel(new BorderLayout());
        zonaEntrada.setBackground(Tema.FONDO);
        zonaEntrada.setBorder(new EmptyBorder(6, 26, 22, 26));

        PanelRedondeado chip = new PanelRedondeado(Tema.PRIVADO_FONDO, 999);
        chip.setBorde(Tema.PRIVADO);
        chip.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 6));
        textoChip.setFont(Tema.fuente(Font.BOLD, 12.5f));
        textoChip.setForeground(Tema.PRIVADO);
        JLabel cerrarChip = new JLabel("×");
        cerrarChip.setFont(Tema.fuente(Font.BOLD, 17f));
        cerrarChip.setForeground(Tema.PRIVADO);
        cerrarChip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cerrarChip.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { elegirDestino(null); }
        });
        chip.add(textoChip);
        chip.add(cerrarChip);
        contenedorChip.setOpaque(false);
        contenedorChip.setBorder(new EmptyBorder(0, 6, 10, 0));
        contenedorChip.add(chip);
        contenedorChip.setVisible(false);
        zonaEntrada.add(contenedorChip, BorderLayout.NORTH);

        JPanel fila = new JPanel(new BorderLayout(12, 0));
        fila.setOpaque(false);
        fila.add(entrada, BorderLayout.CENTER);
        fila.add(botonEnviar, BorderLayout.EAST);
        zonaEntrada.add(fila, BorderLayout.CENTER);
        principal.add(zonaEntrada, BorderLayout.SOUTH);

        entrada.texto.addActionListener(e -> enviar());
        botonEnviar.addActionListener(e -> enviar());
        return principal;
    }

    private static void estilizarScroll(JScrollPane sp, Color fondo) {
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(true);
        sp.getViewport().setBackground(fondo);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.getVerticalScrollBar().setUI(new Componentes.BarraSuave());
        sp.getVerticalScrollBar().setBackground(fondo);
        sp.getVerticalScrollBar().setOpaque(true);
    }

    // ------------------------------------------------------------------ envío

    private void enviar() {
        String texto = entrada.texto.getText().trim();
        if (texto.isEmpty() || !conectado) {
            return;
        }
        entrada.texto.setText("");

        if (texto.equalsIgnoreCase("/salir")) {
            return;   // para salir está el botón; evitamos mandar "/salir" como mensaje
        }
        if (texto.regionMatches(true, 0, "/msg ", 0, 5)) {
            String[] p = texto.split("\\s+", 3);
            if (p.length < 3) {
                agregarError("Uso: /msg usuario texto");
            } else {
                enviarPrivado(p[1], p[2]);
            }
            return;
        }
        if (destino == null) {
            anadirBurbuja(nick, texto, true, false, null);
            alEnviarPublico.accept(texto);
        } else {
            enviarPrivado(destino, texto);
        }
    }

    private void enviarPrivado(String para, String texto) {
        anadirBurbuja(nick, texto, true, true, "Privado · para " + para);
        alEnviarPrivado.accept(para, texto);
    }

    private void elegirDestino(String nuevo) {
        destino = nuevo;
        contenedorChip.setVisible(nuevo != null);
        textoChip.setText(nuevo == null ? "" : "Privado para " + nuevo);
        entrada.texto.setPista(nuevo == null ? "Escribe un mensaje…" : "Mensaje privado para " + nuevo + "…");
        panelUsuarios.repaint();
        revalidate();
        entrada.texto.requestFocusInWindow();
    }

    // ------------------------------------------------- API para ChatGUI (en el EDT)

    public void agregarMensaje(String origen, String texto) {
        anadirBurbuja(origen, texto, false, false, null);
    }

    public void agregarPrivado(String origen, String texto) {
        anadirBurbuja(origen, texto, false, true, "Privado · de " + origen);
    }

    public void agregarInfo(String texto) {
        anadirPildora(texto, Tema.TEXTO_SUAVE);
    }

    public void agregarError(String texto) {
        anadirPildora(texto, Tema.ERROR);
    }

    public void actualizarUsuarios(List<String> nuevos) {
        List<String> orden = new ArrayList<>(nuevos);
        orden.sort(String.CASE_INSENSITIVE_ORDER);
        if (orden.remove(nick)) {
            orden.add(0, nick);   // tú primero
        }

        panelUsuarios.removeAll();
        for (String u : orden) {
            panelUsuarios.add(new FilaUsuario(u));
        }
        panelUsuarios.revalidate();
        panelUsuarios.repaint();

        int n = orden.size();
        etiquetaConectados.setText("EN LÍNEA · " + n);
        subtitulo.setText(!conectado ? "Desconectado"
                : n <= 1 ? "Solo tú por ahora" : n + " personas en línea");
        if (destino != null && !orden.contains(destino)) {
            elegirDestino(null);
        }
    }

    public void mostrarDesconexion(String motivo) {
        conectado = false;
        agregarError(motivo + ". Pulsa «Salir» para volver al inicio.");
        subtitulo.setText("Desconectado");
        subtitulo.setForeground(Tema.ERROR);
        entrada.texto.setEnabled(false);
        entrada.texto.setPista("Sin conexión");
        botonEnviar.setEnabled(false);
    }

    public void enfocar() {
        entrada.texto.requestFocusInWindow();
    }

    // --------------------------------------------------------- lista de mensajes

    private void anadirBurbuja(String autor, String texto, boolean propia, boolean privada, String cabeceraPrivada) {
        Color colorCabecera = privada ? Tema.PRIVADO : Tema.colorAvatar(autor);
        String cabecera = privada ? cabeceraPrivada : (propia ? null : autor);
        Burbuja burbuja = new Burbuja(texto, cabecera, LocalTime.now().format(HORA), colorCabecera, propia, privada);
        burbuja.setAnchoMax(anchoBurbuja());

        Fila fila = new Fila(burbuja);
        if (propia) {
            fila.add(burbuja, BorderLayout.EAST);
        } else {
            JPanel avatarWrap = new JPanel(new BorderLayout());
            avatarWrap.setOpaque(false);
            avatarWrap.add(new Avatar(autor, 34), BorderLayout.NORTH);
            JPanel izquierda = new JPanel(new BorderLayout(10, 0));
            izquierda.setOpaque(false);
            izquierda.add(avatarWrap, BorderLayout.WEST);
            izquierda.add(burbuja, BorderLayout.CENTER);
            fila.add(izquierda, BorderLayout.WEST);
        }
        publicar(fila, propia);
    }

    private void anadirPildora(String texto, Color color) {
        Fila fila = new Fila(null);
        JPanel centro = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        centro.setOpaque(false);
        centro.add(new Pildora(texto, color));
        fila.add(centro, BorderLayout.CENTER);
        publicar(fila, false);
    }

    private void publicar(Fila fila, boolean forzarScroll) {
        JScrollBar barra = scroll.getVerticalScrollBar();
        boolean alFinal = barra.getValue() + barra.getVisibleAmount() >= barra.getMaximum() - 80;

        lista.add(fila);
        while (lista.getComponentCount() > MAX_MENSAJES) {
            lista.remove(0);
        }
        lista.revalidate();
        lista.repaint();

        if (alFinal || forzarScroll) {
            // invokeLater: esperamos a que Swing recalcule el tamaño antes de bajar
            SwingUtilities.invokeLater(() -> barra.setValue(barra.getMaximum()));
        }
    }

    private int anchoBurbuja() {
        int ancho = scroll.getViewport().getWidth();
        if (ancho <= 0) {
            ancho = 760;
        }
        return Math.max(240, Math.min(580, (int) ((ancho - 100) * 0.80)));
    }

    private void ajustarBurbujas() {
        int ancho = anchoBurbuja();
        for (Component c : lista.getComponents()) {
            if (c instanceof Fila f && f.burbuja != null) {
                f.burbuja.setAnchoMax(ancho);
            }
        }
        lista.revalidate();
    }

    // ------------------------------------------------------ componentes internos

    /** Contenedor vertical de mensajes; sigue el ancho de la ventana (Scrollable). */
    private static class Lista extends JPanel implements Scrollable {
        Lista() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(Tema.FONDO);
            setBorder(new EmptyBorder(18, 26, 14, 26));
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Math.max(60, r.height - 60); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    /** Una fila de la lista: una burbuja alineada a un lado, o un aviso centrado. */
    private static class Fila extends JPanel {
        final Burbuja burbuja;

        Fila(Burbuja burbuja) {
            super(new BorderLayout());
            this.burbuja = burbuja;
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setBorder(new EmptyBorder(5, 0, 5, 0));
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }

    /** Un usuario en la barra lateral. Clic = elegirlo como destinatario privado. */
    private class FilaUsuario extends JPanel {
        private final String usuario;
        private final boolean soyYo;
        private boolean encima;

        FilaUsuario(String usuario) {
            this.usuario = usuario;
            this.soyYo = usuario.equals(nick);
            setOpaque(false);
            setPreferredSize(new Dimension(10, 56));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            if (!soyYo) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { encima = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { encima = false; repaint(); }
                    @Override public void mousePressed(MouseEvent e) {
                        if (conectado) {
                            elegirDestino(usuario.equals(destino) ? null : usuario);
                        }
                    }
                });
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Tema.suavizar(g);
            int w = getWidth();
            int h = getHeight();
            boolean elegido = usuario.equals(destino);

            if (elegido) {
                g2.setColor(Tema.PRIVADO_FONDO);
                g2.fillRoundRect(0, 4, w, h - 8, 18, 18);
                g2.setColor(Tema.conAlfa(Tema.PRIVADO, 150));
                g2.drawRoundRect(0, 4, w - 1, h - 9, 18, 18);
            } else if (encima) {
                g2.setColor(Tema.SUPERFICIE);
                g2.fillRoundRect(0, 4, w, h - 8, 18, 18);
            }

            Componentes.dibujarAvatar(g2, usuario, 12, (h - 36) / 2, 36);

            Font fNombre = Tema.fuente(Font.BOLD, 14f);
            Font fEstado = Tema.fuente(Font.PLAIN, 11.5f);
            FontMetrics fmN = g2.getFontMetrics(fNombre);
            FontMetrics fmE = g2.getFontMetrics(fEstado);
            int bloque = fmN.getHeight() + fmE.getHeight();
            int y = (h - bloque) / 2;

            g2.setFont(fNombre);
            g2.setColor(Tema.TEXTO);
            g2.drawString(usuario + (soyYo ? "  (tú)" : ""), 58, y + fmN.getAscent());

            g2.setFont(fEstado);
            g2.setColor(elegido ? Tema.PRIVADO : Tema.TEXTO_SUAVE);
            g2.drawString(elegido ? "Mensaje privado" : soyYo ? "Tú" : "En línea", 58,
                    y + fmN.getHeight() + fmE.getAscent());

            g2.setColor(Tema.EN_LINEA);
            g2.fill(new Ellipse2D.Float(w - 22, h / 2f - 4.5f, 9, 9));
            g2.dispose();
        }
    }
}